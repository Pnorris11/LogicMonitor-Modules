import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URLEncoder

def apiKey  = hostProps.get("unifi.api.key")?.toString()?.trim()
def apiBase = hostProps.get("unifi.api.base")?.toString()?.trim() ?: "https://api.ui.com"

def wild = "##WILDVALUE##".toString().trim()

int cameraStatus = 0

try {
    if (!apiKey) throw new Exception("Missing unifi.api.key")
    if (!wild || !wild.contains("::")) throw new Exception("Missing or invalid wildvalue: ${wild}")

    def parts = wild.split("::")
    if (parts.size() < 2) throw new Exception("Unable to parse wildvalue: ${wild}")

    def consoleId = parts[0]?.trim()
    def cameraId  = parts[parts.size() - 1]?.trim()

    if (!consoleId) throw new Exception("Missing consoleId from wildvalue")
    if (!cameraId) throw new Exception("Missing cameraId from wildvalue")

    def endpoint = "${apiBase}/v1/connector/consoles/${URLEncoder.encode(consoleId, 'UTF-8')}/proxy/protect/integration/v1/cameras"

    HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection()
    conn.setRequestMethod("GET")
    conn.setRequestProperty("Accept", "application/json")
    conn.setRequestProperty("X-API-Key", apiKey)
    conn.setConnectTimeout(15000)
    conn.setReadTimeout(30000)

    int code = conn.getResponseCode()
    if (code < 200 || code >= 300) {
        def err = conn.getErrorStream()?.getText("UTF-8")
        throw new Exception("HTTP ${code} ${err ?: ''}".trim())
    }

    def payload = new JsonSlurper().parse(conn.getInputStream())
    def data = payload instanceof List ? payload : []

    def matchedCamera = data.find { cam ->
        cam?.id?.toString()?.trim() == cameraId
    }

    if (matchedCamera == null) {
        throw new Exception("Camera not found for ${cameraId}")
    }

    def state = matchedCamera?.state?.toString()?.trim()?.toUpperCase()
    cameraStatus = (state == "CONNECTED") ? 1 : -1
}
catch (Exception e) {
    System.err.println("UniFi camera script error: ${e.message}")
}

println "camera_status=${cameraStatus}"
return 0