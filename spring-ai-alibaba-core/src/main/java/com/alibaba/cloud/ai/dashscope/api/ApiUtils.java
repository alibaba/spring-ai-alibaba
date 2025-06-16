/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dashscope.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * @author nuocheng.lxm
 * @since 1.0.0-M2
 */
public class ApiUtils {

	private static final String USER_AGENT = userAgent();

	public static Consumer<HttpHeaders> getJsonContentHeaders(String apiKey) {
		return getJsonContentHeaders(apiKey, null);
	}

	public static Consumer<HttpHeaders> getJsonContentHeaders(String apiKey, String workspaceId) {
		return getJsonContentHeaders(apiKey, workspaceId, false);
	}

	public static Consumer<HttpHeaders> getJsonContentHeaders(String apiKey, String workspaceId, boolean stream) {
		return (headers) -> {
			headers.setBearerAuth(apiKey);
			headers.set(DashScopeApiConstants.HEADER_OPENAPI_SOURCE, DashScopeApiConstants.SOURCE_FLAG);

			headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
			if (workspaceId != null) {
				headers.set(DashScopeApiConstants.HEADER_WORK_SPACE_ID, workspaceId);
			}
			headers.setContentType(MediaType.APPLICATION_JSON);
			if (stream) {
				headers.set("X-DashScope-SSE", "enable");
			}
		};
	}

	public static Map<String, String> getMapContentHeaders(String apiKey, boolean isSecurityCheck, String workspace,
			Map<String, String> customHeaders) {
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.AUTHORIZATION, "bearer " + apiKey);
		headers.put(HttpHeaders.USER_AGENT, USER_AGENT);
		if (workspace != null && !workspace.isEmpty()) {
			headers.put(DashScopeApiConstants.HEADER_WORK_SPACE_ID, workspace);
		}
		if (isSecurityCheck) {
			headers.put("X-DashScope-DataInspection", "enable");
		}
		if (customHeaders != null && !customHeaders.isEmpty()) {
			headers.putAll(customHeaders);
		}
		return headers;
	}

	public static Consumer<HttpHeaders> getAudioTranscriptionHeaders(String apiKey, String workspace,
			Boolean isAsyncTask, Boolean isSecurityCheck, Boolean isSSE) {
		return (headers) -> {
			headers.setBearerAuth(apiKey);
			headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
			if (isSecurityCheck) {
				headers.set("X-DashScope-DataInspection", "enable");
			}

			if (workspace != null && !workspace.isEmpty()) {
				headers.set(DashScopeApiConstants.HEADER_WORK_SPACE_ID, workspace);
			}

			if (isAsyncTask) {
				headers.set("X-DashScope-Async", "enable");
			}

			headers.setContentType(MediaType.APPLICATION_JSON);
			if (isSSE) {
				headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");
				headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM));
				headers.set("X-Accel-Buffering", "no");
				headers.set("X-DashScope-SSE", "enable");
			}
			else {
				headers.setAccept(List.of(MediaType.parseMediaType("application/json; charset=utf-8")));
			}
		};
	}

	public static Consumer<HttpHeaders> getFileUploadHeaders(Map<String, String> input) {
		return (headers) -> {
			String contentType = input.remove(HttpHeaders.CONTENT_TYPE);
			for (Map.Entry<String, String> entry : input.entrySet()) {
				headers.set(entry.getKey(), entry.getValue());
			}
			headers.setContentType(MediaType.parseMediaType((contentType)));
		};
	}

	private static String userAgent() {
		return String.format("%s/%s; java/%s; platform/%s; processor/%s", DashScopeApiConstants.SDK_FLAG, "1.0.0",
				System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.arch"));
	}

}
