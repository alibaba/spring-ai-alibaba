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

package com.alibaba.cloud.ai.dashscope.common;

/**
 * @author nuocheng.lxm
 * @since 1.0.0-M2
 */
public final class DashScopeApiConstants {

	public static final String HEADER_REQUEST_ID = "X-Request-Id";

	public static final String HEADER_OPENAPI_SOURCE = "X-DashScope-OpenAPISource";

	public static final String HEADER_WORK_SPACE_ID = "X-DashScope-Workspace";

	public static final String SOURCE_FLAG = "CloudSDK";

	public static final String SDK_FLAG = "SpringAiAlibaba";

	public static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com";

	public static final String DASHSCOPE_API_KEY = "AI_DASHSCOPE_API_KEY";

	public static final String DEFAULT_WEBSOCKET_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference/";

	public static final Integer DEFAULT_READ_TIMEOUT = 60;

}
