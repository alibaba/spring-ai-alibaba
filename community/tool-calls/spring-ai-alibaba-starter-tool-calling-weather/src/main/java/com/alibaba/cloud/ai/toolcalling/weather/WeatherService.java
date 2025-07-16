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
package com.alibaba.cloud.ai.toolcalling.weather;

import cn.hutool.extra.pinyin.PinyinUtil;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author 31445
 */
public class WeatherService implements Function<WeatherService.Request, WeatherService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public WeatherService(WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	public static Response fromJson(Map<String, Object> json) {
		Map<String, Object> location = (Map<String, Object>) json.get("location");
		Map<String, Object> current = (Map<String, Object>) json.get("current");
		Map<String, Object> forecast = (Map<String, Object>) json.get("forecast");
		List<Map<String, Object>> forecastDays = (List<Map<String, Object>>) forecast.get("forecastday");
		String city = (String) location.get("name");
		return new Response(city, current, forecastDays);
	}

	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.city())) {
			logger.error("Invalid request: city is required.");
			return null;
		}
		String location = preprocessLocation(request.city());
		try {

			String path = "v1/forecast.json";
			MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
				.add("q", location)
				.add("days", String.valueOf(request.days()))
				.build();
			String jsonResponse = webClientTool.get(path, params).block();
			Response response = fromJson(jsonParseTool.jsonToObject(jsonResponse, new TypeReference<>() {
			}));
			logger.info("Weather data fetched successfully for city: {}", response.city());
			return response;
		}
		catch (Exception e) {
			logger.error("Failed to fetch weather data: {}", e.getMessage());
			return null;
		}
	}

	// Use the tools in hutool to convert Chinese place names into pinyin
	private String preprocessLocation(String location) {
		if (containsChinese(location)) {
			return PinyinUtil.getPinyin(location, "");
		}
		return location;
	}

	private boolean containsChinese(String str) {
		return str.matches(".*[\u4e00-\u9fa5].*");
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Weather Service API request")
	public record Request(
			@JsonProperty(required = true, value = "city") @JsonPropertyDescription("THE CITY OF INQUIRY") String city,

			@JsonProperty(required = false,
					value = "days") @JsonPropertyDescription("The number of days for which the weather is forecasted") int days) {
	}

	@JsonClassDescription("Weather Service API response")
	public record Response(String city, Map<String, Object> current, List<Map<String, Object>> forecastDays) {
	}

}
