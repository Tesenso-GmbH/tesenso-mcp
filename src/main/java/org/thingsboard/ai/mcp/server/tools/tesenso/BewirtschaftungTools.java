package org.thingsboard.ai.mcp.server.tools.tesenso;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.tools.McpTools;

import java.util.Map;

import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.END_TS_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.PAGE_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.PAGE_SIZE_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.START_TS_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.TEXT_SEARCH_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.UUID_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.pageParams;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.params;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.putIfNotNull;

/**
 * Tesenso property management (Bewirtschaftung): cockpit, global search, rental units
 * (Nutzeinheiten), unit consumption/meters and tenant change (Mieterwechsel).
 */
@Service
@RequiredArgsConstructor
@ToolGroup("tesenso-bewirtschaftung")
public class BewirtschaftungTools implements McpTools {

    private final TesensoApiClient api;

    @Tool(description = "Use this to get the Tesenso property-management (Bewirtschaftung) cockpit: vacancy rate, " +
            "overdue open items, running billing runs, open/critical tasks and recent activity for the tenant.")
    public String getBewirtschaftungDashboard() {
        return api.get("/api/tesenso/bewirtschaftung/dashboard");
    }

    @Tool(description = "Use this to search the whole property-management hub at once: tenants/parties (Mieter), " +
            "rental units (Nutzeinheiten), buildings and incoming invoices matching a text.")
    public String searchBewirtschaftung(
            @ToolParam(description = "Search text, min 2 characters.") @NotBlank String text,
            @ToolParam(required = false, description = "Max hits per category, default 5, max 20.") Integer limit) {
        Map<String, String> params = params();
        params.put("text", text);
        putIfNotNull(params, "limit", limit);
        return api.get("/api/tesenso/bewirtschaftung/search", params);
    }

    @Tool(description = "Use this to list rental units (Nutzeinheiten) of the tenant with pagination and optional text search.")
    public String getUnits(
            @ToolParam(required = false, description = PAGE_SIZE_DESC) Integer pageSize,
            @ToolParam(required = false, description = PAGE_DESC) Integer page,
            @ToolParam(required = false, description = TEXT_SEARCH_DESC) String textSearch) {
        return api.get("/api/tesenso/unit/list", pageParams(pageSize, page, textSearch, null, null));
    }

    @Tool(description = "Use this to get one rental unit (Nutzeinheit) by id, including status, tenant, rent and area fields.")
    public String getUnitById(
            @ToolParam(description = "Unit id. " + UUID_DESC) @NotBlank String unitId) {
        return api.get("/api/tesenso/unit/" + unitId);
    }

    @Tool(description = "Use this to list all rental units of one building/property.")
    public String getUnitsByProperty(
            @ToolParam(description = "Building/property asset id. " + UUID_DESC) @NotBlank String propertyId) {
        return api.get("/api/tesenso/unit/list/property/" + propertyId);
    }

    @Tool(description = "Use this to get the energy/water consumption of a rental unit over a period, " +
            "per meter and medium with the unit's share percentage applied.")
    public String getUnitConsumption(
            @ToolParam(description = "Unit id. " + UUID_DESC) @NotBlank String unitId,
            @ToolParam(description = START_TS_DESC) Long fromTs,
            @ToolParam(description = END_TS_DESC) Long toTs) {
        Map<String, String> params = params();
        putIfNotNull(params, "fromTs", fromTs);
        putIfNotNull(params, "toTs", toTs);
        return api.get("/api/tesenso/unit/" + unitId + "/consumption", params);
    }

    @Tool(description = "Use this to list the meters assigned to a rental unit (medium, serial, share percent, channel key).")
    public String getUnitMeters(
            @ToolParam(description = "Unit id. " + UUID_DESC) @NotBlank String unitId) {
        return api.get("/api/tesenso/unit/" + unitId + "/meters");
    }

    @Tool(description = "Use this to get the billing summary of a rental unit for one billing period: " +
            "total cost, advance payments (Akonto), balance (Saldo) and cost-position breakdown.")
    public String getUnitBillingSummary(
            @ToolParam(description = "Unit id. " + UUID_DESC) @NotBlank String unitId,
            @ToolParam(description = "Billing period id. " + UUID_DESC) @NotBlank String billingPeriodId) {
        return api.get("/api/tesenso/unit/" + unitId + "/billing-summary/" + billingPeriodId);
    }

    @Tool(description = "Use this to preview a tenant change (Mieterwechsel) before executing it: shows affected " +
            "occupancy, meters, overlapping billing periods and rent history for a contract unit at a given date. Read-only.")
    public String previewTenantChange(
            @ToolParam(description = "Contract unit id. " + UUID_DESC) @NotBlank String contractUnitId,
            @ToolParam(description = "Change date as epoch milliseconds.") Long changeDate) {
        Map<String, String> params = params();
        putIfNotNull(params, "contractUnitId", contractUnitId);
        putIfNotNull(params, "changeDate", changeDate);
        return api.post("/api/tesenso/bewirtschaftung/tenant-change/preview", params, null);
    }

}
