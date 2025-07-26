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
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/**
 * 国家统计局数据查询服务
 *
 * @author makoto
 */
public class NationalStatisticsService
		implements Function<NationalStatisticsService.Request, NationalStatisticsService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(NationalStatisticsService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final NationalStatisticsProperties properties;

	public NationalStatisticsService(WebClientTool webClientTool, JsonParseTool jsonParseTool,
			NationalStatisticsProperties properties) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
	}

	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.dataType())) {
			logger.error("Invalid request: dataType is required.");
			return new Response("error", "数据类型不能为空", null, LocalDateTime.now().toString());
		}

		try {
			logger.info("Fetching national statistics data for type: {}, keyword: {}", request.dataType(),
					request.keyword());

			List<StatisticsItem> items = fetchStatisticsData(request.dataType(), request.keyword(), request.limit());

			if (items.isEmpty()) {
				return new Response("no_data", "未找到相关统计数据", items, LocalDateTime.now().toString());
			}

			logger.info("Successfully fetched {} statistics items", items.size());
			return new Response("success", "获取统计数据成功", items, LocalDateTime.now().toString());

		}
		catch (Exception e) {
			logger.error("Failed to fetch national statistics data: {}", e.getMessage(), e);
			return new Response("error", "获取统计数据失败: " + e.getMessage(), null, LocalDateTime.now().toString());
		}
	}

	/**
	 * 获取统计数据
	 */
	private List<StatisticsItem> fetchStatisticsData(String dataType, String keyword, int limit) {
		List<StatisticsItem> items = new ArrayList<>();

		try {
			String url = buildUrl(dataType);
			String htmlContent = webClientTool.get(url, null).block();

			if (!StringUtils.hasText(htmlContent)) {
				logger.warn("Empty response from statistics website");
				return items;
			}

			Document doc = Jsoup.parse(htmlContent);
			items = parseStatisticsData(doc, keyword, limit);

		}
		catch (Exception e) {
			logger.error("Error fetching statistics data: {}", e.getMessage(), e);
		}

		return items;
	}

	/**
	 * 构建请求URL
	 */
	private String buildUrl(String dataType) {
		switch (dataType.toLowerCase()) {
			case "zxfb":
			case "latest":
				return NationalStatisticsConstants.TJSJ_URL + "/zxfb/";
			case "tjgb":
			case "bulletin":
				return NationalStatisticsConstants.TJSJ_URL + "/tjgb/";
			case "ndsj":
			case "annual":
				return NationalStatisticsConstants.TJSJ_URL + "/ndsj/";
			case "ydsj":
			case "monthly":
				return NationalStatisticsConstants.TJSJ_URL + "/ydsj/";
			case "jdsj":
			case "quarterly":
				return NationalStatisticsConstants.TJSJ_URL + "/jdsj/";
			default:
				return NationalStatisticsConstants.TJSJ_URL + "/zxfb/";
		}
	}

	/**
	 * 解析统计数据
	 */
	private List<StatisticsItem> parseStatisticsData(Document doc, String keyword, int limit) {
		List<StatisticsItem> items = new ArrayList<>();

		try {
			// 查找统计数据列表
			Elements dataElements = doc.select("ul.center_list_contlist li, .news_list li, .list_item");

			if (dataElements.isEmpty()) {
				// 尝试其他可能的选择器
				dataElements = doc.select("a[href*=tjsj], a[href*=data]");
			}

			int count = 0;
			for (Element element : dataElements) {
				if (count >= limit)
					break;

				StatisticsItem item = parseStatisticsItem(element);
				if (item != null && (keyword == null || item.title().contains(keyword))) {
					items.add(item);
					count++;
				}
			}

		}
		catch (Exception e) {
			logger.error("Error parsing statistics data: {}", e.getMessage(), e);
		}

		return items;
	}

	/**
	 * 解析单个统计项目
	 */
	private StatisticsItem parseStatisticsItem(Element element) {
		try {
			String title = "";
			String url = "";
			String publishDate = "";
			String summary = "";

			// 查找标题和链接
			Element linkElement = element.select("a").first();
			if (linkElement != null) {
				title = linkElement.text().trim();
				url = linkElement.attr("href");
				if (url.startsWith("/")) {
					url = NationalStatisticsConstants.BASE_URL + url;
				}
			}

			// 查找发布日期
			Element dateElement = element.select(".date, .time, span[class*=date], span[class*=time]").first();
			if (dateElement != null) {
				publishDate = dateElement.text().trim();
			}

			// 查找摘要
			Element summaryElement = element.select(".summary, .desc, .content").first();
			if (summaryElement != null) {
				summary = summaryElement.text().trim();
			}

			if (StringUtils.hasText(title)) {
				return new StatisticsItem(title, url, publishDate, summary);
			}

		}
		catch (Exception e) {
			logger.error("Error parsing statistics item: {}", e.getMessage(), e);
		}

		return null;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("National Statistics Service API request")
	public record Request(@JsonProperty(required = true,
			value = "dataType") @JsonPropertyDescription("统计数据类型: zxfb(最新发布), tjgb(统计公报), ndsj(年度数据), ydsj(月度数据), jdsj(季度数据)") String dataType,

			@JsonProperty(required = false,
					value = "keyword") @JsonPropertyDescription("搜索关键词，用于过滤统计数据") String keyword,

			@JsonProperty(required = false, value = "limit") @JsonPropertyDescription("返回结果数量限制，默认10条") int limit) {
		public Request {
			if (limit <= 0) {
				limit = 10;
			}
		}
	}

	@JsonClassDescription("National Statistics Service API response")
	public record Response(String status, String message, List<StatisticsItem> data, String timestamp) {
	}

	@JsonClassDescription("Statistics item")
	public record StatisticsItem(String title, String url, String publishDate, String summary) {
	}

}
