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

	public CrawlerFirecrawlServiceImpl(CrawlerFirecrawlProperties firecrawlProperties, ObjectMapper objectMapper) {
		this.firecrawlProperties = firecrawlProperties;
		this.objectMapper = objectMapper;
	}

	@Override
	public String run(String targetUrl) {

		if (this.preCheck(targetUrl)) {
			throw new CrawlerServiceException("Target url error, please check the target URL");
		}

		try {
			String sourceUrl = CrawlerConstants.FIRECRAWL_BASE_URL + this.getMode();
			URL url = URI.create(sourceUrl).toURL();
			logger.info("Firecrawl api request url: {} target url: {}", url, targetUrl);
			logger.debug("Firecrawl api request token: {}, mode: {}", firecrawlProperties.getToken(),
					firecrawlProperties.getMode());

			Map<String, Object> options = this.getOptions();
			options.put(CrawlerConstants.FirecrawlRequestBodyKey.URL, targetUrl);
			String requestBody = this.objectMapper.writeValueAsString(options);
			logger.debug("Firecrawl api request body: {}", requestBody);

			HttpURLConnection connection = this.initHttpURLConnection(firecrawlProperties.getToken(), url, Map.of(),
					requestBody);

			return this.getResponse(connection);
		}
		catch (IOException e) {
			throw new CrawlerServiceException("Firecrawl api request failed: " + e.getMessage());
		}
	}

	/**
	 * Get the mode of the Firecrawl API.
	 * @return {@link CrawlerConstants.FirecrawlMode}
	 */
	private String getMode() {

		return switch (firecrawlProperties.getMode()) {
			case CrawlerConstants.FirecrawlMode.MAP -> CrawlerConstants.FirecrawlMode.MAP;
			case CrawlerConstants.FirecrawlMode.SCRAPE -> CrawlerConstants.FirecrawlMode.SCRAPE;
			case CrawlerConstants.FirecrawlMode.CRAWL -> throw new CrawlerServiceException(
					"Firecrawl " + CrawlerConstants.FirecrawlMode.CRAWL + " mode not supported yet!");

			default -> throw new CrawlerServiceException("Firecrawl mode not supported yet!");
		};
	}

	/**
	 * Get the options of the Firecrawl API.
	 * @return Request body parameters.
	 */
	private Map<String, Object> getOptions() {

		Map<String, Object> map = new HashMap<>();
		CrawlerFirecrawlProperties runtimeOptions = this.firecrawlProperties;

		if (runtimeOptions.getMobile() != null) {
			map.put(CrawlerConstants.FirecrawlRequestBodyKey.MOBILE, runtimeOptions.getMobile().toString());
		}
		if (runtimeOptions.getRemoveBase64Images() != null) {
			map.put(CrawlerConstants.FirecrawlRequestBodyKey.REMOVE_BASE64_IMAGES,
					runtimeOptions.getRemoveBase64Images().toString());
		}
		if (runtimeOptions.getSkipTlsVerification() != null) {
			map.put(CrawlerConstants.FirecrawlRequestBodyKey.SKIP_TLS_VERIFICATION,
					runtimeOptions.getSkipTlsVerification().toString());
		}
		if (runtimeOptions.getWaitFor() != null) {
			map.put(CrawlerConstants.FirecrawlRequestBodyKey.WAIT_FOR, runtimeOptions.getWaitFor().toString());
		}
		if (runtimeOptions.getFormats() != null) {
			map.put(CrawlerConstants.FirecrawlRequestBodyKey.FORMATS, runtimeOptions.getFormats());
		}
		if (runtimeOptions.getIncludeTags() != null) {
			map.put(CrawlerConstants.FirecrawlRequestBodyKey.INCLUDE_TAGS, runtimeOptions.getIncludeTags());
		}
		if (runtimeOptions.getExcludeTags() != null) {
			map.put(CrawlerConstants.FirecrawlRequestBodyKey.EXCLUDE_TAGS, runtimeOptions.getExcludeTags());
		}
		if (runtimeOptions.getFormats() != null) {
			map.put(CrawlerConstants.FirecrawlRequestBodyKey.FORMATS, runtimeOptions.getFormats());
		}
		if (runtimeOptions.getOnlyMainContent() != null) {
			map.put(CrawlerConstants.FirecrawlRequestBodyKey.ONLY_MAIN_CONTENT,
					runtimeOptions.getOnlyMainContent().toString());
		}

		return map;
	}

}
