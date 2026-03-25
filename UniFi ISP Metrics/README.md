# UniFi ISP Metrics for LogicMonitor

This package provides LogicMonitor integration for monitoring Ubiquiti UniFi ISP metrics through the UniFi API.

## Overview

These scripts enable automated discovery and monitoring of UniFi ISP performance metrics including bandwidth, latency, packet loss, and uptime statistics.

## Scripts

### 1. UniFi ISP AD Script (`UniFi ISP AD Script.groovy`)

**Purpose:** Auto-discovery script that identifies available UniFi sites

**Function:**
- Queries the UniFi API for ISP metrics
- Returns a list of available site IDs for dynamic instance creation
- Formats output as `siteId##siteId` for LogicMonitor auto-discovery

**Requirements:**
- Valid UniFi API key in `unifi.api.key` host property
- Network connectivity to UniFi API endpoint

### 2. UniFi ISP Collection Script (`UniFi ISP Collection Script.groovy`)

**Purpose:** Collection script that gathers metrics for a specific UniFi site

**Function:**
- Retrieves ISP metrics for a specific site instance
- Extracts the latest metric period data from the API response
- Outputs the following metrics:
  - `uptime` - Time the ISP connection was up (seconds)
  - `downtime` - Time the ISP connection was down (seconds)
  - `avgLatency` - Average latency to gateway (ms)
  - `maxLatency` - Maximum latency to gateway (ms)
  - `packetLoss` - Packet loss percentage (%)
  - `download_kbps` - Download speed (Kbps)
  - `upload_kbps` - Upload speed (Kbps)
  - `isp_up` - Connection status (1 = up, 0 = down)

**Requirements:**
- Valid UniFi API key in `unifi.api.key` host property
- Instance property with site ID (`wildvalue` or `wildAlias`)

## Setup

### Required Host Properties

Configure these properties on your LogicMonitor device:

| Property | Description | Example |
|----------|-------------|---------|
| `unifi.api.key` | UniFi API authentication key | `sk-us-xxxxx` |
| `unifi.api.base` | UniFi API base URL (optional) | `https://api.ui.com` |
| `unifi.metric.type` | Metric granularity (optional) | `5m` (default) or `hourly` |
| `unifi.metric.duration` | Time period for metrics (optional) | `24h` (default) |

### Instance Properties (Collection Script)

Automatically populated by the discovery script:
- `wildvalue` or `wildAlias` - The UniFi site ID

## Configuration

### Metric Type Options
- `5m` - 5-minute metrics (default)
- `hourly` - Hourly aggregated metrics

### Duration Options
- `24h` - Last 24 hours (default)
- `7d` - Last 7 days
- `30d` - Last 30 days
- Other duration formats supported by UniFi API

## API Details

- **Endpoint:** `{apiBase}/v1/isp-metrics/{type}?duration={duration}`
- **Authentication:** X-API-KEY header with UniFi API key
- **Timeout:** 15 seconds connection, 30 seconds read
- **Response Format:** JSON

## Error Handling

Both scripts include comprehensive error handling:
- HTTP response code validation (200-299 range)
- Missing required properties detection
- Safe numeric conversion with fallback values
- Detailed error messages for troubleshooting

## Return Codes

- `0` - Success
- `1` - Error (API failure, missing properties, no data returned)

## Dependencies

- Groovy
- Standard Java HTTP libraries (included in LogicMonitor)

## Notes

- The collection script uses the most recent metric period timestamp from the API response
- The `safeNumber()` utility function handles numeric conversion gracefully, returning fallback value (0) on parse errors
- All API calls include appropriate accept headers and timeouts for reliability
