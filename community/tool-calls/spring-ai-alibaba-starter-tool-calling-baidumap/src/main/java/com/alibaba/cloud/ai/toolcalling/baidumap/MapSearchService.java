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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.gson.*;

import java.util.function.Function;

/**
 * @author Carbon
 */
public class MapSearchService implements Function<MapSearchService.Request, MapSearchService.Response> {

	private final MapTools mapTools;

	public MapSearchService(BaiDuMapProperties baiDuMapProperties) {
		this.mapTools = new MapTools(baiDuMapProperties);
	}

	@Override
	public Response apply(Request request) {
		Gson gson = new Gson();
		try {
			JsonObject jsonObject = new JsonObject();

			// 获取并解析城市代码信息
			String addressCityCodeResponse = mapTools.getAddressCityCode(request.address);
			JsonObject cityCodeJson = JsonParser.parseString(addressCityCodeResponse).getAsJsonObject();
			JsonElement districtsElement = cityCodeJson.get("districts");

			// 检查districts是否存在且为有效数组
			if (districtsElement != null && districtsElement.isJsonArray()) {
				JsonArray districtsArray = districtsElement.getAsJsonArray();

				if (districtsArray.isEmpty()) {
					return new Response("No districts found in the response.");
				}

				// 遍历处理每个行政区划
				for (JsonElement districtElement : districtsArray) {
					JsonObject district = districtElement.getAsJsonObject();
					JsonElement adcodeElement = district.get("adcode");

					// 处理有效的adcode
					if (adcodeElement != null && !adcodeElement.isJsonNull()) {
						String adcode = adcodeElement.getAsString();
						if (!adcode.isEmpty()) {
							String weather = mapTools.getWeather(adcode);
							jsonObject.addProperty("weather", weather);
						}
					}
				}

				// 获取并处理设施信息
				String facilityJsonStr = mapTools.getFacilityInformation(request.address, request.facilityType);
				JsonElement facilityElement = JsonParser.parseString(facilityJsonStr);
				JsonElement resultsElement = facilityElement.getAsJsonObject().get("results");

				if (resultsElement != null && resultsElement.isJsonArray()) {
					jsonObject.add("facilityInformation", resultsElement.getAsJsonArray());
				}
				else {
					jsonObject.addProperty("facilityInformation", "No facility information found.");
				}

				return new Response(gson.toJson(jsonObject));
			}
			else {
				return new Response("No districts found in the response.");
			}
		}
		catch (JsonSyntaxException e) {
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