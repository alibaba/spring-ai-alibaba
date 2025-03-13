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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.function.Function;

/**
 * @author YunLong
 */
public class WeatherSearchService implements Function<WeatherSearchService.Request, WeatherSearchService.Response> {

	private final WeatherTools weatherTools;

	public WeatherSearchService(AmapProperties amapProperties) {
		this.weatherTools = new WeatherTools(amapProperties);
	}

	@Override
	public Response apply(Request request) {

		String responseBody = weatherTools.getAddressCityCode(request.address);

		String adcode = "";

		try {
			JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
			JsonArray geocodesArray = jsonObject.getAsJsonArray("geocodes");
			if (geocodesArray != null && !geocodesArray.isEmpty()) {
				JsonObject firstGeocode = geocodesArray.get(0).getAsJsonObject();
				adcode = firstGeocode.get("adcode").getAsString();
			}
		}
		catch (Exception e) {
			return new Response("Error occurred while processing the request.");
		}

		String weather = weatherTools.getWeather(adcode);

		return new Response(weather);
	}

	@JsonClassDescription("Get the weather conditions for a specified address.")
	public record Request(@JsonProperty(required = true,
			value = "address") @JsonPropertyDescription("The " + "address") String address) {
	}

	public record Response(String message) {
	}

}
