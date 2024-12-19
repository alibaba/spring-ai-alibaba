package com.alibaba.cloud.ai.plugin.news.service;

import com.alibaba.cloud.ai.plugin.news.NewsService;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: XiaoYunTao
 * @Date: 2024/12/18
 */
public class ToutiaoService extends NewsService<ToutiaoService.Request, ToutiaoService.Response> {

	private static final String API_URL = "https://www.toutiao.com/hot-event/hot-board/?origin=toutiao_pc";

	@Override
	protected JsonNode fetchDataFromApi() {
		return getWebClient().get()
			.uri(API_URL)
			.retrieve()
			.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
					clientResponse -> Mono
						.error(new RuntimeException("API call failed with status " + clientResponse.statusCode())))
			.bodyToMono(JsonNode.class)
			.block();
	}

	@Override
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

	@Override
	protected NewsService.Response createResponse(List<HotEvent> hotEvents) {
		return new Response(hotEvents);
	}

}
