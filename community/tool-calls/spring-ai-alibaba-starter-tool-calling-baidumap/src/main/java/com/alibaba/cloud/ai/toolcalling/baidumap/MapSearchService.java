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
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.micrometer.common.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author Carbon
 * @author vlsmb
 */
public class MapSearchService implements Function<MapSearchService.Request, MapSearchService.Response> {

	private final MapTools mapTools;

	private final JsonParseTool jsonParseTool;

	public MapSearchService(BaiDuMapProperties baiDuMapProperties, WebClientTool webClientTool,
			JsonParseTool jsonParseTool) {
		this.mapTools = new MapTools(baiDuMapProperties, webClientTool, jsonParseTool);
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public Response apply(Request request) {
		try {
			// Retrieves and parses city code information
			MapTools.Region region = mapTools.getAddressCityCode(request.address);

			// Verifies if 'districts' exists and is a valid non-empty array
			if (region != null && region.districts() != null) {
				if (region.districts().isEmpty()) {
					return new Response("No districts found in the response.");
				}

				// Iterate through each district
				List<String> weathers = region.districts()
					.stream()
					// Process valid 'adcode' (Note: renamed to 'code' in current API
					// version)
					.map(MapTools.District::code)
					.filter(Objects::nonNull)
					.filter(StringUtils::isNotBlank)
					.map(mapTools::getWeather)
					.toList();
				String jsonObjectStr = jsonParseTool.setFieldJsonObjectAsString("{}", "weather", weathers);

				// Fetch and process facility information
				String facilityJsonStr = mapTools.getFacilityInformation(request.address, request.facilityType);
				String resultsJsonStr = jsonParseTool.getFieldValueAsString(facilityJsonStr, "results");
				if (StringUtils.isNotBlank(resultsJsonStr)) {
					jsonObjectStr = jsonParseTool.setFieldJsonObjectAsString(jsonObjectStr, "facilityInformation",
							resultsJsonStr);
				}
				else {
					jsonObjectStr = jsonParseTool.setFieldValue(jsonObjectStr, "facilityInformation",
							"No facility information found.");
				}
				return new Response(jsonObjectStr);
			}
			else {
				return new Response("No districts found in the response.");
			}
		}
		catch (JsonProcessingException e) {
			return new Response("Invalid JSON format: " + e.getMessage());
		}
		catch (Exception e) {
			return new Response("Error occurred while processing the request: " + e.getMessage());
		}
	}

	@JsonClassDescription("Get the weather conditions for a specified address and facility type.")
	public record Request(
			@JsonProperty(required = true,
					value = "address") @JsonPropertyDescription("The " + "address") String address,

			@JsonProperty(required = true, value = "facilityType") @JsonPropertyDescription("The "
					+ "type of facility (e.g., bank, airport, restaurant)") String facilityType) {
	}

	public record Response(String message) {
	}

}