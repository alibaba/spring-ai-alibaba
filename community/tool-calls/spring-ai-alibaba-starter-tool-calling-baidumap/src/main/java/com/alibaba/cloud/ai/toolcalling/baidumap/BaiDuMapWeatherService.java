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
package com.alibaba.cloud.ai.toolcalling.baidumap;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Carbon
 * @author vlsmb
 */
public class BaiDuMapWeatherService
		implements Function<BaiDuMapWeatherService.Request, BaiDuMapWeatherService.Response> {

	private final BaiDuMapTools baiDuMapTools;

	private final JsonParseTool jsonParseTool;

	public BaiDuMapWeatherService(JsonParseTool jsonParseTool, BaiDuMapTools baiDuMapTools) {
		this.baiDuMapTools = baiDuMapTools;
		this.jsonParseTool = jsonParseTool;
	}

	// Query regionCodes by province → city → district
	private String getUnitRegionCode(AddressResult addressResult) {
		if (!StringUtils.hasText(addressResult.province())) {
			return null;
		}

		BaiDuMapTools.Region region = baiDuMapTools.getRegionInformation(addressResult.province(), 3);
		BaiDuMapTools.District province = Stream.of(region)
			.map(BaiDuMapTools.Region::districts)
			.flatMap(Collection::stream)
			.findFirst()
			.orElse(null);
		if (province == null) {
			return null;
		}
		String regionCode = province.code();
		// Get city Info (if have)
		BaiDuMapTools.District city = province.districts()
			.stream()
			.filter(d -> d.name().equals(addressResult.city()))
			.findFirst()
			.orElse(null);
		if (city != null && StringUtils.hasText(city.code())) {
			regionCode = city.code();
			BaiDuMapTools.District district = city.districts()
				.stream()
				.filter(d -> d.name().equals(addressResult.area()))
				.findFirst()
				.orElse(null);
			if (district != null && StringUtils.hasText(district.code())) {
				regionCode = district.code();
			}
		}
		return regionCode;
	}

	@Override
	public Response apply(Request request) {
		try {
			String addressInfoStr = baiDuMapTools.getAddressInformation(null, request.address(), false);
			AddressInfo addressInfo = jsonParseTool.jsonToObject(addressInfoStr, new TypeReference<AddressInfo>() {
			});
			if (addressInfo.status() != 0) {
				return new Response("Get AddressWeatherInfo failed, message: " + addressInfo.message());
			}
			if (addressInfo.results() == null || addressInfo.results().isEmpty()) {
				return new Response("Get AddressWeatherInfo failed, message: Address Not Found");
			}

			// Use the first query result as the basis to fetch weather data for its
			// corresponding administrative region.
			AddressResult addressResult = addressInfo.results().get(0);

			// When the user provides a specific venue address, the API returns resultType
			// as 'poi_type' per documentation; otherwise (indicating an administrative
			// region query), directly call the method to obtain regionCode.
			String regionCode = addressInfo.resultType().equals("poi_type") ? this.getUnitRegionCode(addressResult)
					: baiDuMapTools.getRegionInformation(addressResult.name(), 0)
						.districts()
						.stream()
						.map(BaiDuMapTools.District::code)
						.findFirst()
						.orElse(null);

			if (!StringUtils.hasText(regionCode)) {
				return new Response("Get AddressWeatherInfo failed, message: RegionCode Not Found");
			}
			// Get Weather Info
			return new Response(baiDuMapTools.getWeather(regionCode));
		}
		catch (JsonProcessingException e) {
			return new Response("Invalid JSON format: " + e.getMessage());
		}
		catch (Exception e) {
			return new Response("Error occurred while processing the request: " + e.getMessage());
		}
	}

	@JsonClassDescription("Get the weather conditions for a specified address.")
	public record Request(@JsonProperty(required = true,
			value = "address") @JsonPropertyDescription("User-requested specific location address") String address) {
	}

	public record Response(String message) {
	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record AddressResult(String name, String address, String province, String city, String area) {

	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record AddressInfo(Integer status, String message, @JsonProperty(value = "result_type") String resultType,
			List<AddressResult> results) {

	}

}
