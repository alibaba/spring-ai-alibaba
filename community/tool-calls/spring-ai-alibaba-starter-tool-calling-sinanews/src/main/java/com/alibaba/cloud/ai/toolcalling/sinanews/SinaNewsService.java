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
package com.alibaba.cloud.ai.toolcalling.sinanews;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author XiaoYunTao
 * @since 2024/12/18
 */
public class SinaNewsService implements Function<SinaNewsService.Request, SinaNewsService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(SinaNewsService.class);

	private static final String API_URL = "https://newsapp.sina.cn/api/hotlist?newsId=HB-1-snhs%2Ftop_news_list-all";

	private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

	private static final String ACCEPT_ENCODING = "gzip, deflate";

	private static final String ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,ja;q=0.8";

	private static final String CONTENT_TYPE = "application/json";

	private static final WebClient WEB_CLIENT = WebClient.builder()
		.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
		.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
		.defaultHeader(HttpHeaders.ACCEPT_ENCODING, ACCEPT_ENCODING)
		.defaultHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE)
		.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE)
		.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
		.build();

	@Override
	public Response apply(Request request) {
		JsonNode rootNode = fetchDataFromApi();

		List<SinaNewsService.HotEvent> hotEvents = parseHotEvents(rootNode);

		logger.info("{} hotEvents: {}", this.getClass().getSimpleName(), hotEvents);
		return new Response(hotEvents);
	}

	protected JsonNode fetchDataFromApi() {
		return WEB_CLIENT.get()
			.uri(API_URL)
			.retrieve()
			.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
					clientResponse -> Mono
						.error(new RuntimeException("API call failed with status " + clientResponse.statusCode())))
			.bodyToMono(JsonNode.class)
			.block();
	}

	protected List<HotEvent> parseHotEvents(JsonNode rootNode) {
		if (rootNode == null || !rootNode.has("data")) {
			throw new RuntimeException("Failed to retrieve or parse response data");
		}

		JsonNode hotList = rootNode.get("data").get("hotList");
		List<HotEvent> hotEvents = new ArrayList<>();

		for (JsonNode itemNode : hotList) {
			if (!itemNode.has("info")) {
				continue;
			}
			String title = itemNode.get("info").get("title").asText();
			hotEvents.add(new HotEvent(title));
		}

		return hotEvents;
	}

	public record HotEvent(String title) {
	}

	public record Request() {
	}

	public record Response(List<SinaNewsService.HotEvent> events) {
	}

}
