package org.thingsboard.ai.mcp.server.tools.tesenso;

import org.thingsboard.server.common.data.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small shared helpers for the Tesenso tool classes.
 */
final class TesensoToolSupport {

    static final String PAGE_SIZE_DESC = "Maximum number of results per page, e.g. 10.";
    static final String PAGE_DESC = "Page index starting from 0.";
    static final String TEXT_SEARCH_DESC = "Optional case-insensitive substring filter.";
    static final String UUID_DESC = "UUID string, e.g. '784f394c-42b6-435a-983c-b7beff2784f9'.";
    static final String START_TS_DESC = "Start of time range as epoch milliseconds.";
    static final String END_TS_DESC = "End of time range as epoch milliseconds.";

    private TesensoToolSupport() {
    }

    static Map<String, String> params() {
        return new LinkedHashMap<>();
    }

    static Map<String, String> pageParams(Integer pageSize, Integer page, String textSearch,
                                          String sortProperty, String sortOrder) {
        Map<String, String> params = params();
        params.put("pageSize", String.valueOf(pageSize == null ? 10 : pageSize));
        params.put("page", String.valueOf(page == null ? 0 : page));
        putIfNotBlank(params, "textSearch", textSearch);
        putIfNotBlank(params, "sortProperty", sortProperty);
        putIfNotBlank(params, "sortOrder", sortOrder);
        return params;
    }

    static void putIfNotBlank(Map<String, String> params, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            params.put(key, value);
        }
    }

    static void putIfNotNull(Map<String, String> params, String key, Object value) {
        if (value != null) {
            params.put(key, String.valueOf(value));
        }
    }

}
