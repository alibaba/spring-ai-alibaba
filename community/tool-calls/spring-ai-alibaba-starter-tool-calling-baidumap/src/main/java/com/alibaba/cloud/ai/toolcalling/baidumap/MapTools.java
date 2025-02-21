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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carbon
 */
public class MapTools {

	private final String baseUrl = "https://api.map.baidu.com";

	private final BaiDuMapProperties baiDuMapProperties;

	private final HttpClient httpClient;

	public MapTools(BaiDuMapProperties baiDuMapProperties) {
		this.baiDuMapProperties = baiDuMapProperties;

		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

		if (Objects.isNull(baiDuMapProperties.getWebApiKey())) {
			throw new RuntimeException("Please configure your BaiDuMap API key in the application.yml file.");
		}
	}

	/**
	 * Geographic/Inverse Geocoding
	 * @param address
	 * @return https://lbs.baidu.com/faq/api?title=webapi/district-search/base
	 */
	public String getAddressCityCode(String address) {

		String path = String.format("/api_region_search/v1/?ak=%s&keyword=%s&sub_admin=0&extensions_code=1",
				baiDuMapProperties.getWebApiKey(), address);

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
	 * @return https://lbs.baidu.com/faq/api?title=webapi/weather/base
	 */
	public String getWeather(String cityCode) {
		String path = String.format("/weather/v1/?ak=%s&district_id=%s&data_type=%s", baiDuMapProperties.getWebApiKey(),
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

	/**
	 * Public Facility Information
	 * @param address
	 * @param facilityType
	 * @return https://lbsyun.baidu.com/faq/api?title=webapi/guide/webservice-placeapi/district
	 */
	public String getFacilityInformation(String address, String facilityType) {
		String path = String.format("/place/v2/search?query=%s&region=%s&output=json&ak=%s", facilityType, address,
				baiDuMapProperties.getWebApiKey());

		HttpRequest httpRequest = createGetRequest(path);

		CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(httpRequest,
				HttpResponse.BodyHandlers.ofString());

		HttpResponse<String> response = responseFuture.join();

		if (response.statusCode() != 200) {
			throw new RuntimeException("Failed to get facility information");
		}

		return response.body();
	}

	private HttpRequest createGetRequest(String path) {
		URI uri = URI.create(baseUrl + path);
		return HttpRequest.newBuilder().uri(uri).GET().build();
	}

}
