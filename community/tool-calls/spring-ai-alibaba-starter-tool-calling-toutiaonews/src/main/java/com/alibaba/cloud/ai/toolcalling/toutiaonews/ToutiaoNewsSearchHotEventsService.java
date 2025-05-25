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
package com.alibaba.cloud.ai.toolcalling.toutiaonews;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author XiaoYunTao
 * @since 2024/12/18
 */
public class ToutiaoNewsSearchHotEventsService
		implements Function<ToutiaoNewsSearchHotEventsService.Request, ToutiaoNewsSearchHotEventsService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(ToutiaoNewsSearchHotEventsService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final ToutiaoNewsProperties properties;

	public ToutiaoNewsSearchHotEventsService(JsonParseTool jsonParseTool, ToutiaoNewsProperties properties,
			WebClientTool webClientTool) {
		this.webClientTool = webClientTool;
		this.properties = properties;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public ToutiaoNewsSearchHotEventsService.Response apply(ToutiaoNewsSearchHotEventsService.Request request) {
		JsonNode rootNode = fetchDataFromApi();

		List<HotEvent> hotEvents = parseHotEvents(rootNode);

		logger.info("{} hotEvents: {}", this.getClass().getSimpleName(), hotEvents);
		return new Response(hotEvents);
	}

	protected JsonNode fetchDataFromApi() {
		try {
			String json = webClientTool.get("").block();

			return jsonParseTool.jsonToObject(json, JsonNode.class);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to fetch or parse Toutiao API data", e);
		}
	}

	protected List<HotEvent> parseHotEvents(JsonNode rootNode) {
		if (rootNode == null || !rootNode.has("data")) {
			throw new RuntimeException("Failed to retrieve or parse response data");
		}

		JsonNode dataNode = rootNode.get("data");
		List<HotEvent> hotEvents = new ArrayList<>();

		for (JsonNode itemNode : dataNode) {
			if (!itemNode.has("Title")) {
				continue;
			}
			String title = itemNode.get("Title").asText();
			hotEvents.add(new HotEvent(title));
		}

		return hotEvents;
	}

	public record HotEvent(String title) {
	}

	public record Request() {
	}

	public record Response(List<HotEvent> events) {
	}

}
