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
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.pinyin4j.PinyinHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author 31445
 */
public class WeatherService implements Function<WeatherService.Request, WeatherService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

	private static final String WEATHER_API_URL = "https://api.weatherapi.com/v1/forecast.json";

	private final WebClient webClient;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final int MEMORY_SIZE = 5;

	private static final int BYTE_SIZE = 1024;

	private static final int MAX_MEMORY_SIZE = MEMORY_SIZE * BYTE_SIZE * BYTE_SIZE;

	public WeatherService(WeatherProperties properties) {
		this.webClient = WebClient.builder()
			.defaultHeader(HttpHeaders.USER_AGENT, HttpHeaders.USER_AGENT)
			.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,ja;q=0.8")
			.defaultHeader("key", properties.getApiKey())
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_MEMORY_SIZE))
			.build();
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
		String url = UriComponentsBuilder.fromHttpUrl(WEATHER_API_URL)
			.queryParam("q", location)
			.queryParam("days", request.days())
			.toUriString();
		try {
			Mono<String> responseMono = webClient.get().uri(url).retrieve().bodyToMono(String.class);
			String jsonResponse = responseMono.block();
			assert jsonResponse != null;

			Response response = fromJson(objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {
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
