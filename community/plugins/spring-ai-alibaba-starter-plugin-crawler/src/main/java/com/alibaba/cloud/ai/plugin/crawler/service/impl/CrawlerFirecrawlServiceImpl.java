/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.plugin.crawler.service.impl;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.plugin.crawler.CrawlerFirecrawlProperties;
import com.alibaba.cloud.ai.plugin.crawler.constant.CrawlerConstants;
import com.alibaba.cloud.ai.plugin.crawler.exception.CrawlerServiceException;
import com.alibaba.cloud.ai.plugin.crawler.service.AbstractCrawlerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 *
 * https://jina.ai/reader/
 */

public class CrawlerFirecrawlServiceImpl extends AbstractCrawlerService {

	private static final Logger logger = LoggerFactory.getLogger(CrawlerFirecrawlServiceImpl.class);

	private final CrawlerFirecrawlProperties firecrawlProperties;

	private final ObjectMapper objectMapper;

	public CrawlerFirecrawlServiceImpl(
			CrawlerFirecrawlProperties firecrawlProperties,
			ObjectMapper objectMapper
	) {
		this.firecrawlProperties = firecrawlProperties;
		this.objectMapper = objectMapper;
	}

	@Override
	public String run(String targetUrl) {

		if (this.preCheck(targetUrl)) {
			throw new CrawlerServiceException("Target url error, please check the target URL");
		}

		try {
			URL url = URI.create(CrawlerConstants.FIRECRAWL_BASE_URL).toURL();
			logger.info("Firecrawl api request url: {}", url);
			HttpURLConnection connection = this.initHttpURLConnection(url, Map.of());

			connection.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			String requestBody = convertFirecrawlRequestBody(firecrawlProperties);
			wr.writeBytes(requestBody);
			wr.flush();
			wr.close();

			return this.getResponse(connection);
		}
		catch (IOException e) {
			throw new CrawlerServiceException("Firecrawl api request failed: " + e.getMessage());
		}
	}

	private String convertFirecrawlRequestBody(CrawlerFirecrawlProperties firecrawlProperties) {

		Map<String, String> map = new HashMap<>();

		if (firecrawlProperties.getExcludePaths() != null) {
			map.put("excludePaths", firecrawlProperties.getExcludePaths().toString());
		}
		if (firecrawlProperties.getScrapeOptions() != null) {
			map.put("scrapeOptions", firecrawlProperties.getScrapeOptions().toString());
		}
		if (firecrawlProperties.getUrl() != null) {
			map.put("url", firecrawlProperties.getUrl());
		}
		if (firecrawlProperties.getIncludePaths() != null) {
			map.put("includePaths", firecrawlProperties.getIncludePaths().toString());
		}
		if (firecrawlProperties.getMaxDepth() != null) {
			map.put("maxDepth", firecrawlProperties.getMaxDepth().toString());
		}
		if (firecrawlProperties.getIgnoreSitemap() != null) {
			map.put("ignoreSitemap", firecrawlProperties.getIgnoreSitemap().toString());
		}
		if (firecrawlProperties.getLimit() != null) {
			map.put("limit", firecrawlProperties.getLimit().toString());
		}
		if (firecrawlProperties.getWebhook() != null) {
			map.put("webhook", firecrawlProperties.getWebhook());
		}
		if (firecrawlProperties.getAllowExternalLinks() != null) {
			map.put("allowExternalLinks", firecrawlProperties.getAllowExternalLinks().toString());
		}
		if (firecrawlProperties.getAllowBackwardLinks() != null) {
			map.put("allowBackwardLinks", firecrawlProperties.getAllowBackwardLinks().toString());
		}

		return objectMapper.convertValue(map, String.class);
	}
}
