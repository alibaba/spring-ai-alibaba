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
package com.alibaba.cloud.ai.example.deepresearch.service;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.jinacrawler.JinaCrawlerService;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sort the SearchService's returned results based on user-configured website blocklists
 * and allowlists.
 *
 * @author vlsmb
 * @since 2025/7/10
 */
public class SearchInfoService {

	private static final Logger logger = LoggerFactory.getLogger(SearchInfoService.class);

	private final Integer MAX_RETRY_COUNT = 3;

	private final Long RETRY_DELAY_MS = 500L;

	private final JinaCrawlerService jinaCrawlerService;

	private final SearchFilterService searchFilterService;

	public SearchInfoService(JinaCrawlerService jinaCrawlerService, SearchFilterService searchFilterService) {
		this.jinaCrawlerService = jinaCrawlerService;
		this.searchFilterService = searchFilterService;
	}

	public List<Map<String, String>> searchInfo(boolean enableSearchFilter, SearchEnum searchEnum, String query)
			throws InterruptedException {

		List<Map<String, String>> results = new ArrayList<>();

		// Retry logic
		for (int i = 0; i < MAX_RETRY_COUNT; i++) {
			try {
				results = searchFilterService.queryAndFilter(enableSearchFilter, searchEnum, query)
					.stream()
					.map(info -> {
						Map<String, String> result = new HashMap<>();
						result.put("title", info.content().title());
						result.put("weight", String.valueOf(info.weight()));
						// try to obtain url
						boolean isUrl = CommonToolCallUtils.isValidUrl(info.content().url());
						String url = null;
						if (isUrl) {
							url = info.content().url();
						}
						result.put("url", url);
						// try to obtain icon
						String icon = info.content().icon();
						if (icon == null || icon.isEmpty()) {
							icon = getIcon(url);
						}
						result.put("icon", icon);

						if (jinaCrawlerService == null || !isUrl) {
							result.put("content", info.content().content());
						}
						else {
							try {
								logger.info("Get detail info of a url using Jina Crawler...");
								result.put("content",
										jinaCrawlerService.apply(new JinaCrawlerService.Request(info.content().url()))
											.content());
							}
							catch (Exception e) {
								logger.error("Jina Crawler Service Error", e);
								result.put("content", info.content().content());
							}
						}
						return result;
					})
					.collect(Collectors.toList());
				break;
			}
			catch (Exception e) {
				logger.warn("搜索尝试 {} 失败: {}", i + 1, e.getMessage());
				Thread.sleep(RETRY_DELAY_MS);
			}
		}
		return results;
	}

	public String getIcon(String url) {
		try {
			URL urlObj = new URL(url);
			String protocol = urlObj.getProtocol();
			String host = urlObj.getHost();
			int port = urlObj.getPort();
			StringBuilder root = new StringBuilder();
			root.append(protocol).append("://").append(host);
			if (port != -1) {
				root.append(":").append(port);
			}
			root.append("/favicon.ico");
			return root.toString();
		}
		catch (MalformedURLException e) {
			logger.error("Invalid URL: {}", url, e);
			return null;
		}
	}

}
