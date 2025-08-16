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
package com.alibaba.cloud.ai.toolcalling.worldbankdata;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * World Bank Data Service for accessing development indicators and country data. Provides
 * access to World Bank's development data through their open API. Documentation:
 * https://datahelpdesk.worldbank.org/knowledgebase/topics/125589
 *
 * @author Makoto
 */
public class WorldBankDataService
		implements SearchService, Function<WorldBankDataService.Request, WorldBankDataService.Response> {

	private static final Logger log = LoggerFactory.getLogger(WorldBankDataService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final WorldBankDataProperties properties;

	public WorldBankDataService(WebClientTool webClientTool, JsonParseTool jsonParseTool,
			WorldBankDataProperties properties) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
	}

	@Override
	public SearchService.Response query(String query) {
		return this.apply(Request.simpleQuery(query));
	}

	@Override
	public Response apply(Request request) {
		try {
			String endpoint = buildEndpoint(request);
			MultiValueMap<String, String> params = buildQueryParams(request);

			log.debug("World Bank API endpoint: {}, params: {}", endpoint, params);

			String responseStr = webClientTool.get(endpoint, params).block();
			log.debug("World Bank API response: {}", responseStr);

			// Parse the response - World Bank API returns array format [metadata, data]
			List<Object> rawResponse = jsonParseTool.jsonToObject(responseStr, new TypeReference<List<Object>>() {
			});

			if (rawResponse.size() >= 2) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) rawResponse.get(1);
				return parseResponse(dataList, request);
			}
			else {
				return new Response(new ArrayList<>());
			}
		}
		catch (Exception e) {
			log.error("World Bank Data Service request error: ", e);
			throw new RuntimeException("Failed to fetch World Bank data: " + e.getMessage(), e);
		}
	}

	private String buildEndpoint(Request request) {
		StringBuilder endpoint = new StringBuilder();

		if (StringUtils.hasText(request.country()) && StringUtils.hasText(request.indicator())) {
			// Country-specific indicator data
			endpoint.append("/country/").append(request.country()).append("/indicator/").append(request.indicator());
		}
		else if (StringUtils.hasText(request.indicator())) {
			// Indicator metadata or all countries data for indicator
			if ("metadata".equals(request.queryType())) {
				endpoint.append("/indicator/").append(request.indicator());
			}
			else {
				endpoint.append("/country/all/indicator/").append(request.indicator());
			}
		}
		else if (StringUtils.hasText(request.country())) {
			// Country information
			endpoint.append("/country/").append(request.country());
		}
		else {
			// Search indicators by name/description
			endpoint.append("/indicator");
		}

		return endpoint.toString();
	}

	private MultiValueMap<String, String> buildQueryParams(Request request) {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

		params.add("format", properties.getFormat());
		params.add("per_page", String.valueOf(request.perPage() != null ? request.perPage() : properties.getPerPage()));

		if (StringUtils.hasText(request.dateRange())) {
			params.add("date", request.dateRange());
		}

		if (request.page() != null && request.page() > 0) {
			params.add("page", String.valueOf(request.page()));
		}

		if (request.mrv() != null && request.mrv() > 0) {
			params.add("mrv", String.valueOf(request.mrv()));
		}

		return params;
	}

	private Response parseResponse(List<Map<String, Object>> dataList, Request request) {
		List<DataResult> results = new ArrayList<>();

		for (Map<String, Object> item : dataList) {
			if (item == null)
				continue;

			String title = extractTitle(item, request);
			String description = extractDescription(item, request);
			String url = buildDataUrl(item, request);

			results.add(new DataResult(title, description, url, item.get("value")));
		}

		return new Response(results);
	}

	private String extractTitle(Map<String, Object> item, Request request) {
		if (StringUtils.hasText(request.indicator())) {
			// For indicator data, show country and indicator
			Object countryObj = item.get("country");
			if (countryObj instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> country = (Map<String, Object>) countryObj;
				String countryName = (String) country.get("value");
				Object indicatorObj = item.get("indicator");
				if (indicatorObj instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> indicator = (Map<String, Object>) indicatorObj;
					String indicatorName = (String) indicator.get("value");
					return countryName + " - " + indicatorName;
				}
				return countryName;
			}
		}

		// Fallback to name or value
		return item.getOrDefault("name", item.getOrDefault("value", "")).toString();
	}

	private String extractDescription(Map<String, Object> item, Request request) {
		StringBuilder desc = new StringBuilder();

		Object year = item.get("date");
		Object value = item.get("value");

		if (year != null) {
			desc.append("年份: ").append(year);
		}

		if (value != null) {
			if (desc.length() > 0)
				desc.append(", ");
			desc.append("数值: ").append(value);
		}

		// Add source note if available
		Object sourceNote = item.get("sourceNote");
		if (sourceNote != null) {
			if (desc.length() > 0)
				desc.append(". ");
			desc.append(sourceNote.toString());
		}

		return desc.toString();
	}

	private String buildDataUrl(Map<String, Object> item, Request request) {
		// Return URL to World Bank data page
		return "https://data.worldbank.org/";
	}

	@JsonClassDescription("World Bank Data Request for querying development indicators and country data.")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Request(@JsonProperty(required = true,
			value = "query") @JsonPropertyDescription("Search query - can be indicator code (e.g., 'SP.POP.TOTL'), country code (e.g., 'CHN'), or search terms") String query,

			@JsonProperty(
					value = "country") @JsonPropertyDescription("Country code (e.g., 'CHN' for China, 'USA' for United States, 'all' for all countries)") String country,

			@JsonProperty(
					value = "indicator") @JsonPropertyDescription("Indicator code (e.g., 'SP.POP.TOTL' for Population, 'NY.GDP.MKTP.CD' for GDP)") String indicator,

			@JsonProperty(
					value = "dateRange") @JsonPropertyDescription("Date range for data (e.g., '2020', '2018:2022', 'YTD:2023')") String dateRange,

			@JsonProperty(value = "queryType",
					defaultValue = "data") @JsonPropertyDescription("Type of query: 'data' for indicator data, 'metadata' for indicator information, 'country' for country info") String queryType,

			@JsonProperty(value = "page",
					defaultValue = "1") @JsonPropertyDescription("Page number for pagination") Integer page,

			@JsonProperty(value = "perPage",
					defaultValue = "10") @JsonPropertyDescription("Number of results per page (max 100)") Integer perPage,

			@JsonProperty(
					value = "mrv") @JsonPropertyDescription("Most recent values count (e.g., 5 for last 5 available data points)") Integer mrv)
			implements
				SearchService.Request {

		@Override
		public String getQuery() {
			return this.query();
		}

		public static Request simpleQuery(String query) {
			// Try to detect if query looks like indicator code or country code
			if (query.contains(".") && query.length() > 5) {
				// Looks like indicator code (e.g., SP.POP.TOTL)
				return new Request(query, "all", query, null, "data", 1, 10, null);
			}
			else if (query.length() == 3 && query.toUpperCase().equals(query)) {
				// Looks like country code (e.g., CHN, USA)
				return new Request(query, query, null, null, "country", 1, 10, null);
			}
			else {
				// General search
				return new Request(query, null, null, null, "data", 1, 10, null);
			}
		}
	}

	@JsonClassDescription("World Bank Data Response")
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(@JsonProperty("results") List<DataResult> results) implements SearchService.Response {

		@Override
		public SearchService.SearchResult getSearchResult() {
			return new SearchService.SearchResult(this.results()
				.stream()
				.map(item -> new SearchService.SearchContent(item.title(), item.description(), item.url(), null))
				.toList());
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record DataResult(String title, String description, String url, Object value) {
	}

}
