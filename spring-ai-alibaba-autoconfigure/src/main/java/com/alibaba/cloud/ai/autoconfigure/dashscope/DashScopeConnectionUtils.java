package com.alibaba.cloud.ai.autoconfigure.dashscope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import org.jetbrains.annotations.NotNull;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */
public final class DashScopeConnectionUtils {

	private DashScopeConnectionUtils() {
	}

	public static @NotNull ResolvedConnectionProperties resolveConnectionProperties(
			DashScopeParentProperties commonProperties, DashScopeParentProperties modelProperties, String modelType) {

		String baseUrl = StringUtils.hasText(modelProperties.getBaseUrl()) ? modelProperties.getBaseUrl()
				: commonProperties.getBaseUrl();
		String apiKey = StringUtils.hasText(modelProperties.getApiKey()) ? modelProperties.getApiKey()
				: commonProperties.getApiKey();
		String workspaceId = StringUtils.hasText(modelProperties.getWorkspaceId()) ? modelProperties.getWorkspaceId()
				: commonProperties.getWorkspaceId();

		Map<String, List<String>> connectionHeaders = new HashMap<>();
		if (StringUtils.hasText(workspaceId)) {
			connectionHeaders.put("DashScope-Workspace", List.of(workspaceId));
		}

		// Get apikey from system env.
		if (Objects.isNull(apiKey)) {
			if (Objects.nonNull(System.getenv(DashScopeApiConstants.DASHSCOPE_API_KEY))) {
				apiKey = System.getenv(DashScopeApiConstants.DASHSCOPE_API_KEY);
			}
		}

		Assert.hasText(baseUrl,
				"DashScope base URL must be set.  Use the connection property: spring.ai.dashscope.base-url or spring.ai.dashscope."
						+ modelType + ".base-url property.");
		Assert.hasText(apiKey,
				"DashScope API key must be set. Use the connection property: spring.ai.dashscope.api-key or spring.ai.dashscope."
						+ modelType + ".api-key property.");

		return new ResolvedConnectionProperties(baseUrl, apiKey, workspaceId,
				CollectionUtils.toMultiValueMap(connectionHeaders));
	}

}
