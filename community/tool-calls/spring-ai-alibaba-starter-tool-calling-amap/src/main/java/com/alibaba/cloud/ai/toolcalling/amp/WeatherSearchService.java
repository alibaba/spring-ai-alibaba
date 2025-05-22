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
package com.alibaba.cloud.ai.toolcalling.amp;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.function.Function;

/**
 * @author YunLong
 */
public class WeatherSearchService implements Function<WeatherSearchService.Request, WeatherSearchService.Response> {

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final AmapProperties amapProperties;

	public WeatherSearchService(JsonParseTool jsonParseTool, AmapProperties amapProperties,
			WebClientTool webClientTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.amapProperties = amapProperties;
	}

	/**
	 * Geographic/Inverse Geocoding
	 * @param address
	 * @return https://lbs.amap.com/api/webservice/guide/api/georegeo#s2
	 */
	private String getAddressCityCode(String address) {
		try {
			return webClientTool
				.get("/geocode/geo",
						MultiValueMap.fromSingleValue(Map.of("key", amapProperties.getApiKey(), "address", address)))
				.block();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get address city code", e);
		}
	}

	/**
	 * Weather Information
	 * @param cityCode
	 * @return https://lbs.amap.com/api/webservice/guide/api/weatherinfo#s0
	 */
	private String getWeather(String cityCode) {
		try {
			return webClientTool
				.get("/weather/weatherInfo",
						MultiValueMap.fromSingleValue(
								Map.of("key", amapProperties.getApiKey(), "city", cityCode, "extensions", "all")))
				.block();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get weather information", e);
		}

	}

	@Override
	public Response apply(Request request) {
		String responseBody = this.getAddressCityCode(request.address);
		try {
			String arrayString = jsonParseTool.getFieldValueAsString(responseBody, "geocodes");
			String firstElement = jsonParseTool.getFirstElementFromJsonArrayString(arrayString);
			return new Response(this.getWeather(jsonParseTool.getFieldValue(firstElement, String.class, "adcode")));
		}
		catch (Exception e) {
			return new Response("Error occurred while processing the request.");
		}
	}

	@JsonClassDescription("Get the weather conditions for a specified address.")
	public record Request(@JsonProperty(required = true,
			value = "address") @JsonPropertyDescription("The " + "address") String address) {
	}

	public record Response(String message) {
	}

}
