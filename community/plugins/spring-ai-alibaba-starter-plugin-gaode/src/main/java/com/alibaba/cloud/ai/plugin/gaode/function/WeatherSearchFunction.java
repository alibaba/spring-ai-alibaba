package com.alibaba.cloud.ai.plugin.gaode.function;

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
import com.alibaba.cloud.ai.plugin.gaode.GaoDeProperties;
import com.alibaba.cloud.ai.plugin.gaode.service.WebService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

/**
 * @author YunLong
 */
public class WeatherSearchFunction implements Function<WeatherSearchFunction.Request, WeatherSearchFunction.Response> {

	private final WebService webService;

	public WeatherSearchFunction(GaoDeProperties gaoDeProperties) {
		this.webService = new WebService(gaoDeProperties);
	}

	@Override
	public Response apply(Request request) {

		String responseBody = webService.getAddressCityCode(request.address);

		String adcode = "";

		try {
			JSONObject jsonObject = JSON.parseObject(responseBody);
			JSONArray geocodesArray = jsonObject.getJSONArray("geocodes");
			if (geocodesArray != null && !geocodesArray.isEmpty()) {
				JSONObject firstGeocode = geocodesArray.getJSONObject(0);
				adcode = firstGeocode.getString("adcode");
			}
		}
		catch (Exception e) {
			return new Response("Error occurred while processing the request.");
		}

		String weather = webService.getWeather(adcode);

		return new Response(weather);
	}

	@JsonClassDescription("Get the weather conditions for a specified address.")
	public record Request(
			@JsonProperty(required = true, value = "address") @JsonPropertyDescription("The address") String address) {
	}

	public record Response(String message) {
	}

}
