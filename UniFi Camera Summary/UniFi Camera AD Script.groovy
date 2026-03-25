import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URLEncoder

def apiKey  = hostProps.get("unifi.api.key")?.toString()?.trim()
def apiBase = hostProps.get("unifi.api.base")?.toString()?.trim() ?: "https://api.ui.com"

if (!apiKey) {
    System.err.println("Missing property: unifi.api.key")
    println "debug##Missing API key"
    println ""
    return 0
}

// Collect NVR console IDs manually
def consoleIds = []

if (hostProps.get("unifi.console.nvr.Id.1")) {
    consoleIds << hostProps.get("unifi.console.nvr.Id.1")?.toString()?.trim()
}
if (hostProps.get("unifi.console.nvr.Id.2")) {
    consoleIds << hostProps.get("unifi.console.nvr.Id.2")?.toString()?.trim()
}
if (hostProps.get("unifi.console.nvr.Id.3")) {
    consoleIds << hostProps.get("unifi.console.nvr.Id.3")?.toString()?.trim()
}
if (hostProps.get("unifi.console.nvr.Id.4")) {
    consoleIds << hostProps.get("unifi.console.nvr.Id.4")?.toString()?.trim()
}
if (hostProps.get("unifi.console.nvr.Id.5")) {
    consoleIds << hostProps.get("unifi.console.nvr.Id.5")?.toString()?.trim()
}
if (hostProps.get("unifi.console.nvr.Id.6")) {
    consoleIds << hostProps.get("unifi.console.nvr.Id.6")?.toString()?.trim()
}

consoleIds = consoleIds.findAll { it }.unique()

if (consoleIds.isEmpty()) {
    System.err.println("No unifi.console.nvr.Id.N values found")
    println "debug##No NVR console IDs found"
    println ""
    return 0
}

def slurper = new JsonSlurper()
def discovered = 0

consoleIds.each { consoleId ->
    try {
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
            System.err.println("HTTP ${code} for NVR console ${consoleId}: ${err ?: 'no body'}")
            return
        }

        def payload = slurper.parse(conn.getInputStream())
        def data = payload instanceof List ? payload : []

        data.each { cam ->
            def cameraId = cam?.id?.toString()?.trim()
            if (!cameraId) return

            def cameraName = cam?.name?.toString()?.trim() ?: "unknown-camera"
            def mac        = cam?.mac?.toString()?.trim() ?: ""
            def state      = cam?.state?.toString()?.trim() ?: ""
            def modelKey   = cam?.modelKey?.toString()?.trim() ?: ""
            def micEnabled = cam?.isMicEnabled != null ? cam.isMicEnabled.toString() : ""

            def wildvalue = "${consoleId}::${cameraId}"
            def alias = cameraName

            println "${wildvalue}##${alias}"
            println "auto.unifi.nvrConsoleId=${consoleId}"
            println "auto.unifi.cameraId=${cameraId}"
            println "auto.unifi.cameraName=${cameraName}"
            println "auto.unifi.cameraMac=${mac}"
            println "auto.unifi.cameraState=${state}"
            println "auto.unifi.cameraModel=${modelKey}"
            println "auto.unifi.cameraMicEnabled=${micEnabled}"
            println ""

            discovered++
        }
    }
    catch (Exception e) {
        System.err.println("NVR console ${consoleId} exception: ${e.class.name}: ${e.message}")
    }
}

if (discovered == 0) {
    System.err.println("No cameras discovered from configured NVR consoles")
    println "debug##No cameras discovered"
    println ""
    return 0
}

return 0