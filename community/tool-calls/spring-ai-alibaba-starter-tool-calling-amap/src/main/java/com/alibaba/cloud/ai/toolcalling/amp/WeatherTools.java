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

import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import java.util.Objects;

/**
 * @author YunLong
 */
public class WeatherTools {

	private final WebClientTool webClientTool;

	private final AmapProperties amapProperties;

	;

	public WeatherTools(AmapProperties amapProperties, WebClientTool webClientTool) {
		this.amapProperties = amapProperties;
		this.webClientTool = webClientTool;

		if (Objects.isNull(amapProperties.getWebApiKey())) {
			throw new RuntimeException("Please configure your GaoDe API key in the application.yml file.");
		}
	}

	/**
	 * Geographic/Inverse Geocoding
	 * @param address
	 * @return https://lbs.amap.com/api/webservice/guide/api/georegeo#s2
	 */
	public String getAddressCityCode(String address) {

		String path = String.format("/geocode/geo?key=%s&address=%s", amapProperties.getWebApiKey(), address);

		String uri = amapProperties.getBaseUrl() + path;
		try {
			String json = webClientTool.get(uri).block();

			return json;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get address city code", e);
		}
	}

	/**
	 * Weather Information
	 * @param cityCode
	 * @return https://lbs.amap.com/api/webservice/guide/api/weatherinfo#s0
	 */
	public String getWeather(String cityCode) {
		String path = String.format("/weather/weatherInfo?key=%s&city=%s&extensions=%s", amapProperties.getWebApiKey(),
				cityCode, "all");

		String uri = amapProperties.getBaseUrl() + path;
		try {
			String json = webClientTool.get(uri).block();

			return json;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get weather information", e);
		}

	}

}
