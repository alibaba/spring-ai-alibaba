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
import java.util.*;
import java.util.function.Function;

/**
 * 国家统计局数据查询服务
 *
 * @author makoto
 */
public class NationalStatisticsService
		implements SearchService, Function<NationalStatisticsService.Request, NationalStatisticsService.Response> {

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
	public SearchService.Response query(String query) {
		return this.apply(new Request("zxfb", query, 10));
	}

	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.dataType())) {
			logger.error("Invalid request: dataType is required.");
			return Response.errorResponse("", "数据类型不能为空");
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
			return Response.errorResponse(request.dataType(), "获取统计数据失败: " + e.getMessage());
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
			logger.error("Error fetching statistics data from URL for dataType {}: {}", dataType, e.getMessage(), e);
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
			Elements dataElements = doc.select("ul.center_list_contlist li");

			if (dataElements.isEmpty()) {
				// 尝试其他可能的选择器
				dataElements = doc.select(".news_list li, .list_item, .cont_list li");
			}

			if (dataElements.isEmpty()) {
				dataElements = doc.select("li:has(a[href*=tjsj]), li:has(a[href*=html])");
			}

			int count = 0;
			for (Element element : dataElements) {
				if (count >= limit)
					break;

				StatisticsItem item = parseStatisticsItem(element);
				if (item != null && isValidItem(item, keyword)) {
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
	 * 验证统计项目是否有效
	 */
	private boolean isValidItem(StatisticsItem item, String keyword) {
		if (!StringUtils.hasText(item.title())) {
			return false;
		}

		if (keyword == null || keyword.trim().isEmpty()) {
			return true;
		}

		String lowerKeyword = keyword.toLowerCase();
		return item.title().toLowerCase().contains(lowerKeyword)
				|| (item.summary() != null && item.summary().toLowerCase().contains(lowerKeyword));
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
				if (StringUtils.hasText(url) && url.startsWith("/")) {
					url = NationalStatisticsConstants.BASE_URL + url;
				}
			}

			// 查找发布日期 - 尝试多种选择器
			Element dateElement = element.select(".date, .time, span[class*=date], span[class*=time]").first();
			if (dateElement == null) {
				// 尝试查找包含日期模式的文本
				Elements spans = element.select("span");
				for (Element span : spans) {
					String text = span.text().trim();
					if (text.matches("\\d{4}-\\d{2}-\\d{2}") || text.matches("\\d{4}/\\d{2}/\\d{2}")) {
						publishDate = text;
						break;
					}
				}
			}
			else {
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
			logger.debug("Error parsing statistics item: {}", e.getMessage());
		}

		return null;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("National Statistics Service API request")
	public record Request(@JsonProperty(required = true,
			value = "dataType") @JsonPropertyDescription("统计数据类型: zxfb(最新发布), tjgb(统计公报), ndsj(年度数据), ydsj(月度数据), jdsj(季度数据)") String dataType,

			@JsonProperty(required = false,
					value = "keyword") @JsonPropertyDescription("搜索关键词，用于过滤统计数据") String keyword,

			@JsonProperty(required = false, value = "limit") @JsonPropertyDescription("返回结果数量限制，默认10条") int limit)
			implements
				SearchService.Request {
		public Request {
			if (limit <= 0) {
				limit = 10;
			}
		}

		@Override
		public String getQuery() {
			return keyword != null ? keyword : "";
		}
	}

	@JsonClassDescription("National Statistics Service API response")
	public record Response(String status, String message, List<StatisticsItem> data,
			String timestamp) implements SearchService.Response {

		public static Response errorResponse(String dataType, String errorMsg) {
			return new Response("error", errorMsg, null, LocalDateTime.now().toString());
		}

		@Override
		public SearchResult getSearchResult() {
			if (data == null || data.isEmpty()) {
				return new SearchResult(List.of());
			}

			return new SearchResult(data.stream()
				.map(item -> new SearchService.SearchContent(item.title(),
						item.summary() != null ? item.summary() : item.title(), item.url(), null // 国家统计局没有特定的图标
				))
				.toList());
		}
	}

	@JsonClassDescription("Statistics item")
	public record StatisticsItem(String title, String url, String publishDate, String summary) {
	}

}
