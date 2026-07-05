package org.thingsboard.ai.mcp.server.tools.tesenso;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.tools.McpTools;

import java.util.Map;

import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.PAGE_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.PAGE_SIZE_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.TEXT_SEARCH_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.UUID_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.pageParams;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.params;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.putIfNotNull;

/**
 * Tesenso SIM card connectivity: dashboard, SIM inventory, usage, cost leaks and provider sync.
 */
@Service
@RequiredArgsConstructor
@ToolGroup("tesenso-connectivity")
public class ConnectivityTools implements McpTools {

    private final TesensoApiClient api;

    @Tool(description = "Use this to get the tenant-wide SIM connectivity dashboard: SIM counts, status distribution, " +
            "data usage and cost overview.")
    public String getConnectivityDashboard() {
        return api.get("/api/tesenso/connectivity/dashboard");
    }

    @Tool(description = "Use this to list SIM cards of the tenant enriched with assigned device/gateway names, paginated.")
    public String getSimCards(
            @ToolParam(required = false, description = PAGE_SIZE_DESC) Integer pageSize,
            @ToolParam(required = false, description = PAGE_DESC) Integer page,
            @ToolParam(required = false, description = TEXT_SEARCH_DESC) String textSearch) {
        return api.get("/api/tesenso/sim-cards/list/info", pageParams(pageSize, page, textSearch, null, null));
    }

    @Tool(description = "Use this to get one SIM card by id, enriched with assigned device/gateway info.")
    public String getSimCardById(
            @ToolParam(description = "SIM card id. " + UUID_DESC) @NotBlank String simCardId) {
        return api.get("/api/tesenso/sim-cards/" + simCardId + "/info");
    }

    @Tool(description = "Use this to get the current usage (data, SMS) of a SIM card from the provider.")
    public String getSimCardUsage(
            @ToolParam(description = "SIM card id. " + UUID_DESC) @NotBlank String simCardId) {
        return api.get("/api/tesenso/connectivity/sim-cards/" + simCardId + "/usage");
    }

    @Tool(description = "Use this to get the daily usage history of a SIM card.")
    public String getSimCardUsageHistory(
            @ToolParam(description = "SIM card id. " + UUID_DESC) @NotBlank String simCardId,
            @ToolParam(required = false, description = "Number of days back, default 30.") Integer days) {
        Map<String, String> params = params();
        putIfNotNull(params, "days", days);
        return api.get("/api/tesenso/connectivity/sim-cards/" + simCardId + "/usage/history", params);
    }

    @Tool(description = "Use this to find SIM cost leaks: SIM cards that cost money but are unassigned or inactive.")
    public String getSimCardCostLeaks() {
        return api.get("/api/tesenso/sim-cards/cost-leak");
    }

    @Tool(description = "Use this to find SIM cards with high data usage relative to their plan.")
    public String getHighUsageSimCards(
            @ToolParam(required = false, description = "Usage threshold between 0.0 and 1.0, default 0.8 (80% of plan).")
            Double threshold) {
        Map<String, String> params = params();
        putIfNotNull(params, "threshold", threshold);
        return api.get("/api/tesenso/sim-cards/high-usage", params);
    }

    @Tool(description = "Use this to sync one SIM card's state and usage from the connectivity provider.")
    public String syncSimCard(
            @ToolParam(description = "SIM card id. " + UUID_DESC) @NotBlank String simCardId) {
        return api.post("/api/tesenso/connectivity/sim-cards/" + simCardId + "/sync", null);
    }

    @Tool(description = "Use this to activate or deactivate a SIM card at the provider. " +
            "Confirm with the user before changing SIM state.")
    public String setSimCardActive(
            @ToolParam(description = "SIM card id. " + UUID_DESC) @NotBlank String simCardId,
            @ToolParam(description = "true to activate, false to deactivate.") Boolean active) {
        String action = Boolean.TRUE.equals(active) ? "activate" : "deactivate";
        return api.post("/api/tesenso/connectivity/sim-cards/" + simCardId + "/" + action, null);
    }

}
