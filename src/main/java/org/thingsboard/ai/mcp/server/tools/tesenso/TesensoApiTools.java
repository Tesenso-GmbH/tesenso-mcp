package org.thingsboard.ai.mcp.server.tools.tesenso;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Generic "escape hatch" over the whole Tesenso platform REST API. The platform exposes ~2400
 * OpenAPI-documented endpoints under /api/tesenso — far too many for individual MCP tools.
 * These three tools let an LLM discover any endpoint from the live OpenAPI spec and call it
 * with the authenticated session.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ToolGroup("tesenso-api")
public class TesensoApiTools implements McpTools {

    private static final long SPEC_CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final int MAX_SEARCH_RESULTS = 50;
    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final TesensoApiClient api;

    private volatile JsonNode cachedSpec;
    private volatile long cachedSpecTs;

    @Tool(description = "Use this to discover Tesenso platform REST endpoints by keyword when no dedicated tool exists. " +
            "Searches the live OpenAPI spec (~2400 Tesenso endpoints: Bewirtschaftung, contracts, energy communities, billing, " +
            "connectors, SIM connectivity, data management, reports, BIM, marketplace, gateways, and more). " +
            "Returns matching endpoints as 'METHOD path — summary'. " +
            "Then call getTesensoApiEndpointDetails for parameter/schema details and callTesensoApi to execute.")
    public String searchTesensoApi(
            @ToolParam(description = "Space-separated keywords to match against endpoint path, summary, description and tag. " +
                    "Use English or German domain terms, e.g. 'sim card usage', 'vertrag', 'settlement', 'connector job'.")
            @NotBlank String keywords,
            @ToolParam(required = false, description = "Max results, default 25, max 50.")
            Integer limit) {
        JsonNode spec = getSpec();
        if (spec == null) {
            return "Error: could not load the OpenAPI spec from the platform.";
        }
        String[] terms = keywords.toLowerCase(Locale.ROOT).trim().split("\\s+");
        int max = limit == null ? 25 : Math.min(Math.max(limit, 1), MAX_SEARCH_RESULTS);

        record Hit(int score, String line) {}
        List<Hit> hits = new ArrayList<>();
        JsonNode paths = spec.path("paths");
        Iterator<Map.Entry<String, JsonNode>> it = paths.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = it.next();
            String path = pathEntry.getKey();
            Iterator<Map.Entry<String, JsonNode>> ops = pathEntry.getValue().fields();
            while (ops.hasNext()) {
                Map.Entry<String, JsonNode> op = ops.next();
                String method = op.getKey().toUpperCase(Locale.ROOT);
                if (!ALLOWED_METHODS.contains(method)) {
                    continue;
                }
                JsonNode def = op.getValue();
                String summary = def.path("summary").asText("");
                String description = def.path("description").asText("");
                String tag = def.path("tags").isArray() && !def.path("tags").isEmpty()
                        ? def.path("tags").get(0).asText("") : "";
                String pathLower = path.toLowerCase(Locale.ROOT);
                String textLower = (summary + " " + description + " " + tag).toLowerCase(Locale.ROOT);
                int score = 0;
                for (String term : terms) {
                    boolean inPath = pathLower.contains(term);
                    boolean inText = textLower.contains(term);
                    if (inPath) {
                        score += 2;
                    }
                    if (inText) {
                        score += 1;
                    }
                    if (!inPath && !inText) {
                        score = 0;
                        break;
                    }
                }
                if (score > 0) {
                    String line = method + " " + path + (StringUtils.isBlank(summary) ? "" : " — " + summary);
                    hits.add(new Hit(score, line));
                }
            }
        }
        if (hits.isEmpty()) {
            return "No endpoints matched '" + keywords + "'. Try fewer or different keywords (English and German terms are used in the API).";
        }
        hits.sort(Comparator.comparingInt(Hit::score).reversed().thenComparing(Hit::line));
        StringBuilder sb = new StringBuilder();
        sb.append(hits.size()).append(" endpoints matched");
        if (hits.size() > max) {
            sb.append(" (showing top ").append(max).append(", refine keywords for more precision)");
        }
        sb.append(":\n");
        hits.stream().limit(max).forEach(h -> sb.append(h.line()).append('\n'));
        return sb.toString();
    }

    @Tool(description = "Use this to get full parameter and request-body schema details for one Tesenso platform endpoint " +
            "found via searchTesensoApi, before calling it with callTesensoApi.")
    public String getTesensoApiEndpointDetails(
            @ToolParam(description = "HTTP method, e.g. 'GET' or 'POST'.") @NotBlank String method,
            @ToolParam(description = "Endpoint path exactly as returned by searchTesensoApi, e.g. '/api/tesenso/sim-cards'.")
            @NotBlank String path) {
        JsonNode spec = getSpec();
        if (spec == null) {
            return "Error: could not load the OpenAPI spec from the platform.";
        }
        JsonNode def = spec.path("paths").path(path).path(method.toLowerCase(Locale.ROOT));
        if (def.isMissingNode()) {
            return "Endpoint not found in spec: " + method.toUpperCase(Locale.ROOT) + " " + path +
                    ". Use searchTesensoApi to find the exact path.";
        }
        ObjectNode out = JacksonUtil.newObjectNode();
        out.put("method", method.toUpperCase(Locale.ROOT));
        out.put("path", path);
        if (def.hasNonNull("summary")) {
            out.put("summary", def.get("summary").asText());
        }
        if (def.hasNonNull("description")) {
            out.put("description", def.get("description").asText());
        }
        if (def.has("parameters")) {
            ArrayNode params = out.putArray("parameters");
            for (JsonNode p : def.get("parameters")) {
                ObjectNode param = params.addObject();
                param.put("name", p.path("name").asText());
                param.put("in", p.path("in").asText());
                param.put("required", p.path("required").asBoolean(false));
                JsonNode schema = resolveRef(spec, p.path("schema"), 1);
                if (!schema.isMissingNode()) {
                    param.set("schema", schema);
                }
                if (p.hasNonNull("description")) {
                    param.put("description", p.get("description").asText());
                }
            }
        }
        JsonNode bodySchema = def.path("requestBody").path("content").path("application/json").path("schema");
        if (!bodySchema.isMissingNode()) {
            out.set("requestBodySchema", resolveRef(spec, bodySchema, 2));
        }
        String result = JacksonUtil.toString(out);
        if (result != null && result.length() > 20000) {
            result = result.substring(0, 20000) + "... (truncated)";
        }
        return result;
    }

    @Tool(description = "Use this to call any Tesenso platform REST endpoint that has no dedicated tool. " +
            "Executes the request with the authenticated session of the configured platform user. " +
            "Discover endpoints with searchTesensoApi and check parameters with getTesensoApiEndpointDetails first. " +
            "Fill path variables like {id} directly into the path. Returns the JSON response, " +
            "or {\"status\":..., \"error\":...} on failure.")
    public String callTesensoApi(
            @ToolParam(description = "HTTP method: GET, POST, PUT, PATCH or DELETE.") @NotBlank String method,
            @ToolParam(description = "Endpoint path starting with /api/, with path variables already substituted, " +
                    "e.g. '/api/tesenso/connectors/3fa85f64-.../jobs'.") @NotBlank String path,
            @ToolParam(required = false, description = "Query parameters as flat JSON object, e.g. {\"pageSize\":\"10\",\"page\":\"0\"}.")
            String queryParamsJson,
            @ToolParam(required = false, description = "JSON request body for POST/PUT/PATCH.")
            String bodyJson,
            @ToolParam(required = false, description = "Must be true to execute a DELETE request. Ask the user before deleting.")
            Boolean confirmDelete) {
        String normalizedMethod = method.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_METHODS.contains(normalizedMethod)) {
            return "Error: unsupported method '" + method + "'. Allowed: " + ALLOWED_METHODS + ".";
        }
        String normalizedPath = path.trim();
        if (!normalizedPath.startsWith("/api/") || normalizedPath.contains("://") || normalizedPath.contains("..")) {
            return "Error: path must be a platform API path starting with /api/.";
        }
        if ("DELETE".equals(normalizedMethod) && !Boolean.TRUE.equals(confirmDelete)) {
            return "Error: DELETE requires confirmDelete=true. Confirm the deletion with the user first.";
        }
        Map<String, String> queryParams = null;
        if (StringUtils.isNotBlank(queryParamsJson)) {
            JsonNode node = JacksonUtil.toJsonNode(queryParamsJson);
            if (node == null || !node.isObject()) {
                return "Error: queryParamsJson must be a flat JSON object.";
            }
            queryParams = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                queryParams.put(field.getKey(), field.getValue().asText());
            }
        }
        String body = StringUtils.isBlank(bodyJson) ? null : bodyJson;
        return api.exchange(HttpMethod.valueOf(normalizedMethod), normalizedPath, queryParams, body);
    }

    private JsonNode getSpec() {
        long now = System.currentTimeMillis();
        JsonNode spec = cachedSpec;
        if (spec != null && now - cachedSpecTs < SPEC_CACHE_TTL_MS) {
            return spec;
        }
        try {
            String raw = api.get("/v3/api-docs");
            JsonNode parsed = JacksonUtil.toJsonNode(raw);
            if (parsed != null && parsed.has("paths")) {
                cachedSpec = parsed;
                cachedSpecTs = now;
                return parsed;
            }
            log.warn("OpenAPI spec response had no 'paths' node");
        } catch (Exception e) {
            log.warn("Failed to load OpenAPI spec", e);
        }
        return cachedSpec;
    }

    private JsonNode resolveRef(JsonNode spec, JsonNode schema, int depth) {
        if (schema == null || schema.isMissingNode() || depth < 0) {
            return JacksonUtil.newObjectNode();
        }
        if (schema.has("$ref")) {
            String ref = schema.get("$ref").asText();
            String name = ref.substring(ref.lastIndexOf('/') + 1);
            JsonNode resolved = spec.path("components").path("schemas").path(name);
            if (resolved.isMissingNode() || depth == 0) {
                ObjectNode stub = JacksonUtil.newObjectNode();
                stub.put("type", "object");
                stub.put("schemaName", name);
                return stub;
            }
            return resolveRef(spec, resolved, depth - 1);
        }
        if (schema.isObject() && schema.has("properties")) {
            ObjectNode copy = ((ObjectNode) schema).deepCopy();
            ObjectNode props = (ObjectNode) copy.get("properties");
            Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
            ObjectNode newProps = JacksonUtil.newObjectNode();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                newProps.set(field.getKey(), resolveRef(spec, field.getValue(), depth - 1));
            }
            copy.set("properties", newProps);
            return copy;
        }
        return schema;
    }

}
