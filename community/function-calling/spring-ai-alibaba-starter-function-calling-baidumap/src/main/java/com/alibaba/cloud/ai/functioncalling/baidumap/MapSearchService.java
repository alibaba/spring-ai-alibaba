/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.functioncalling.baidumap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

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
		try {
			JSONObject jsonObject = new JSONObject();

			String addressCityCodeResponse = mapTools.getAddressCityCode(request.address);
			JSONObject cityCodeJson = JSON.parseObject(addressCityCodeResponse);
			JSONArray districtsArray = cityCodeJson.getJSONArray("districts");

			if (districtsArray != null && !districtsArray.isEmpty()) {
				for (int i = 0; i < districtsArray.size(); i++) {
					JSONObject district = districtsArray.getJSONObject(i);
					String adcode = district.getString("adcode"); 

					if (adcode != null && !adcode.isEmpty()) {
						String weather = mapTools.getWeather(adcode);
						jsonObject.put("weather", weather);
					}
				}

				String facilityInformationJson = mapTools.getFacilityInformation(request.address, request.facilityType);
				JSONArray facilityResults = JSON.parseObject(facilityInformationJson).getJSONArray("results");
				if (facilityResults != null) {
					jsonObject.put("facilityInformation", facilityResults.toJSONString());
				}
				else {
					jsonObject.put("facilityInformation", "No facility information found.");
				}
			}
			else {
				return new Response("No districts found in the response.");
			}

			return new Response(jsonObject.toJSONString());
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
