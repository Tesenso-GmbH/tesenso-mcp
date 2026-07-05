package org.thingsboard.ai.mcp.server.tools.tesenso;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.server.common.data.StringUtils;

import java.net.URI;
import java.util.Map;

/**
 * Thin HTTP helper for Tesenso platform endpoints (/api/tesenso/...) that are not covered
 * by the typed ThingsBoard RestClient. Reuses the authenticated RestTemplate from
 * RestClientService, so the same JWT/API-key session covers both TB CE and Tesenso APIs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TesensoApiClient {

    private final RestClientService clientService;

    @Value("${thingsboard.url:}")
    private String baseUrl;

    public String get(String path) {
        return exchange(HttpMethod.GET, path, null, null);
    }

    public String get(String path, Map<String, String> queryParams) {
        return exchange(HttpMethod.GET, path, queryParams, null);
    }

    public String post(String path, String jsonBody) {
        return exchange(HttpMethod.POST, path, null, jsonBody);
    }

    public String post(String path, Map<String, String> queryParams, String jsonBody) {
        return exchange(HttpMethod.POST, path, queryParams, jsonBody);
    }

    public String put(String path, String jsonBody) {
        return exchange(HttpMethod.PUT, path, null, jsonBody);
    }

    public String delete(String path, Map<String, String> queryParams) {
        return exchange(HttpMethod.DELETE, path, queryParams, null);
    }

    public String exchange(HttpMethod method, String path, Map<String, String> queryParams, String jsonBody) {
        if (clientService.getClient() == null) {
            return errorJson(503, "Not connected to the platform yet. Check THINGSBOARD_URL and credentials.");
        }
        if (StringUtils.isBlank(baseUrl)) {
            return errorJson(503, "thingsboard.url is not configured.");
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).path(path);
        if (queryParams != null) {
            queryParams.forEach((k, v) -> {
                if (k != null && v != null) {
                    builder.queryParam(k, v);
                }
            });
        }
        URI uri = builder.encode().build().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        try {
            ResponseEntity<String> response = clientService.getClient().getRestTemplate()
                    .exchange(uri, method, entity, String.class);
            String body = response.getBody();
            if (StringUtils.isBlank(body)) {
                return "{\"status\":" + response.getStatusCode().value() + ",\"result\":\"OK\"}";
            }
            return body;
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            return errorJson(e.getStatusCode().value(), StringUtils.isBlank(body) ? e.getStatusText() : body);
        } catch (RestClientException e) {
            log.debug("Tesenso API call failed: {} {}", method, path, e);
            return errorJson(502, e.getMessage());
        }
    }

    private static String errorJson(int status, String message) {
        return "{\"status\":" + status + ",\"error\":" + quote(message) + "}";
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

}
