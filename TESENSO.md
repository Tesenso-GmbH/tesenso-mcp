# Tesenso Extension

This is the Tesenso fork of [thingsboard/thingsboard-mcp](https://github.com/thingsboard/thingsboard-mcp).
It adds MCP tools for the Tesenso platform (`/api/tesenso/...` on tesenso-server) on top of the
unmodified upstream ThingsBoard tools. Total: ~178 tools against a CE instance.

## Rules for keeping the fork upstream-mergeable

- **Never modify upstream files.** All Tesenso code lives in new files under
  `src/main/java/org/thingsboard/ai/mcp/server/tools/tesenso/` (inside the existing component scan,
  so no registration/config change is needed — any `@Service` implementing `McpTools` is picked up
  automatically by `EditionAwareToolProvider`).
- Upstream sync: `git fetch upstream && git merge upstream/master` (remote `upstream` =
  thingsboard/thingsboard-mcp). Only `README.md` (one pointer line) and this file can ever conflict.
- Commits as `Tesenso <developer@tesenso.ch>`.

## Tesenso tool groups

All groups can be disabled via `THINGSBOARD_TOOLS_GROUPS_<NAME>=false` (same mechanism as upstream).

| Group | Class | Content |
|---|---|---|
| `tesenso-bewirtschaftung` | `BewirtschaftungTools` | Cockpit, hub search, Nutzeinheiten, unit consumption/meters/billing summary, Mieterwechsel preview |
| `tesenso-billing` | `TesensoBillingTools` | Contracts, billing periods (progress/preflight), allocation results, invoices, billing metrics |
| `tesenso-energy-community` | `EnergyCommunityTools` | Communities, members/meters, topology preflight, metrics/Sankey, tariffs, calculation, settlements, MEG ownership |
| `tesenso-connectors` | `ConnectorTools` | Connector configs, connection test, jobs (cancel/retry), sync logs, entity mappings |
| `tesenso-connectivity` | `ConnectivityTools` | SIM dashboard, SIM inventory/usage/history, cost leaks, high usage, sync, activate/deactivate |
| `tesenso-datamanagement` | `DataManagementTools` | DM background jobs, bulk telemetry delete (preview/submit/undo), edit, rename |
| `tesenso-ai` | `TesensoAiTools` | Platform AI agents (list/execute/executions), AI chat, message hub channels/timeline/post |
| `tesenso-api` | `TesensoApiTools` | **Generic escape hatch**: `searchTesensoApi` (keyword search over the live OpenAPI spec, ~2400 Tesenso endpoints), `getTesensoApiEndpointDetails`, `callTesensoApi` (executes any `/api/...` call with the session; DELETE requires `confirmDelete=true`) |

`TesensoApiClient` reuses the authenticated `RestTemplate` from upstream `RestClientService`, so the
one configured login (TB JWT or API key) covers both the ThingsBoard API and all Tesenso endpoints
(all Tesenso controllers sit in the TB security chain).

## Deployment on testVM

- **STDIO** (Claude Code, registered as `tesenso-platform`, user scope):
  `java -jar /usr/share/tesenso-mcp/bin/tesenso-mcp.jar` with `THINGSBOARD_URL=http://localhost:8080`.
- **SSE**: systemd unit `tesenso-mcp.service`, port `8091`, bound to `127.0.0.1` only — the SSE
  endpoint has **no own authentication**; remote clients must use an SSH tunnel
  (`ssh -L 8091:127.0.0.1:8091 <vm>`). Credentials in `/etc/tesenso-mcp/env` (mode 600).
- Redeploy after build: `sudo cp target/thingsboard-mcp-server-*.jar /usr/share/tesenso-mcp/bin/tesenso-mcp.jar && sudo systemctl restart tesenso-mcp`.
