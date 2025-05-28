/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.tool.tavily;

import com.alibaba.cloud.ai.example.deepresearch.model.TavilySearchRequest;
import com.alibaba.cloud.ai.example.deepresearch.model.TavilySearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author yingzi
 * @since 2025/5/18 14:36
 */
@Service
public class TavilySearchApi {

	private final String URL = "https://api.tavily.com/search";

	private final WebClient webClient;

	private final TavilySearchProperties properties;

	public TavilySearchApi(TavilySearchProperties properties) {
		this.webClient = WebClient.builder()
			.baseUrl(URL)
			.defaultHeader("Authorization", "Bearer " + properties.getApiKey())
			.build();
		this.properties = properties;
	}

	public TavilySearchResponse search(String query) {
		TavilySearchRequest build = TavilySearchRequest.builder()
			.query(query)
			.topic(properties.getTopic())
			.searchDepth(properties.getSearchDepth())
			.chunksPerSource(properties.getChunksPerSource())
			.maxResults(properties.getMaxResults())
			.days(properties.getDays())
			.includeRawContent(properties.isIncludeRawContent())
			.includeImages(properties.isIncludeImages())
			.includeImageDescriptions(properties.isIncludeImageDescriptions())
			.includeAnswer(properties.isIncludeAnswer())
			.build();
		TavilySearchResponse response = webClient.post()
			.bodyValue(build)
			.retrieve()
			.bodyToMono(TavilySearchResponse.class)
			.block();
		return response;
	}

}
