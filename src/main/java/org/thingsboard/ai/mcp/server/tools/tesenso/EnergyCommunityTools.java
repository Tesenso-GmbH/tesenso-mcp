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
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.putIfNotBlank;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.putIfNotNull;

/**
 * Tesenso energy communities (ZEV/vZEV/LEG/MEG): topology, members, metrics, tariffs,
 * calculations, settlements and community billing.
 */
@Service
@RequiredArgsConstructor
@ToolGroup("tesenso-energy-community")
public class EnergyCommunityTools implements McpTools {

    private final TesensoApiClient api;

    @Tool(description = "Use this to list the energy communities (ZEV, vZEV, LEG, MEG) of the tenant, paginated.")
    public String getEnergyCommunities(
            @ToolParam(required = false, description = PAGE_SIZE_DESC) Integer pageSize,
            @ToolParam(required = false, description = PAGE_DESC) Integer page,
            @ToolParam(required = false, description = TEXT_SEARCH_DESC) String textSearch) {
        return api.get("/api/tesenso/energy-communities", pageParams(pageSize, page, textSearch, null, null));
    }

    @Tool(description = "Use this to get one energy community by id, including type, status, grid operator and capacity.")
    public String getEnergyCommunityById(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId) {
        return api.get("/api/tesenso/energy-community/" + communityId);
    }

    @Tool(description = "Use this to list all members of an energy community.")
    public String getEnergyCommunityMembers(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId) {
        return api.get("/api/tesenso/energy-community/" + communityId + "/members");
    }

    @Tool(description = "Use this to list all meters of an energy community across all its buildings.")
    public String getEnergyCommunityMeters(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId) {
        return api.get("/api/tesenso/energy-community/" + communityId + "/meters");
    }

    @Tool(description = "Use this to validate the metering topology (Messkonzept) of an energy community. " +
            "Returns GREEN/YELLOW/RED checks; RED findings block community billing. Read-only.")
    public String validateEnergyCommunityTopology(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId) {
        return api.get("/api/tesenso/energy-community/" + communityId + "/validate/topology");
    }

    @Tool(description = "Use this to get the dashboard summary of an energy community: production, consumption, " +
            "self-consumption and autarky for a date range.")
    public String getEnergyCommunityDashboard(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId,
            @ToolParam(required = false, description = "Start date ISO format e.g. '2026-01-01'; default first of current month.")
            String startDate,
            @ToolParam(required = false, description = "End date ISO format e.g. '2026-01-31'; default today.")
            String endDate) {
        Map<String, String> params = params();
        putIfNotBlank(params, "startDate", startDate);
        putIfNotBlank(params, "endDate", endDate);
        return api.get("/api/tesenso/energy-community/" + communityId + "/dashboard", params);
    }

    @Tool(description = "Use this to get energy time series (production, consumption, grid import/export) " +
            "of an energy community for a time range.")
    public String getEnergyCommunityMetrics(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId,
            @ToolParam(description = START_TS_DESC) Long startTs,
            @ToolParam(description = END_TS_DESC) Long endTs) {
        Map<String, String> params = params();
        putIfNotNull(params, "startTs", startTs);
        putIfNotNull(params, "endTs", endTs);
        return api.get("/api/tesenso/energy-community/" + communityId + "/metrics", params);
    }

    @Tool(description = "Use this to get Sankey energy-flow data (nodes and links) of an energy community for a time range.")
    public String getEnergyCommunitySankeyFlow(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId,
            @ToolParam(description = START_TS_DESC) Long startTs,
            @ToolParam(description = END_TS_DESC) Long endTs,
            @ToolParam(required = false, description = "Medium: 'ELECTRICITY' (default), 'HEAT', 'WATER' or 'ALL'.")
            String medium) {
        Map<String, String> params = params();
        putIfNotNull(params, "startTs", startTs);
        putIfNotNull(params, "endTs", endTs);
        putIfNotBlank(params, "medium", medium);
        return api.get("/api/tesenso/energy-community/" + communityId + "/sankey-flow-data", params);
    }

    @Tool(description = "Use this to get the active tariff configuration of an energy community " +
            "(community price per kWh, grid import price, feed-in tariff, fees, VAT).")
    public String getEnergyCommunityActiveTariff(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId) {
        return api.get("/api/tesenso/energy-community/" + communityId + "/tariff/active");
    }

    @Tool(description = "Use this to save/replace the active tariff of an energy community. " +
            "Validates the solar price cap. Confirm with the user before changing tariffs.")
    public String saveEnergyCommunityTariff(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId,
            @ToolParam(description = "Tariff JSON, e.g. {\"communityPricePerKwh\":0.20,\"gridImportPricePerKwh\":0.25," +
                    "\"gridFeedInTariff\":0.10,\"vatRate\":8.1,\"currency\":\"CHF\"}.")
            @NotBlank String tariffJson) {
        return api.post("/api/tesenso/energy-community/" + communityId + "/tariff", tariffJson);
    }

    @Tool(description = "Use this to trigger an energy allocation/settlement calculation for an energy community " +
            "over a period. Writes calculation results; confirm with the user first.")
    public String triggerEnergyCommunityCalculation(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId,
            @ToolParam(description = "Period start. " + START_TS_DESC) Long periodStart,
            @ToolParam(description = "Period end. " + END_TS_DESC) Long periodEnd) {
        Map<String, String> params = params();
        putIfNotBlank(params, "communityId", communityId);
        putIfNotNull(params, "periodStart", periodStart);
        putIfNotNull(params, "periodEnd", periodEnd);
        return api.post("/api/tesenso/energy-community/calculation/trigger", params, null);
    }

    @Tool(description = "Use this to list the calculation results of an energy community.")
    public String getEnergyCommunityCalculationResults(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId) {
        return api.get("/api/tesenso/energy-community/calculation/" + communityId + "/results");
    }

    @Tool(description = "Use this to list all monthly settlements of an energy community.")
    public String getEnergyCommunitySettlements(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId) {
        return api.get("/api/tesenso/energy-community/settlement/community/" + communityId);
    }

    @Tool(description = "Use this to get one settlement of an energy community by settlement id, " +
            "or its per-member rows when includeMembers is true.")
    public String getEnergyCommunitySettlementById(
            @ToolParam(description = "Settlement id. " + UUID_DESC) @NotBlank String settlementId,
            @ToolParam(required = false, description = "If true, returns the per-member settlement rows instead of the header.")
            Boolean includeMembers) {
        String path = "/api/tesenso/energy-community/settlement/" + settlementId;
        if (Boolean.TRUE.equals(includeMembers)) {
            path += "/members";
        }
        return api.get(path);
    }

    @Tool(description = "Use this to generate the monthly settlement of an energy community for a month. " +
            "Writes a settlement; confirm with the user first.")
    public String generateEnergyCommunitySettlement(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId,
            @ToolParam(description = "Settlement month in 'yyyy-MM' format, e.g. '2026-06'.") @NotBlank String month) {
        Map<String, String> params = params();
        putIfNotBlank(params, "communityId", communityId);
        putIfNotBlank(params, "month", month);
        return api.post("/api/tesenso/energy-community/settlement/generate", params, null);
    }

    @Tool(description = "Use this to list the community billing periods of an energy community " +
            "(status, totals, self-sufficiency and autarky rates).")
    public String getEnergyCommunityBillingPeriods(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId) {
        return api.get("/api/tesenso/energy-community/" + communityId + "/billing/periods");
    }

    @Tool(description = "Use this to list the MEG PV co-owners of an energy community with their ownership share percent.")
    public String getMegOwnerships(
            @ToolParam(description = "Energy community id. " + UUID_DESC) @NotBlank String communityId) {
        Map<String, String> params = params();
        putIfNotBlank(params, "communityId", communityId);
        return api.get("/api/tesenso/meg-ownership", params);
    }

}
