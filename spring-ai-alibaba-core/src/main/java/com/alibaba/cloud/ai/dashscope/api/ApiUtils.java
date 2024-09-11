/*
 * Copyright 2023-2024 the original author or authors.
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

import java.util.Map;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.HEADER_OPENAPI_SOURCE;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.HEADER_WORK_SPACE_ID;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.SOURCE_FLAG;

/**
 * @author nuocheng.lxm
 * @date 2024/7/23 17:53
 */
public class ApiUtils {

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
			if (workspaceId != null) {
				headers.set(HEADER_WORK_SPACE_ID, workspaceId);
			}
			headers.setContentType(MediaType.APPLICATION_JSON);
			if (stream) {
				headers.set("X-DashScope-SSE", "enable");
			}
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

}
