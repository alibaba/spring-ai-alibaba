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
package com.alibaba.cloud.ai.toolcalling.crawler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 *
 * Reference: https://jina.ai/reader/
 */

public class CrawlerJinaServiceImpl extends AbstractCrawlerService {

	private static final Logger logger = LoggerFactory.getLogger(CrawlerJinaServiceImpl.class);

	private final CrawlerJinaProperties jinaProperties;

	private final ObjectMapper objectMapper;

	public CrawlerJinaServiceImpl(CrawlerJinaProperties jinaProperties, ObjectMapper objectMapper) {
		this.jinaProperties = jinaProperties;
		this.objectMapper = objectMapper;
	}

	@Override
	public String run(String targetUrl) {

		if (this.preCheck(targetUrl)) {
			throw new CrawlerServiceException("Target url error, please check the target URL");
		}

		try {
			URL url = URI.create(CrawlerConstants.JINA_BASE_URL).toURL();
			logger.info("Jina api request url: {}", targetUrl);
			logger.debug("Jina Reader api token: {}", jinaProperties.getToken());

			Map<String, String> requestParam = Map.of("url", targetUrl);
			String requestBody = this.objectMapper.writeValueAsString(requestParam);
			logger.debug("Jina request body: {}", requestBody);

			HttpURLConnection connection = this.initHttpURLConnection(jinaProperties.getToken(), url, this.getOptions(),
					requestBody);

			return objectMapper.writeValueAsString(this.convert2Response(this.getResponse(connection)));
		}
		catch (IOException e) {
			throw new CrawlerServiceException("Jina reader request failed: " + e.getMessage());
		}
	}

	/**
	 * Get request options.
	 */
	private Map<String, String> getOptions() {

		Map<String, String> map = new HashMap<>();

		if (Objects.nonNull(jinaProperties.getLocale())) {
			map.put(CrawlerConstants.JinaHeaders.X_LOCALE, jinaProperties.getLocale());
		}
		if (Objects.nonNull(jinaProperties.getNoCache())) {
			map.put(CrawlerConstants.JinaHeaders.X_NO_CACHE, jinaProperties.getNoCache().toString());
		}
		if (Objects.nonNull(jinaProperties.getProxyUrl())) {
			map.put(CrawlerConstants.JinaHeaders.X_PROXY_URL, jinaProperties.getProxyUrl());
		}
		if (Objects.nonNull(jinaProperties.getRemoveSelector())) {
			map.put(CrawlerConstants.JinaHeaders.X_REMOVE_SELECTOR, jinaProperties.getRemoveSelector());
		}
		if (Objects.nonNull(jinaProperties.getRetainImages())) {
			map.put(CrawlerConstants.JinaHeaders.X_RETAIN_IMAGES, jinaProperties.getRetainImages());
		}
		if (Objects.nonNull(jinaProperties.getSetCookie())) {
			map.put(CrawlerConstants.JinaHeaders.X_SET_COOKIE, jinaProperties.getSetCookie());
		}
		if (Objects.nonNull(jinaProperties.getWithGeneratedAlt())) {
			map.put(CrawlerConstants.JinaHeaders.X_WITH_GENERATED_ALT, jinaProperties.getWithGeneratedAlt().toString());
		}
		if (Objects.nonNull(jinaProperties.getWithIframe())) {
			map.put(CrawlerConstants.JinaHeaders.X_WITH_IFRAME, jinaProperties.getWithIframe().toString());
		}
		if (Objects.nonNull(jinaProperties.getWithShadowDom())) {
			map.put(CrawlerConstants.JinaHeaders.X_WITH_SHADOW_DOM, jinaProperties.getWithShadowDom().toString());
		}
		if (Objects.nonNull(jinaProperties.getWithImagesSummary())) {
			map.put(CrawlerConstants.JinaHeaders.X_WITH_IMAGES_SUMMARY,
					jinaProperties.getWithImagesSummary().toString());
		}
		if (Objects.nonNull(jinaProperties.getWithLinksSummary())) {
			map.put(CrawlerConstants.JinaHeaders.X_WITH_LINKS_SUMMARY, jinaProperties.getWithLinksSummary().toString());
		}
		if (Objects.nonNull(jinaProperties.getTargetSelector())) {
			map.put(CrawlerConstants.JinaHeaders.X_TARGET_SELECTOR, jinaProperties.getTargetSelector());
		}
		if (Objects.nonNull(jinaProperties.getWaitForSelector())) {
			map.put(CrawlerConstants.JinaHeaders.X_WAIT_FOR_SELECTOR, jinaProperties.getWaitForSelector());
		}

		return map;
	}

	private JinaResponse convert2Response(String respStr) {

		JinaResponse response;

		try {
			JsonNode rootNode = objectMapper.readTree(respStr);
			JsonNode dataNode = rootNode.get("data");

			response = objectMapper.treeToValue(dataNode, JinaResponse.class);
		}
		catch (IOException e) {
			throw new CrawlerServiceException("Parse json data failed: " + e.getMessage());
		}

		return response;
	}

}
