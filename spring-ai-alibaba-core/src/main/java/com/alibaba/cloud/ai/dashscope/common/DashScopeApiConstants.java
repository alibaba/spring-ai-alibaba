package com.alibaba.cloud.ai.dashscope.common;

import com.alibaba.cloud.ai.dashscope.observation.conventions.AiProvider;

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

	public static final String PROVIDER_NAME = AiProvider.DASHSCOPE.value();

}
