import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URLEncoder

def apiKey   = hostProps.get("unifi.api.key")
def apiBase  = hostProps.get("unifi.api.base") ?: "https://api.ui.com"
def type     = hostProps.get("unifi.metric.type") ?: "5m"
def duration = hostProps.get("unifi.metric.duration") ?: "24h"

def instanceSiteId = instanceProps.get("wildvalue")?.toString()?.trim()
if (!instanceSiteId) {
    instanceSiteId = instanceProps.get("wildAlias")?.toString()?.trim()
}

if (!apiKey) {
    System.err.println("Missing required property: unifi.api.key")
    return 1
}

if (!instanceSiteId) {
    System.err.println("Missing instance siteId from instanceProps")
    return 1
}

def endpoint = "${apiBase}/v1/isp-metrics/${URLEncoder.encode(type, 'UTF-8')}?duration=${URLEncoder.encode(duration, 'UTF-8')}"

try {
    HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection()
    conn.setRequestMethod("GET")
    conn.setRequestProperty("Accept", "application/json")
    conn.setRequestProperty("X-API-KEY", apiKey)
    conn.setConnectTimeout(15000)
    conn.setReadTimeout(30000)

    int code = conn.getResponseCode()
    if (code < 200 || code >= 300) {
        System.err.println("UniFi API returned HTTP ${code}")
        return 1
    }

    def payload = new JsonSlurper().parse(conn.getInputStream())
    def sites = payload?.data instanceof List ? payload.data : []

    println "DEBUG: Total sites in response: ${sites.size()}"
    sites.each { s ->
        println "DEBUG: Available site siteId: ${s?.siteId}"
    }

    def site = sites.find { s ->
        s?.siteId?.toString()?.trim() == instanceSiteId
    }

    println "DEBUG: Looking for instanceSiteId: ${instanceSiteId}"
    println "DEBUG: Site found: ${site != null}"
    if (site) {
        println "DEBUG: Site metricType: ${site?.metricType}"
        println "DEBUG: Site periods count: ${site?.periods?.size()}"
    }

    if (!site) {
        System.err.println("No site found for siteId=${instanceSiteId}")
        return 1
    }

    def periods = site?.periods instanceof List ? site.periods : []
    if (!periods || periods.isEmpty()) {
        System.err.println("No periods found for siteId=${instanceSiteId}")
        return 1
    }

    println "DEBUG: Total periods: ${periods.size()}"
    def periodsWithTime = periods.findAll { it?.metricTime }
    println "DEBUG: Periods with metricTime: ${periodsWithTime.size()}"

    def latest = periodsWithTime.sort { a, b ->
        a.metricTime.toString() <=> b.metricTime.toString()
    }?.last()

    if (!latest) {
        System.err.println("Unable to determine latest metric period for siteId=${instanceSiteId}")
        return 1
    }

    println "DEBUG: Latest metricTime: ${latest?.metricTime}"
    println "DEBUG: Latest period data structure: ${latest?.data.keySet()}"
    
    def wan = latest?.data?.wan ?: [:]
    println "DEBUG: WAN object keys: ${wan.keySet()}"
    println "DEBUG: WAN full object: ${wan}"

    def uptime       = safeNumber(wan?.uptime, 0)
    def downtime     = safeNumber(wan?.downtime, 0)
    def avgLatency   = safeNumber(wan?.avgLatency, 0)
    def maxLatency   = safeNumber(wan?.maxLatency, 0)
    def packetLoss   = safeNumber(wan?.packetLoss, 0)
    def downloadKbps = safeNumber(wan?.download_kbps, 0)
    def uploadKbps   = safeNumber(wan?.upload_kbps, 0)

    def ispUp = 0
    println "DEBUG: uptime=${uptime} (${uptime.class.simpleName}), downtime=${downtime} (${downtime.class.simpleName})"
    println "DEBUG: uptime==100=${uptime == 100}, downtime==0=${downtime == 0}"
    
    if (uptime == 100 && downtime == 0) {
        ispUp = 1
    } else if (uptime == 0 && downtime == 100) {
        ispUp = -1
    } else if (uptime > 0 && downtime == 0) {
        ispUp = 0
    }
    println "DEBUG: final ispUp=${ispUp}"

    println "uptime: ${uptime}"
    println "downtime: ${downtime}"
    println "avgLatency: ${avgLatency}"
    println "maxLatency: ${maxLatency}"
    println "packetLoss: ${packetLoss}"
    println "download_kbps: ${downloadKbps}"
    println "upload_kbps: ${uploadKbps}"
    println "isp_up: ${ispUp}"

    return 0
}
catch (Exception e) {
    System.err.println("Script exception: ${e.message}")
    return 1
}

def safeNumber(val, fallback) {
    if (val == null) return fallback
    try {
        if (val instanceof Number) return val
        def s = val.toString().trim()
        if (!s) return fallback
        if (s.contains(".")) return new BigDecimal(s)
        return Long.parseLong(s)
    } catch (Exception ignored) {
        return fallback
    }
}