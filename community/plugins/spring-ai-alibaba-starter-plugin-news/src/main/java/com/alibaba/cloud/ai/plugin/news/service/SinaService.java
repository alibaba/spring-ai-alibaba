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
public class SinaService extends NewsService<SinaService.Request, SinaService.Response> {

	private static final String API_URL = "https://newsapp.sina.cn/api/hotlist?newsId=HB-1-snhs%2Ftop_news_list-all";

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

	@Override
	protected Response createResponse(List<HotEvent> hotEvents) {
		return new Response(hotEvents);
	}

}
