package org.thingsboard.ai.mcp.server.tools.tesenso;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.UUID_DESC;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.params;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.putIfNotBlank;
import static org.thingsboard.ai.mcp.server.tools.tesenso.TesensoToolSupport.putIfNotNull;

/**
 * Tesenso platform AI agents and the message hub (Logbuch/channels).
 */
@Service
@RequiredArgsConstructor
@ToolGroup("tesenso-ai")
public class TesensoAiTools implements McpTools {

    private final TesensoApiClient api;

    @Tool(description = "Use this to list the configured platform AI agents of the tenant (name, model, enabled state).")
    public String getAiAgents() {
        return api.get("/api/tesenso/ai/agent/all");
    }

    @Tool(description = "Use this to get one platform AI agent by id.")
    public String getAiAgentById(
            @ToolParam(description = "Agent id. " + UUID_DESC) @NotBlank String agentId) {
        return api.get("/api/tesenso/ai/agent/" + agentId);
    }

    @Tool(description = "Use this to run a platform AI agent with an instruction (non-streaming). " +
            "Returns the execution record; poll getAiAgentExecutionById for the result.")
    public String executeAiAgent(
            @ToolParam(description = "Agent id. " + UUID_DESC) @NotBlank String agentId,
            @ToolParam(description = "Natural-language instruction for the agent.") @NotBlank String instruction) {
        Map<String, String> params = params();
        putIfNotBlank(params, "instruction", instruction);
        return api.post("/api/tesenso/ai/agent/" + agentId + "/execute", params, null);
    }

    @Tool(description = "Use this to list recent platform AI agent executions.")
    public String getAiAgentExecutions(
            @ToolParam(required = false, description = "Page index, default 0.") Integer page,
            @ToolParam(required = false, description = "Page size, default 20.") Integer pageSize) {
        Map<String, String> params = params();
        putIfNotNull(params, "page", page);
        putIfNotNull(params, "pageSize", pageSize);
        return api.get("/api/tesenso/ai/agent/execution", params);
    }

    @Tool(description = "Use this to get one platform AI agent execution by id, including status, result summary and log.")
    public String getAiAgentExecutionById(
            @ToolParam(description = "Execution id. " + UUID_DESC) @NotBlank String executionId) {
        return api.get("/api/tesenso/ai/agent/execution/" + executionId);
    }

    @Tool(description = "Use this to list currently running platform AI agent executions.")
    public String getRunningAiAgentExecutions() {
        return api.get("/api/tesenso/ai/agent/execution/running");
    }

    @Tool(description = "Use this to send a message to the platform AI chat and get the reply (non-streaming). " +
            "Starts a new conversation or continues one when conversationId is given.")
    public String sendAiChatMessage(
            @ToolParam(description = "The chat message text.") @NotBlank String message,
            @ToolParam(required = false, description = "Existing conversation id to continue. " + UUID_DESC)
            String conversationId) {
        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("message", message);
        if (conversationId != null && !conversationId.isBlank()) {
            ObjectNode idNode = body.putObject("conversationId");
            idNode.put("id", conversationId);
        }
        return api.post("/api/tesenso/ai/chat", JacksonUtil.toString(body));
    }

    @Tool(description = "Use this to list the message hub channels of the tenant (team channels).")
    public String getMessageChannels() {
        return api.get("/api/tesenso/messages/channels");
    }

    @Tool(description = "Use this to read the message/logbook timeline of a channel or entity " +
            "(TEAM_CHANNEL, DEVICE or ASSET), newest first.")
    public String getMessageChannelTimeline(
            @ToolParam(description = "Entity type: 'TEAM_CHANNEL', 'DEVICE' or 'ASSET'.") @NotBlank String entityType,
            @ToolParam(description = "Entity id. " + UUID_DESC) @NotBlank String entityId) {
        return api.get("/api/tesenso/messages/channel/" + entityType + "/" + entityId);
    }

    @Tool(description = "Use this to post a message into a channel or entity logbook (e.g. a note on a device or asset).")
    public String postMessageToChannel(
            @ToolParam(description = "Entity type: 'TEAM_CHANNEL', 'DEVICE' or 'ASSET'.") @NotBlank String entityType,
            @ToolParam(description = "Entity id. " + UUID_DESC) @NotBlank String entityId,
            @ToolParam(description = "Message text.") @NotBlank String body,
            @ToolParam(required = false, description = "Optional message title.") String title) {
        ObjectNode payload = JacksonUtil.newObjectNode();
        payload.put("body", body);
        if (title != null && !title.isBlank()) {
            payload.put("title", title);
        }
        return api.post("/api/tesenso/messages/channel/" + entityType + "/" + entityId, JacksonUtil.toString(payload));
    }

}
