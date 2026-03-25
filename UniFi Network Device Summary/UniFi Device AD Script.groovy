import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URLEncoder

def apiKey   = hostProps.get("unifi.api.key")?.toString()?.trim()
def apiBase  = hostProps.get("unifi.api.base")?.toString()?.trim() ?: "https://api.ui.com"
def siteId   = hostProps.get("unifi.site.Id")?.toString()?.trim()
def siteName = hostProps.get("unifi.site.name")?.toString()?.trim()

if (!apiKey) {
    System.err.println("Missing property: unifi.api.key")
    println "debug##Missing API key"
    println ""
    return 0
}

if (!siteId) {
    System.err.println("Missing property: unifi.site.Id")
    println "debug##Missing site ID"
    println ""
    return 0
}

if (!siteName) {
    siteName = siteId
}

// Collect console IDs manually (safe + explicit)
def consoleIds = []

if (hostProps.get("unifi.console.Id.1")) {
    consoleIds << hostProps.get("unifi.console.Id.1")?.toString()?.trim()
}
if (hostProps.get("unifi.console.Id.2")) {
    consoleIds << hostProps.get("unifi.console.Id.2")?.toString()?.trim()
}
if (hostProps.get("unifi.console.Id.3")) {
    consoleIds << hostProps.get("unifi.console.Id.3")?.toString()?.trim()
}
if (hostProps.get("unifi.console.Id.4")) {
    consoleIds << hostProps.get("unifi.console.Id.4")?.toString()?.trim()
}
if (hostProps.get("unifi.console.Id.5")) {
    consoleIds << hostProps.get("unifi.console.Id.5")?.toString()?.trim()
}

// Clean list
consoleIds = consoleIds.findAll { it }

if (consoleIds.isEmpty()) {
    System.err.println("No unifi.console.Id.N values found")
    println "debug##No console IDs found"
    println ""
    return 0
}

def slurper = new JsonSlurper()
def discovered = 0

consoleIds.each { consoleId ->

    int offset = 0
    int limit = 100
    int total = Integer.MAX_VALUE

    while (offset < total) {

        try {
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
                System.err.println("HTTP ${code} for console ${consoleId}: ${err ?: 'no body'}")
                break
            }

            def payload = slurper.parse(conn.getInputStream())
            def data = payload?.data instanceof List ? payload.data : []

            // Handle pagination safely
            total = (payload?.totalCount instanceof Number) ? payload.totalCount.intValue() : data.size()

            data.each { dev ->

                def deviceId = dev?.id?.toString()?.trim()
                if (!deviceId) return

                def name  = dev?.name?.toString()?.trim() ?: "unknown"
                def ip    = dev?.ipAddress?.toString()?.trim() ?: ""
                def model = dev?.model?.toString()?.trim() ?: ""

                // Clean alias (NO deviceId)
                def aliasParts = [siteName, name, ip, model].findAll { it }
                def alias = aliasParts.join(" - ")

                // UNIQUE instance key
                def wildvalue = "${consoleId}::${siteId}::${deviceId}"

                println "${wildvalue}##${alias}"
                println "auto.unifi.consoleId=${consoleId}"
                println "auto.unifi.siteId=${siteId}"
                println "auto.unifi.deviceId=${deviceId}"
                println ""

                discovered++
            }

            if (data.isEmpty()) {
                break
            }

            offset += data.size()
        }
        catch (Exception e) {
            System.err.println("Console ${consoleId} exception: ${e.class.name}: ${e.message}")
            break
        }
    }
}

if (discovered == 0) {
    System.err.println("No devices discovered from configured consoles")
    println "debug##No devices discovered"
    println ""
    return 0
}

return 0