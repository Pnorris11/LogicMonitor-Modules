# UniFi Network Device Summary (LogicMonitor)

This package contains two Groovy scripts for LogicMonitor:

- `UniFi Device AD Script.groovy` (Active Discovery)
- `UniFi Device Collection Script.groovy` (Data Collection)

Together, they discover UniFi Network devices from the UniFi API and collect per-device status metrics.

## What the scripts do

### 1) Active Discovery script

`UniFi Device AD Script.groovy`:

- Reads API/site credentials from host properties.
- Reads up to 5 console IDs from:
	- `unifi.console.Id.1`
	- `unifi.console.Id.2`
	- `unifi.console.Id.3`
	- `unifi.console.Id.4`
	- `unifi.console.Id.5`
- Calls UniFi devices endpoint with pagination (`offset`/`limit`, default `limit=100`).
- Builds a unique LogicMonitor instance wildvalue in this format:
	- `<consoleId>::<siteId>::<deviceId>`
- Emits instance-level auto properties:
	- `auto.unifi.consoleId`
	- `auto.unifi.siteId`
	- `auto.unifi.deviceId`

Alias format emitted by AD:

- `<siteName> - <deviceName> - <ipAddress> - <model>`

If `unifi.site.name` is not defined, it falls back to `unifi.site.Id`.

### 2) Collection script

`UniFi Device Collection Script.groovy`:

- Reads the discovered wildvalue (`##WILDVALUE##`) and parses:
	- `consoleId`
	- `siteId`
	- `deviceId`
- Calls the same devices endpoint with pagination until it finds the matching `deviceId`.
- Emits three datapoints as `0/1` values:
	- `device_status`
		- `1` = device `state` is `ONLINE`
		- `0` = any other state
	- `firmware_updatable`
		- `1` = `firmwareUpdatable == true`
		- `0` = otherwise
	- `supported`
		- `1` = `supported == true`
		- `0` = otherwise

On script error, each datapoint remains `-1`.

## Required host properties

At minimum:

- `unifi.api.key` (required)
- `unifi.site.Id` (required)
- `unifi.console.Id.1` (required if only one console; add `.2`-`.5` as needed)

Optional:

- `unifi.api.base` (default: `https://api.ui.com`)
- `unifi.site.name` (used for friendly alias text)

## UniFi API endpoint used

Both scripts query:

`/v1/connector/consoles/{consoleId}/proxy/network/integration/v1/sites/{siteId}/devices?offset={offset}&limit={limit}`

Base URL comes from `unifi.api.base` (or defaults to `https://api.ui.com`).

## Error handling behavior

- AD script:
	- Logs missing required properties and returns cleanly.
	- Skips consoles that fail API calls.
	- If no devices are discovered, emits a debug line and exits.
- Collection script:
	- Logs exceptions to stderr.
	- Always prints datapoints; uses `-1` when lookup/collection fails.

## LogicMonitor notes

- Ensure the DataSource has three datapoints matching collection output:
	- `device_status`
	- `firmware_updatable`
	- `supported`
- Ensure the Datapoints are configured as so: 
    - Datapoint source = "Content the script writes to the standard output"
    - Interpret output with = "Multi-line key-value pairs"
    - Key = Datapoint 
- Ensure the AD script output parser is configured for:
	- `wildvalue##wildalias`
	- Followed by instance `auto.*` properties
- Ensure host properties are defined with exact key names (case-sensitive), especially:
	- `unifi.site.Id`
	- `unifi.console.Id.N`

## Quick validation checklist

1. Set required host properties.
2. Run Active Discovery and confirm instances are created with wildvalue format:
	 - `<consoleId>::<siteId>::<deviceId>`
3. Confirm instance auto properties populate.
4. Run collection and verify datapoints return`1` for healthy devices and `0` for device offline.
5. If values are `-1`, review script stderr and API access/permissions.
