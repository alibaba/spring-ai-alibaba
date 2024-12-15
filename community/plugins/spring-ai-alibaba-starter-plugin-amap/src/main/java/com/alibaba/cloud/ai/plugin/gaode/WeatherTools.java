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

package com.alibaba.cloud.ai.plugin.gaode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author YunLong
 */
public class WeatherTools {

	private final String baseUrl = "https://restapi.amap.com/v3";

	private final GaoDeProperties gaoDeProperties;

	private final HttpClient httpClient;

	public WeatherTools(GaoDeProperties gaoDeProperties) {
		this.gaoDeProperties = gaoDeProperties;

		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

		if (Objects.isNull(gaoDeProperties.getWebApiKey())) {
			throw new RuntimeException("Please configure your GaoDe API key in the application.yml file.");
		}
	}

	/**
	 * Geographic/Inverse Geocoding
	 * @param address
	 * @return https://lbs.amap.com/api/webservice/guide/api/georegeo#s2
	 */
	public String getAddressCityCode(String address) {

		String path = String.format("/geocode/geo?key=%s&address=%s", gaoDeProperties.getWebApiKey(), address);

		HttpRequest httpRequest = createGetRequest(path);

		CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(httpRequest,
				HttpResponse.BodyHandlers.ofString());

		HttpResponse<String> response = responseFuture.join();

		if (response.statusCode() != 200) {
			throw new RuntimeException("Failed to get address city code");
		}

		return response.body();
	}

	/**
	 * Weather Information
	 * @param cityCode
	 * @return https://lbs.amap.com/api/webservice/guide/api/weatherinfo#s0
	 */
	public String getWeather(String cityCode) {
		String path = String.format("/weather/weatherInfo?key=%s&city=%s&extensions=%s", gaoDeProperties.getWebApiKey(),
				cityCode, "all");

		HttpRequest httpRequest = createGetRequest(path);

		CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(httpRequest,
				HttpResponse.BodyHandlers.ofString());

		HttpResponse<String> response = responseFuture.join();

		if (response.statusCode() != 200) {
			throw new RuntimeException("Failed to get weather information");
		}

		return response.body();
	}

	private HttpRequest createGetRequest(String path) {
		URI uri = URI.create(baseUrl + path);

		return HttpRequest.newBuilder().uri(uri).GET().build();
	}

}
