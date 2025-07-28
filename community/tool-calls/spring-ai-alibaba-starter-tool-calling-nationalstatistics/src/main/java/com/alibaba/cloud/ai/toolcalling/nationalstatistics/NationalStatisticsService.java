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
package com.alibaba.cloud.ai.toolcalling.nationalstatistics;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * 国家统计局数据查询服务
 *
 * @author Makoto
 */
public class NationalStatisticsService
		implements SearchService, Function<NationalStatisticsService.Request, NationalStatisticsService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(NationalStatisticsService.class);

	private final JsonParseTool jsonParseTool;

	private final WebClientTool webClientTool;

	public NationalStatisticsService(JsonParseTool jsonParseTool, WebClientTool webClientTool) {
		this.jsonParseTool = jsonParseTool;
		this.webClientTool = webClientTool;
	}

	@Override
	public SearchService.Response query(String query) {
		return this.apply(Request.simpleQuery(query));
	}

	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.keyword())) {
			return Response.error(request != null ? request.keyword : "", "查询关键词不能为空");
		}

		try {
			logger.info("调用国家统计局API查询数据，关键词：{}", request.keyword);

			// 构建查询参数
			Map<String, Object> params = buildQueryParams(request);

			// 发送HTTP请求
			String responseData = webClientTool.post("easyquery.htm", params).block();

			if (!StringUtils.hasText(responseData)) {
				return Response.error(request.keyword, "API返回空响应");
			}

			// 验证响应内容类型
			if (responseData.trim().startsWith("<")) {
				logger.error("API返回HTML内容而非JSON，可能是访问被重定向或API变更。响应内容前100字符：{}",
						responseData.length() > 100 ? responseData.substring(0, 100) : responseData);
				return Response.error(request.keyword, "API返回格式错误，可能是接口变更或访问限制");
			}

			// 记录响应内容用于调试
			if (logger.isDebugEnabled()) {
				logger.debug("API响应内容：{}", responseData);
			}

			// 解析响应
			return parseResponse(responseData, request);

		}
		catch (Exception e) {
			logger.error("查询国家统计局数据失败：{}", e.getMessage(), e);
			return Response.error(request.keyword, "查询国家统计局数据失败：" + e.getMessage());
		}
	}

	/**
	 * 构建查询参数
	 */
	private Map<String, Object> buildQueryParams(Request request) {
		Map<String, Object> params = new HashMap<>();

		// 基础参数 - 使用国家统计局API的标准参数格式
		params.put("m", "QueryData");
		// 宏观数据库
		params.put("dbcode", "hgnd");
		// 指标作为行
		params.put("rowcode", "zb");
		// 时间作为列
		params.put("colcode", "sj");
		// 无筛选条件
		params.put("wds", "[]");
		params.put("dfwds", buildDfwds(request));
		// 添加时间戳避免缓存
		params.put("k1", System.currentTimeMillis());

		return params;
	}

	/**
	 * 构建dfwds参数
	 */
	private String buildDfwds(Request request) {
		List<Map<String, String>> dfwds = new ArrayList<>();

		// 指标条件
		if (StringUtils.hasText(request.keyword)) {
			Map<String, String> zbCondition = new HashMap<>();
			zbCondition.put("wdcode", "zb");
			zbCondition.put("valuecode", getIndicatorCode(request.keyword));
			dfwds.add(zbCondition);
		}

		// 时间条件 - 如果没有指定年份，查询最近几年的数据
		Map<String, String> sjCondition = new HashMap<>();
		sjCondition.put("wdcode", "sj");
		if (StringUtils.hasText(request.year)) {
			sjCondition.put("valuecode", request.year);
		}
		else {
			// 默认查询2020-2023年的数据
			sjCondition.put("valuecode", "2020,2021,2022,2023");
		}
		dfwds.add(sjCondition);

		try {
			String result = jsonParseTool.objectToJson(dfwds);
			logger.debug("构建的dfwds参数：{}", result);
			return result;
		}
		catch (Exception e) {
			logger.warn("构建dfwds参数失败：{}", e.getMessage());
			return "[{\"wdcode\":\"zb\",\"valuecode\":\"A020101\"},{\"wdcode\":\"sj\",\"valuecode\":\"2023\"}]";
		}
	}

	/**
	 * 根据关键词获取指标代码
	 */
	private String getIndicatorCode(String keyword) {
		// 常见统计指标映射
		Map<String, String> indicatorMap = new HashMap<>();
		indicatorMap.put("GDP", "A020101");
		indicatorMap.put("人口", "A030101");
		indicatorMap.put("就业", "A040101");
		indicatorMap.put("消费", "A050101");
		indicatorMap.put("投资", "A060101");
		indicatorMap.put("进出口", "A070101");
		indicatorMap.put("工业", "A020201");
		indicatorMap.put("农业", "A080101");
		indicatorMap.put("服务业", "A020301");
		indicatorMap.put("房地产", "A060201");

		// 模糊匹配
		for (Map.Entry<String, String> entry : indicatorMap.entrySet()) {
			if (keyword.contains(entry.getKey())) {
				return entry.getValue();
			}
		}

		// 默认返回GDP指标
		return "A020101";
	}

	/**
	 * 解析响应
	 */
	private Response parseResponse(String responseBody, Request request) {
		try {
			Map<String, Object> jsonResponse = jsonParseTool.jsonToObject(responseBody, new TypeReference<>() {
			});

			// 检查返回码
			if (jsonResponse.containsKey("returncode")) {
				Integer returnCode = (Integer) jsonResponse.get("returncode");
				if (returnCode == null || returnCode != 200) {
					String returnMsg = (String) jsonResponse.getOrDefault("returnmsg", "未知错误");
					return Response.error(request.keyword, "API返回错误：" + returnMsg);
				}
			}

			// 解析数据
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> dataNodes = (List<Map<String, Object>>) jsonResponse.get("returndata");
			if (dataNodes == null || dataNodes.isEmpty()) {
				return Response.success(request.keyword, "未找到相关统计数据", new ArrayList<>());
			}

			List<StatisticsData> dataList = new ArrayList<>();
			for (Map<String, Object> dataNode : dataNodes) {
				StatisticsData data = parseStatisticsData(dataNode);
				if (data != null) {
					dataList.add(data);
				}
			}

			String message = String.format("成功查询到 %d 条统计数据", dataList.size());
			return Response.success(request.keyword, message, dataList);

		}
		catch (Exception e) {
			logger.error("解析统计局响应失败：{}", e.getMessage(), e);
			return Response.error(request.keyword, "解析统计局响应失败：" + e.getMessage());
		}
	}

	/**
	 * 解析统计数据
	 */
	private StatisticsData parseStatisticsData(Map<String, Object> dataNode) {
		try {
			StatisticsData data = new StatisticsData();
			data.setName((String) dataNode.getOrDefault("zb_name", ""));
			data.setValue((String) dataNode.getOrDefault("data_value", ""));
			data.setUnit((String) dataNode.getOrDefault("unit_name", ""));
			data.setYear((String) dataNode.getOrDefault("sj_name", ""));
			data.setCode((String) dataNode.getOrDefault("zb_code", ""));
			return data;
		}
		catch (Exception e) {
			logger.warn("解析单条统计数据失败：{}", e.getMessage());
			return null;
		}
	}

	/**
	 * 请求类
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("国家统计局数据查询请求")
	public record Request(
			@JsonProperty(required = true) @JsonPropertyDescription("查询关键词，如：GDP、人口、就业、消费、投资等") String keyword,

			@JsonProperty @JsonPropertyDescription("查询年份，格式：YYYY，如：2023") String year,

			@JsonProperty @JsonPropertyDescription("查询地区，如：全国、北京市等") String region)
			implements
				Serializable,
				SearchService.Request {

		public static Request simpleQuery(String keyword) {
			return new Request(keyword, null, null);
		}

		@Override
		public String getQuery() {
			return this.keyword();
		}
	}

	/**
	 * 响应类
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(@JsonProperty("query") String query, @JsonProperty("success") boolean success,
			@JsonProperty("message") String message,
			@JsonProperty("data") List<StatisticsData> data) implements SearchService.Response {

		public static Response success(String query, String message, List<StatisticsData> data) {
			return new Response(query, true, message, data);
		}

		public static Response error(String query, String message) {
			return new Response(query, false, message, new ArrayList<>());
		}

		@Override
		public SearchService.SearchResult getSearchResult() {
			return new SearchService.SearchResult(this.data()
				.stream()
				.map(item -> new SearchService.SearchContent(item.getName(), item.getValue() + item.getUnit(),
						NationalStatisticsConstants.BASE_URL, null))
				.toList());
		}
	}

	/**
	 * 统计数据类
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class StatisticsData {

		@JsonProperty
		@JsonPropertyDescription("指标名称")
		private String name;

		@JsonProperty
		@JsonPropertyDescription("数据值")
		private String value;

		@JsonProperty
		@JsonPropertyDescription("计量单位")
		private String unit;

		@JsonProperty
		@JsonPropertyDescription("统计年份")
		private String year;

		@JsonProperty
		@JsonPropertyDescription("指标代码")
		private String code;

		// Getters and Setters
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getUnit() {
			return unit;
		}

		public void setUnit(String unit) {
			this.unit = unit;
		}

		public String getYear() {
			return year;
		}

		public void setYear(String year) {
			this.year = year;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

	}

}
