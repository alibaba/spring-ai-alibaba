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
package com.alibaba.cloud.ai.toolcalling.firecrawl;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class FireCrawlService implements Function<FireCrawlService.Request, FireCrawlService.Response> {

	private static final Logger log = LoggerFactory.getLogger(FireCrawlService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final FireCrawlProperties properties;

	public FireCrawlService(WebClientTool webClientTool, JsonParseTool jsonParseTool, FireCrawlProperties properties) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
	}

	@Override
	public Response apply(Request request) {
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new RuntimeException("FireCrawl API Key is Empty");
		}
		if (!CommonToolCallUtils.isValidUrl(request.url)) {
			throw new RuntimeException("Target URL is not valid");
		}
		try {
			return new Response(
					webClientTool.post("/".concat(properties.getMode().toString()), getRequestBody(request.url))
						.block());
		}
		catch (Exception e) {
			log.error("Exception occurred when calling FireCrawl API", e);
			throw new RuntimeException(e);
		}
	}

	private Map<String, Object> getRequestBody(String targetUrl) {
		Map<String, Object> map = new HashMap<>();

		if (properties.getMobile() != null) {
			map.put(FireCrawlConstants.MOBILE, properties.getMobile().toString());
		}
		if (properties.getRemoveBase64Images() != null) {
			map.put(FireCrawlConstants.REMOVE_BASE64_IMAGES, properties.getRemoveBase64Images().toString());
		}
		if (properties.getSkipTlsVerification() != null) {
			map.put(FireCrawlConstants.SKIP_TLS_VERIFICATION, properties.getSkipTlsVerification().toString());
		}
		if (properties.getWaitFor() != null) {
			map.put(FireCrawlConstants.WAIT_FOR, properties.getWaitFor().toString());
		}
		if (properties.getFormats() != null) {
			map.put(FireCrawlConstants.FORMATS,
					Arrays.stream(properties.getFormats()).map(FireCrawlFormatsEnum::toString).toList());
		}
		if (properties.getIncludeTags() != null) {
			map.put(FireCrawlConstants.INCLUDE_TAGS, List.of(properties.getIncludeTags()));
		}
		if (properties.getExcludeTags() != null) {
			map.put(FireCrawlConstants.EXCLUDE_TAGS, List.of(properties.getExcludeTags()));
		}
		if (properties.getFormats() != null) {
			map.put(FireCrawlConstants.FORMATS,
					Arrays.stream(properties.getFormats()).map(FireCrawlFormatsEnum::toString).toList());
		}
		if (properties.getOnlyMainContent() != null) {
			map.put(FireCrawlConstants.ONLY_MAIN_CONTENT, properties.getOnlyMainContent().toString());
		}
		map.put(FireCrawlConstants.URL, targetUrl);
		return map;
	}

	@JsonClassDescription("Firecrawl request body")
	public record Request(@JsonPropertyDescription("url") String url) {
	}

	public record Response(String content) {
	}

}
