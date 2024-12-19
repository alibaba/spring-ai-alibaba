package com.alibaba.cloud.ai.plugin.news;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.function.Function;

/**
 * @Author: XiaoYunTao
 * @Date: 2024/12/19
 */
public abstract class NewsService<T extends NewsService.Request, R extends NewsService.Response>
		implements Function<T, R> {

	private static final Logger logger = LoggerFactory.getLogger(NewsService.class);

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

	public WebClient getWebClient() {
		return WEB_CLIENT;
	}

	@Override
	public R apply(T request) {
		JsonNode rootNode = fetchDataFromApi();

		List<HotEvent> hotEvents = parseHotEvents(rootNode);

		logger.info("{} hotEvents: {}", this.getClass().getSimpleName(), hotEvents);
		return createResponse(hotEvents);
	}

	protected abstract JsonNode fetchDataFromApi();

	protected abstract List<HotEvent> parseHotEvents(JsonNode rootNode);

	protected abstract R createResponse(List<HotEvent> hotEvents);

	public record HotEvent(String title) {
	}

	public record Request() {
	}

	public record Response(List<HotEvent> events) {
	}

}
