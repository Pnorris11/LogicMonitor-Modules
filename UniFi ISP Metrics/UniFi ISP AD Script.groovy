import groovy.json.JsonSlurper
import java.net.HttpURLConnection

def apiKey  = hostProps.get("unifi.api.key")
def apiBase = hostProps.get("unifi.api.base") ?: "https://api.ui.com"
def type    = hostProps.get("unifi.metric.type") ?: "5m"
def duration = hostProps.get("unifi.metric.duration") ?: "24h"

if (!apiKey) {
    System.err.println("Missing unifi.api.key")
    return 1
}

try {
    def url = "${apiBase}/v1/isp-metrics/${type}?duration=${duration}"
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
    conn.setRequestMethod("GET")
    conn.setRequestProperty("Accept", "application/json")
    conn.setRequestProperty("X-API-KEY", apiKey)
    conn.setConnectTimeout(15000)
    conn.setReadTimeout(30000)

    int code = conn.getResponseCode()
    if (code < 200 || code >= 300) {
        System.err.println("UniFi API HTTP code: ${code}")
        return 1
    }

    def payload = new JsonSlurper().parse(conn.getInputStream())
    def sites = payload?.data instanceof List ? payload.data : []

    if (sites.isEmpty()) {
        System.err.println("No sites returned in payload.data")
        return 1
    }

    sites.each { site ->
        def siteId = site?.siteId?.toString()?.trim()
        if (siteId) {
            println "${siteId}##${siteId}"
        }
    }

    return 0
}
catch (Exception e) {
    System.err.println("Script exception: " + e.message)
    return 1
}