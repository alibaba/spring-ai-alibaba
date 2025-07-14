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
package com.alibaba.cloud.ai.toolcalling.tencentmap;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.function.Function;

/**
 * @author HunterPorter
 */
public class TencentMapWeatherService
		implements Function<TencentMapWeatherService.Request, TencentMapWeatherService.Response> {

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final TencentMapProperties tencentMapProperties;

	public TencentMapWeatherService(WebClientTool webClientTool, JsonParseTool jsonParseTool,
			TencentMapProperties tencentMapProperties) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.tencentMapProperties = tencentMapProperties;
	}

	/**
	 * 地址解析（地址转坐标）
	 * @param address 地址
	 * @return https://lbs.qq.com/service/webService/webServiceGuide/address/Geocoder
	 */
	private String getAddressInfo(String address) {
		try {
			return webClientTool
				.get("geocoder/v1/",
						MultiValueMap
							.fromSingleValue(Map.of("key", tencentMapProperties.getApiKey(), "address", address)))
				.block();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get address adcode", e);
		}
	}

	/**
	 * 天气查询
	 * @param adcode 行政区划代码
	 * @param type 查询天气类型，取值：now[默认] 实时天气预报; future 未来天气预报（默认获取当天和未来3天的天气信息）
	 * @return https://lbs.qq.com/service/webService/webServiceGuide/weatherinfo
	 */
	private String getWeather(String adcode, String type) {
		try {
			return webClientTool
				.get("weather/v1/",
						MultiValueMap.fromSingleValue(
								Map.of("key", tencentMapProperties.getApiKey(), "adcode", adcode, "type", type)))
				.block();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get weather information", e);
		}

	}

	@Override
	public Response apply(Request request) {
		String responseBody = this.getAddressInfo(request.address);
		try {
			String adInfo = jsonParseTool.getDepthFieldValueAsString(responseBody, "result", "ad_info");

			String weatherResponse = this.getWeather(jsonParseTool.getFieldValueAsText(adInfo, "adcode"),
					request.getTypeOrDefault());

			String weatherResult = jsonParseTool.getFieldValueAsString(weatherResponse, "result");
			String weatherInfo = request.getTypeOrDefault().equals("now")
					? jsonParseTool.getFieldValueAsString(weatherResult, "realtime")
					: jsonParseTool.getDepthFieldValueAsString(weatherResult, "forecast");
			String weather = jsonParseTool
				.getFieldValueAsString(jsonParseTool.getFirstElementFromJsonArrayString(weatherInfo), "infos");
			return new Response(weather);
		}
		catch (Exception e) {
			return new Response("Error occurred while processing the request.");
		}
	}

	@JsonClassDescription("根据指定地址获取天气情况")
	public record Request(
			@JsonProperty(required = true, value = "address") @JsonPropertyDescription("地址") String address,
			@JsonProperty(required = false,
					value = "type") @JsonPropertyDescription("查询天气类型，取值：now[默认] 实时天气预报; future 未来天气预报（默认获取当天和未来3天的天气信息）") String type) {
		public String getTypeOrDefault() {
			return type == null || type.isEmpty() ? "now" : type;
		}
	}

	public record Response(String message) {
	}

}
