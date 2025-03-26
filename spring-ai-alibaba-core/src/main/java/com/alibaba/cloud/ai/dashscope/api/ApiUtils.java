/*
 * Copyright 2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.*;

/**
 * @author nuocheng.lxm
 * @since 1.0.0-M2
 */
public class ApiUtils {

	public interface HeaderStrategy {

		void applyHeaders(HttpHeaders headers);

	}

	public static class SecurityCheckHeaders implements HeaderStrategy {

		@Override
		public void applyHeaders(HttpHeaders headers) {
			headers.set("X-DashScope-DataInspection", "enable");
		}

	}

	public static class AsyncTaskHeaders implements HeaderStrategy {

		@Override
		public void applyHeaders(HttpHeaders headers) {
			headers.set("X-DashScope-Async", "enable");
		}

	}

	public static class SSEHeaders implements HeaderStrategy {

		@Override
		public void applyHeaders(HttpHeaders headers) {
			headers.set("Cache-Control", "no-cache");
			headers.set("Accept", "text/event-stream");
			headers.set("X-Accel-Buffering", "no");
			headers.set("X-DashScope-SSE", "enable");
		}

	}

	public static class DefaultHeaders implements HeaderStrategy {

		@Override
		public void applyHeaders(HttpHeaders headers) {
			headers.set("Accept", "application/json; charset=utf-8");
		}

	}

	public static Consumer<HttpHeaders> getJsonContentHeaders(String apiKey) {
		return getJsonContentHeaders(apiKey, null);
	}

	public static Consumer<HttpHeaders> getJsonContentHeaders(String apiKey, String workspaceId) {
		return getJsonContentHeaders(apiKey, workspaceId, false);
	}

	public static Consumer<HttpHeaders> getJsonContentHeaders(String apiKey, String workspaceId, boolean stream) {
		return (headers) -> {
			headers.setBearerAuth(apiKey);
			headers.set(HEADER_OPENAPI_SOURCE, SOURCE_FLAG);

			headers.set("user-agent", userAgent());
			if (workspaceId != null) {
				headers.set(HEADER_WORK_SPACE_ID, workspaceId);
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
		headers.put("Authorization", "bearer " + apiKey);
		headers.put("user-agent", userAgent());
		if (workspace != null && !workspace.isEmpty()) {
			headers.put("X-DashScope-WorkSpace", workspace);
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
		List<HeaderStrategy> strategies = new ArrayList<>();

		if (isSecurityCheck)
			strategies.add(new SecurityCheckHeaders());
		if (isAsyncTask)
			strategies.add(new AsyncTaskHeaders());
		strategies.add(isSSE ? new SSEHeaders() : new DefaultHeaders());

		return headers -> {
			headers.setBearerAuth(apiKey);
			headers.set("user-agent", userAgent());
			if (workspace != null && !workspace.isEmpty()) {
				headers.set("X-DashScope-WorkSpace", workspace);
			}
			headers.set("Content-Type", "application/json");

			strategies.forEach(strategy -> strategy.applyHeaders(headers));
		};
	}

	public static Consumer<HttpHeaders> getFileUploadHeaders(Map<String, String> input) {
		return (headers) -> {
			String contentType = input.remove("Content-Type");
			for (Map.Entry<String, String> entry : input.entrySet()) {
				headers.set(entry.getKey(), entry.getValue());
			}
			headers.setContentType(MediaType.parseMediaType((contentType)));
		};
	}

	private static String userAgent() {
		return String.format("%s/%s; java/%s; platform/%s; processor/%s", SDK_FLAG, "1.0.0",
				System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.arch"));
	}

}
