package com.alibaba.cloud.ai.toolcalling.tavily;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.function.Function;

public class TavilySearchService implements Function<TavilySearchSchema.Request, TavilySearchSchema.Response> {

	private static final Logger logger = LoggerFactory.getLogger(TavilySearchService.class);

	private static final String TAVILY_SEARCH_API = "https://api.tavily.com/search";

	private final WebClient webClient;

	public TavilySearchService(TavilySearchProperties properties) {
		final Map<String, String> headers = Map.of("Authorization", "Bearer " + properties.getToken(), "Content-Type",
				"application/json");
		this.webClient = CommonToolCallUtils.buildWebClient(headers,
				CommonToolCallConstants.DEFAULT_CONNECT_TIMEOUT_MILLIS,
				CommonToolCallConstants.DEFAULT_RESPONSE_TIMEOUT_SECONDS, CommonToolCallConstants.MAX_MEMORY_SIZE);
	}

	@Override
	public TavilySearchSchema.Response apply(TavilySearchSchema.Request request) {
		if (request == null || !StringUtils.hasText(request.query())) {
			return null;
		}

		try {
			return webClient.post()
				.uri(TAVILY_SEARCH_API)
				.bodyValue(request)
				.retrieve()
				.bodyToMono(TavilySearchSchema.Response.class)
				.block();

		}
		catch (Exception ex) {
			logger.error("tavily search error: {}", ex.getMessage());
			return null;
		}
	}

}
