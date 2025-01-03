/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.functioncalling.baidumap;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.function.Function;

/**
 * @author Carbon
 */
public class MapSearchService implements Function<MapSearchService.Request, MapSearchService.Response> {

	private final MapTools mapTools;

	private final ObjectMapper objectMapper;

	public MapSearchService(BaiDuMapProperties baiDuMapProperties) {
		this.mapTools = new MapTools(baiDuMapProperties);
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public Response apply(Request request) {
		try {
			var jsonObject = new com.fasterxml.jackson.databind.node.ObjectNode(objectMapper.getNodeFactory());

			String addressCityCodeResponse = mapTools.getAddressCityCode(request.address);
			var cityCodeJson = objectMapper.readTree(addressCityCodeResponse);
			var districtsArray = cityCodeJson.path("districts");

			if (districtsArray.isArray() && districtsArray.size() > 0) {
				for (var district : districtsArray) {
					String adcode = district.path("adcode").asText();

					if (adcode != null && !adcode.isEmpty()) {
						String weather = mapTools.getWeather(adcode);
						jsonObject.put("weather", weather);
					}
				}

				String facilityInformationJson = mapTools.getFacilityInformation(request.address, request.facilityType);
				var facilityResults = objectMapper.readTree(facilityInformationJson).path("results");
				if (facilityResults.isArray()) {
					jsonObject.set("facilityInformation", facilityResults);
				}
				else {
					jsonObject.put("facilityInformation", "No facility information found.");
				}
			}
			else {
				return new Response("No districts found in the response.");
			}

			return new Response(objectMapper.writeValueAsString(jsonObject));
		}
		catch (Exception e) {
			return new Response("Error occurred while processing the request: " + e.getMessage());
		}
	}

	@JsonClassDescription("Get the weather conditions for a specified address and facility type.")
	public record Request(
			@JsonProperty(required = true, value = "address") @JsonPropertyDescription("The address") String address,

			@JsonProperty(required = true,
					value = "facilityType") @JsonPropertyDescription("The type of facility (e.g., bank, airport, restaurant)") String facilityType) {
	}

	public record Response(String message) {
	}

}