import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URLEncoder

def apiKey  = hostProps.get("unifi.api.key")?.toString()?.trim()
def apiBase = hostProps.get("unifi.api.base")?.toString()?.trim() ?: "https://api.ui.com"

def wild = "##WILDVALUE##".toString().trim()

int deviceStatus = -1
int firmwareUpdatable = -1
int supported = -1

try {
    if (!apiKey) throw new Exception("Missing unifi.api.key")
    if (!wild || !wild.contains("::")) throw new Exception("Missing or invalid wildvalue")

    def parts = wild.split("::", 3)
    if (parts.size() != 3) throw new Exception("Unable to parse wildvalue: ${wild}")

    def consoleId = parts[0]?.trim()
    def siteId    = parts[1]?.trim()
    def deviceId  = parts[2]?.trim()

    if (!consoleId) throw new Exception("Missing consoleId from wildvalue")
    if (!siteId) throw new Exception("Missing siteId from wildvalue")
    if (!deviceId) throw new Exception("Missing deviceId from wildvalue")

    int offset = 0
    int limit = 100
    int total = Integer.MAX_VALUE
    def slurper = new JsonSlurper()
    def matchedDevice = null

    while (offset < total && matchedDevice == null) {
        def endpoint = "${apiBase}/v1/connector/consoles/${URLEncoder.encode(consoleId, 'UTF-8')}" +
                       "/proxy/network/integration/v1/sites/${URLEncoder.encode(siteId, 'UTF-8')}" +
                       "/devices?offset=${offset}&limit=${limit}"

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection()
        conn.setRequestMethod("GET")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("X-API-KEY", apiKey)
        conn.setConnectTimeout(15000)
        conn.setReadTimeout(30000)

        int code = conn.getResponseCode()
        if (code < 200 || code >= 300) {
            def err = conn.getErrorStream()?.getText("UTF-8")
            throw new Exception("HTTP ${code} ${err ?: ''}".trim())
        }

        def payload = slurper.parse(conn.getInputStream())
        def data = payload?.data instanceof List ? payload.data : []
        total = (payload?.totalCount instanceof Number) ? payload.totalCount.intValue() : data.size()

        matchedDevice = data.find { dev ->
            dev?.id?.toString()?.trim() == deviceId
        }

        if (data.isEmpty()) break
        offset += data.size()
    }

    if (matchedDevice == null) {
        throw new Exception("Device not found for ${deviceId}")
    }

    def state = matchedDevice?.state?.toString()?.trim()?.toUpperCase()
    deviceStatus = (state == "ONLINE") ? 1 : 0
    firmwareUpdatable = (matchedDevice?.firmwareUpdatable == true) ? 1 : 0
    supported = (matchedDevice?.supported == true) ? 1 : 0
}
catch (Exception e) {
    System.err.println("UniFi device script error: ${e.message}")
}

println "device_status=${deviceStatus}"
println "firmware_updatable=${firmwareUpdatable}"
println "supported=${supported}"
return 0