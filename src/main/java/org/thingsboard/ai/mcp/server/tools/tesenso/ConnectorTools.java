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
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.putIfNotBlank;

/**
 * Tesenso Connector Center: external integrations (smart-me, SolarLog, Zaptec, Bexio, Zevvy,
 * Minergie, NeoVac, ...) with their sync jobs, logs and entity mappings.
 */
@Service
@RequiredArgsConstructor
@ToolGroup("tesenso-connectors")
public class ConnectorTools implements McpTools {

    private final TesensoApiClient api;

    @Tool(description = "Use this to list all connectors (external integrations) of the tenant, e.g. smart-me, " +
            "SolarLog, Zaptec, Bexio, Zevvy, Minergie, NeoVac. Shows type, name and enabled state.")
    public String getConnectors() {
        return api.get("/api/tesenso/connectors/all");
    }

    @Tool(description = "Use this to get one connector configuration by id.")
    public String getConnectorById(
            @ToolParam(description = "Connector id. " + UUID_DESC) @NotBlank String connectorId) {
        return api.get("/api/tesenso/connectors/" + connectorId);
    }

    @Tool(description = "Use this to test the connection of a connector against the external system. " +
            "Returns status and message. Does not modify data.")
    public String testConnectorConnection(
            @ToolParam(description = "Connector id. " + UUID_DESC) @NotBlank String connectorId) {
        return api.post("/api/tesenso/connectors/" + connectorId + "/test", null);
    }

    @Tool(description = "Use this to list the import/export jobs of one connector, optionally filtered by status.")
    public String getConnectorJobsForConnector(
            @ToolParam(description = "Connector id. " + UUID_DESC) @NotBlank String connectorId,
            @ToolParam(required = false, description = PAGE_SIZE_DESC) Integer pageSize,
            @ToolParam(required = false, description = PAGE_DESC) Integer page,
            @ToolParam(required = false, description = "Job status filter, e.g. 'PENDING', 'RUNNING', 'COMPLETED', 'FAILED'.")
            String status) {
        Map<String, String> params = pageParams(pageSize, page, null, null, null);
        putIfNotBlank(params, "status", status);
        return api.get("/api/tesenso/connectors/" + connectorId + "/jobs", params);
    }

    @Tool(description = "Use this to list all connector jobs of the tenant across connectors, optionally filtered by status.")
    public String getConnectorJobs(
            @ToolParam(required = false, description = PAGE_SIZE_DESC) Integer pageSize,
            @ToolParam(required = false, description = PAGE_DESC) Integer page,
            @ToolParam(required = false, description = "Job status filter, e.g. 'PENDING', 'RUNNING', 'COMPLETED', 'FAILED'.")
            String status) {
        Map<String, String> params = pageParams(pageSize, page, null, null, null);
        putIfNotBlank(params, "status", status);
        return api.get("/api/tesenso/connector-jobs", params);
    }

    @Tool(description = "Use this to get one connector job by id, including its status and progress.")
    public String getConnectorJobById(
            @ToolParam(description = "Job id. " + UUID_DESC) @NotBlank String jobId) {
        return api.get("/api/tesenso/connector-jobs/" + jobId);
    }

    @Tool(description = "Use this to cancel a running connector job.")
    public String cancelConnectorJob(
            @ToolParam(description = "Job id. " + UUID_DESC) @NotBlank String jobId) {
        return api.post("/api/tesenso/connector-jobs/" + jobId + "/cancel", null);
    }

    @Tool(description = "Use this to retry a failed connector job.")
    public String retryConnectorJob(
            @ToolParam(description = "Job id. " + UUID_DESC) @NotBlank String jobId) {
        return api.post("/api/tesenso/connector-jobs/" + jobId + "/retry", null);
    }

    @Tool(description = "Use this to list connector sync logs, either all or scoped to one connector.")
    public String getConnectorSyncLogs(
            @ToolParam(required = false, description = "Optional connector id to scope the logs. " + UUID_DESC)
            String connectorId,
            @ToolParam(required = false, description = PAGE_SIZE_DESC) Integer pageSize,
            @ToolParam(required = false, description = PAGE_DESC) Integer page,
            @ToolParam(required = false, description = TEXT_SEARCH_DESC) String textSearch) {
        Map<String, String> params = pageParams(pageSize, page, textSearch, null, null);
        String path = connectorId == null || connectorId.isBlank()
                ? "/api/tesenso/connector-sync-logs"
                : "/api/tesenso/connector-sync-logs/by-connector/" + connectorId;
        return api.get(path, params);
    }

    @Tool(description = "Use this to get entity mapping statistics of a connector (how many external entities are mapped/synced).")
    public String getConnectorMappingStats(
            @ToolParam(description = "Connector id. " + UUID_DESC) @NotBlank String connectorId) {
        return api.get("/api/tesenso/connectors/" + connectorId + "/entity-mappings/stats");
    }

    @Tool(description = "Use this to list the entity mappings of a connector (external entity ↔ platform entity), paginated.")
    public String getConnectorMappings(
            @ToolParam(description = "Connector id. " + UUID_DESC) @NotBlank String connectorId,
            @ToolParam(required = false, description = PAGE_SIZE_DESC) Integer pageSize,
            @ToolParam(required = false, description = PAGE_DESC) Integer page,
            @ToolParam(required = false, description = TEXT_SEARCH_DESC) String textSearch) {
        return api.get("/api/tesenso/connectors/" + connectorId + "/entity-mappings",
                pageParams(pageSize, page, textSearch, null, null));
    }

}
