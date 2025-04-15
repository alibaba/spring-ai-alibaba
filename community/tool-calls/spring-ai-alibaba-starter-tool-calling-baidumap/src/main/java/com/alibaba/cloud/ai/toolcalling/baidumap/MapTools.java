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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Objects;

/**
 * @author Carbon
 */
public class MapTools {

	private final BaiDuMapProperties baiDuMapProperties;

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public MapTools(BaiDuMapProperties baiDuMapProperties, WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.baiDuMapProperties = baiDuMapProperties;
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;

		if (Objects.isNull(baiDuMapProperties.getApiKey())) {
			throw new RuntimeException("Please configure your BaiDuMap API key in the application.yml file.");
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record District(String code, String name, Integer level) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Region(Integer status, List<District> districts) {
	}

	/**
	 * Geographic/Inverse Geocoding
	 * @param address
	 * @return https://lbs.baidu.com/faq/api?title=webapi/district-search/base
	 */
	public Region getAddressCityCode(String address) {
		String path = "/api_region_search/v1/";
		MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
			.add("ak", baiDuMapProperties.getApiKey())
			.add("keyword", address)
			.add("sub_admin", "0")
			.add("extensions_code", "1")
			.build();
		try {
			String response = webClientTool.get(path, params).block();
			return jsonParseTool.jsonToObject(response, new TypeReference<Region>() {
			});
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get address city code", e);
		}
	}

	/**
	 * Weather Information
	 * @param cityCode
	 * @return https://lbs.baidu.com/faq/api?title=webapi/weather/base
	 */
	public String getWeather(String cityCode) {
		String path = "/weather/v1/";
		MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
			.add("ak", baiDuMapProperties.getApiKey())
			.add("district_id", cityCode)
			.add("data_type", "all")
			.build();

		try {
			return webClientTool.get(path, params).block();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get weather information", e);
		}
	}

	/**
	 * Public Facility Information
	 * @param address
	 * @param facilityType
	 * @return https://lbsyun.baidu.com/faq/api?title=webapi/guide/webservice-placeapi/district
	 */
	public String getFacilityInformation(String address, String facilityType) {
		String path = "/place/v2/search/";
		MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
			.add("ak", baiDuMapProperties.getApiKey())
			.add("query", facilityType)
			.add("region", address)
			.add("output", "json")
			.build();
		try {
			return webClientTool.get(path, params).block();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get facility information", e);
		}
	}

}
