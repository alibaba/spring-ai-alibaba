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
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author Carbon
 * @author vlsmb
 */
public final class BaiDuMapTools {

	private final BaiDuMapProperties baiDuMapProperties;

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public BaiDuMapTools(BaiDuMapProperties baiDuMapProperties, WebClientTool webClientTool,
			JsonParseTool jsonParseTool) {
		this.baiDuMapProperties = baiDuMapProperties;
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record District(String code, String name, Integer level, List<District> districts) {
	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Region(Integer status, List<District> districts) {
	}

	/**
	 * Query administrative division information This method can be used to obtain
	 * administrative division codes
	 * @param regionName the name of a city/province/district
	 * @param depth depth of child information
	 * @return https://lbs.baidu.com/faq/api?title=webapi/district-search/base
	 */
	public Region getRegionInformation(String regionName, Integer depth) {
		if (Objects.isNull(baiDuMapProperties.getApiKey())) {
			throw new RuntimeException("Please configure your BaiDuMap API key in the application.yml file.");
		}
		String path = "/api_region_search/v1/";
		MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
			.add("ak", baiDuMapProperties.getApiKey())
			.add("keyword", regionName)
			.add("sub_admin", depth.toString())
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
	 * Get Weather Information
	 * @param cityCode the code of a city/province/district
	 * @return https://lbsyun.baidu.com/faq/api?title=webapi/weather/base
	 */
	public String getWeather(String cityCode) {
		if (Objects.isNull(baiDuMapProperties.getApiKey())) {
			throw new RuntimeException("Please configure your BaiDuMap API key in the application.yml file.");
		}
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
	 * Get detailed information about a certain location
	 * @param region the code/name of a city/province
	 * @param queryPlace location of inquiry
	 * @param isDetail when the value is true, returns detailed geographical coordinates;
	 * when false, returns only the administrative region.
	 * @return https://lbsyun.baidu.com/faq/api?title=webapi/guide/webservice-placeapi/district
	 */
	public String getAddressInformation(String region, String queryPlace, boolean isDetail) {
		if (Objects.isNull(baiDuMapProperties.getApiKey())) {
			throw new RuntimeException("Please configure your BaiDuMap API key in the application.yml file.");
		}
		String path = "/place/v2/search/";
		MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
			.add("ak", baiDuMapProperties.getApiKey())
			.add("query", queryPlace)
			.add("region", StringUtils.hasText(region) ? region : "china") // region
																			// default
																			// value
			.add("output", "json")
			.add("scope", isDetail ? "2" : "1") // get detail information
			.build();
		try {
			return webClientTool.get(path, params).block();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get information", e);
		}
	}

}
