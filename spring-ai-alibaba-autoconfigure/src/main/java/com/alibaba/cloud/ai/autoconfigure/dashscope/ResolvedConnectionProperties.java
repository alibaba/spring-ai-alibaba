package com.alibaba.cloud.ai.autoconfigure.dashscope;

import org.springframework.util.MultiValueMap;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public record ResolvedConnectionProperties(String baseUrl, String apiKey, String workspaceId,
		MultiValueMap<String, String> headers) {
}
