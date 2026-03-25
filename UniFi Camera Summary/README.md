# UniFi Camera Monitoring

LogicMonitor datasource for monitoring UniFi Protect cameras via the UniFi Management API.

## Overview

This datasource provides automatic discovery and status monitoring of UniFi cameras across one or more NVR (Network Video Recorder) consoles. It uses the UniFi Management API to:

- **Auto-Discovery**: Discover all cameras connected to configured NVR consoles
- **Collection**: Monitor individual camera connection status

## Components

### 1. UniFi Camera AD Script (Auto-Discovery)

**File**: `UniFi Camera AD Script.groovy`

Discovers all available UniFi cameras from configured NVR console IDs and creates monitoring instances for each camera.

**Process**:
1. Retrieves NVR console IDs from host properties
2. Queries the UniFi Management API for each console
3. Collects camera metadata (name, MAC address, model, state, mic status)
4. Outputs discovery data in LogicMonitor format

**Output Format**:
```
{consoleId}::{cameraId}##Camera Name
auto.unifi.nvrConsoleId=consoleId
auto.unifi.cameraId=cameraId
auto.unifi.cameraName=Camera Name
auto.unifi.cameraMac=xx:xx:xx:xx:xx:xx
auto.unifi.cameraState=CONNECTED|DISCONNECTED
auto.unifi.cameraModel=model_key
auto.unifi.cameraMicEnabled=true|false
```

### 2. UniFi Camera Collection Script

**File**: `UniFi Camera Collection Script.groovy`

Collects real-time status metrics for individual cameras discovered by the auto-discovery script.

**Process**:
1. Parses the wildvalue to extract console ID and camera ID
2. Fetches current camera status from the UniFi API
3. Evaluates connection state and returns numeric status

**Output**:
```
camera_status=1     (camera is CONNECTED)
camera_status=-1    (camera is DISCONNECTED)
```

## Configuration

### Required Properties

These properties must be configured at the host level in LogicMonitor:

| Property | Description | Example |
|----------|-------------|---------|
| `unifi.api.key` | UniFi Management API key | `your-api-key-here` |
| `unifi.api.base` | Base URL for UniFi API | `https://api.ui.com` (default) |
| `unifi.console.nvr.Id.1` | NVR Console ID (1st) | `console-id-1` |
| `unifi.console.nvr.Id.2` | NVR Console ID (2nd) | `console-id-2` |
| `unifi.console.nvr.Id.3` | NVR Console ID (3rd) | (optional) |
| `unifi.console.nvr.Id.4` | NVR Console ID (4th) | (optional) |
| `unifi.console.nvr.Id.5` | NVR Console ID (5th) | (optional) |
| `unifi.console.nvr.Id.6` | NVR Console ID (6th) | (optional) |

**Notes**:
- Up to 6 NVR consoles are supported
- At least one NVR console ID must be configured for discovery to work
- API key can be generated from the UniFi Management Console

### Optional Properties

| Property | Description | Default |
|----------|-------------|---------|
| `unifi.api.base` | API endpoint base URL | `https://api.ui.com` |

## Installation

1. Add this datasource to your LogicMonitor account
2. Configure the required host properties on your UniFi monitoring host
3. Assign the datasource to the host
4. Run auto-discovery to find all connected cameras

## Collected Metrics

### Camera Status (collection script)
- **Metric**: `camera_status`
- **Type**: Numeric
- **Values**:
  - `1` = Connected ✓
  - `-1` = Disconnected ✗

### Discovered Properties (auto-discovery)
- `auto.unifi.nvrConsoleId` - NVR console identifier
- `auto.unifi.cameraId` - Unique camera ID
- `auto.unifi.cameraName` - Camera display name
- `auto.unifi.cameraMac` - MAC address
- `auto.unifi.cameraState` - Current connection state
- `auto.unifi.cameraModel` - Camera model/key
- `auto.unifi.cameraMicEnabled` - Microphone status (true/false)

## API Connection Details

- **API Endpoint**: UniFi Management API v1
- **Authentication**: X-API-Key header
- **Connection Timeout**: 15 seconds
- **Read Timeout**: 30 seconds
- **SSL Verification**: Enabled

### API Endpoints Used

- Discovery: `GET /v1/connector/consoles/{consoleId}/proxy/protect/integration/v1/cameras`
- Collection: `GET /v1/connector/consoles/{consoleId}/proxy/protect/integration/v1/cameras`

## Error Handling

Both scripts include error handling for:
- Missing API key configuration
- Network timeouts
- Invalid API responses
- Missing NVR console IDs
- Camera not found errors

Errors are logged to stderr and debug output is provided to help troubleshoot issues.

## Troubleshooting

### No Cameras Discovered
- Verify `unifi.api.key` is set and valid
- Verify at least one `unifi.console.nvr.Id.N` is configured
- Check that the API key has permissions to access the cameras endpoint
- Verify network connectivity to the API endpoint

### Camera Status Shows -1 (Disconnected)
- This could indicate the camera is actually disconnected from the NVR
- Check UniFi console for camera connection status
- Verify network connection to camera

### HTTP 40x Errors
- Verify API key validity and permissions
- Confirm console ID is correct
- Check for API key expiration

## Example LogicMonitor Configuration

**Host Properties**:
```
unifi.api.key = your-management-api-key
unifi.api.base = https://api.ui.com
unifi.console.nvr.Id.1 = 1234567890abcdef
unifi.console.nvr.Id.2 = fedcba0987654321
```

**Discovery Parameters**:
- Auto-discovery will find all cameras and create instances
- Each instance has a unique wildvalue combining console ID and camera ID

**Collection Parameters**:
- Collection runs per-instance using the wildvalue
- Returns camera_status metric for alerting and graphing

## Support

For issues related to:
- **UniFi API**: Contact Ubiquiti support
- **LogicMonitor Configuration**: Review LogicMonitor documentation
- **Scripts**: Review error messages in Debug Output
