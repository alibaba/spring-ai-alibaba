package com.alibaba.cloud.ai.autoconfigure.dashscope;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionUtils.resolveConnectionProperties;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public class DashScopeRerankAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DashScopeRerankModel dashscopeRerankModel(DashScopeConnectionProperties commonProperties,
			DashScopeRerankProperties rerankProperties, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, RetryTemplate retryTemplate,
			ResponseErrorHandler responseErrorHandler) {

		ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties, rerankProperties,
				"rerank");

		var dashScopeApi = DashScopeApi.builder()
			.apiKey(resolved.apiKey())
			.headers(resolved.headers())
			.baseUrl(resolved.baseUrl())
			.webClientBuilder(webClientBuilder)
			.workSpaceId(resolved.workspaceId())
			.restClientBuilder(restClientBuilder)
			.responseErrorHandler(responseErrorHandler)
			.build();

		return new DashScopeRerankModel(dashScopeApi, rerankProperties.getOptions(), retryTemplate);
	}

}
