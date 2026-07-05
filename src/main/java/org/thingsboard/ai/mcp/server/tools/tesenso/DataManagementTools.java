package org.thingsboard.ai.mcp.server.tools.tesenso;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.tools.McpTools;

import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.PAGE_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.PAGE_SIZE_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.TEXT_SEARCH_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.UUID_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.pageParams;

/**
 * Tesenso data management: telemetry background jobs (bulk delete/edit/rename) with
 * preview, results and undo.
 */
@Service
@RequiredArgsConstructor
@ToolGroup("tesenso-datamanagement")
public class DataManagementTools implements McpTools {

    private final TesensoApiClient api;

    @Tool(description = "Use this to list the data-management background jobs of the tenant " +
            "(bulk telemetry delete/edit/rename), paginated.")
    public String getDataManagementJobs(
            @ToolParam(required = false, description = PAGE_SIZE_DESC) Integer pageSize,
            @ToolParam(required = false, description = PAGE_DESC) Integer page,
            @ToolParam(required = false, description = TEXT_SEARCH_DESC) String textSearch) {
        return api.get("/api/tesenso/data-management/jobs", pageParams(pageSize, page, textSearch, null, null));
    }

    @Tool(description = "Use this to get one data-management background job by id, including its status.")
    public String getDataManagementJobById(
            @ToolParam(description = "Job id. " + UUID_DESC) @NotBlank String jobId) {
        return api.get("/api/tesenso/data-management/jobs/" + jobId);
    }

    @Tool(description = "Use this to cancel a pending or running data-management background job.")
    public String cancelDataManagementJob(
            @ToolParam(description = "Job id. " + UUID_DESC) @NotBlank String jobId) {
        return api.put("/api/tesenso/data-management/jobs/" + jobId + "/cancel", null);
    }

    @Tool(description = "Use this to get the per-entity results of a finished data-management bulk job.")
    public String getDataManagementJobResults(
            @ToolParam(description = "Job id. " + UUID_DESC) @NotBlank String jobId) {
        return api.get("/api/tesenso/data-management/bulk/" + jobId + "/results");
    }

    @Tool(description = "Use this to preview how many telemetry data points a bulk delete would remove. " +
            "Dry-run only, deletes nothing.")
    public String previewBulkTelemetryDelete(
            @ToolParam(description = "Entity id. " + UUID_DESC) @NotBlank String entityId,
            @ToolParam(description = "Telemetry key to delete, e.g. 'temperature'.") @NotBlank String telemetryKey,
            @ToolParam(required = false, description = "Entity type, default 'DEVICE'.") String entityType,
            @ToolParam(required = false, description = "Range start epoch ms, default 0.") Long startTs,
            @ToolParam(required = false, description = "Range end epoch ms, default now.") Long endTs) {
        StringBuilder body = new StringBuilder("{\"entityId\":\"").append(entityId)
                .append("\",\"telemetryKey\":\"").append(telemetryKey).append('"');
        if (entityType != null && !entityType.isBlank()) {
            body.append(",\"entityType\":\"").append(entityType).append('"');
        }
        if (startTs != null) {
            body.append(",\"startTs\":").append(startTs);
        }
        if (endTs != null) {
            body.append(",\"endTs\":").append(endTs);
        }
        body.append('}');
        return api.post("/api/tesenso/data-management/bulk/delete/preview", body.toString());
    }

    @Tool(description = "Use this to submit a bulk telemetry job as a background job JSON. Job types: bulk delete " +
            "(jobPath 'delete'), formula edit ('edit'), key rename ('rename'). Deleted data is backed up and can be " +
            "restored with undoBulkTelemetryJob. Always run previewBulkTelemetryDelete first and confirm with the user.")
    public String submitBulkTelemetryJob(
            @ToolParam(description = "Job path: 'delete', 'edit' or 'rename'.") @NotBlank String jobPath,
            @ToolParam(description = "DmBackgroundJob JSON, e.g. {\"jobName\":\"...\",\"entityId\":\"<uuid>\"," +
                    "\"entityType\":\"DEVICE\",\"jsonData\":\"{\\\"sourceKey\\\":\\\"power\\\",\\\"startTs\\\":0,\\\"endTs\\\":1}\"}. " +
                    "For 'edit' jsonData needs sourceKey+formula (variable 'value'); for 'rename' old/new key info.")
            @NotBlank String jobJson) {
        String normalized = jobPath.trim().toLowerCase();
        if (!normalized.equals("delete") && !normalized.equals("edit") && !normalized.equals("rename")) {
            return "Error: jobPath must be 'delete', 'edit' or 'rename'.";
        }
        return api.post("/api/tesenso/data-management/bulk/" + normalized, jobJson);
    }

    @Tool(description = "Use this to undo a bulk telemetry delete job by restoring the backed-up data points.")
    public String undoBulkTelemetryJob(
            @ToolParam(description = "Job id. " + UUID_DESC) @NotBlank String jobId) {
        return api.post("/api/tesenso/data-management/bulk/" + jobId + "/undo", null);
    }

}
