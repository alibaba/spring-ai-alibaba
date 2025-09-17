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
package com.alibaba.cloud.ai.toolcalling.baidusearch;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Function;

/**
 * baidu AI Search https://cloud.baidu.com/doc/AppBuilder/s/pmaxd1hvy
 * 该版本相对稳定，但需要配置apiKey，每天有100次免费查询次数
 *
 * @author HunterPorter
 * @author <a href="mailto:zongpeng_hzp@163.com">HunterPorter</a>
 */
public class BaiduAiSearchService
		implements SearchService, Function<BaiduAiSearchService.Request, BaiduAiSearchService.Response> {

	private static final Logger log = LoggerFactory.getLogger(BaiduAiSearchService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final BaiduAiSearchProperties properties;

	public BaiduAiSearchService(WebClientTool webClientTool, JsonParseTool jsonParseTool,
			BaiduAiSearchProperties properties) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
	}

	@Override
	public Response query(String query) {
		return this.apply(Request.simplyQuery(query));
	}

	@Override
	public Response apply(Request request) {
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new RuntimeException("Service Api Key is Invalid.");
		}
		try {
			String responseStr = webClientTool.post("/v2/ai_search/chat/completions", request).block();
			log.debug("Response: {}", responseStr);
			return jsonParseTool.jsonToObject(responseStr, Response.class);
		}
		catch (Exception e) {
			log.error("Service Baidu AI Request Error: ", e);
			throw new RuntimeException(e);
		}
	}

	@JsonClassDescription("根据问题或者关键词，返回网页等数据源的实时搜索结果")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Request(
			@JsonProperty(required = true,
					value = "messages") @JsonPropertyDescription("搜索输入，包含用户查询内容") List<Message> messages,
			@JsonProperty(value = "edition", defaultValue = "standard") @JsonPropertyDescription("""
					搜索版本。默认为standard。
					可选值：
					standard：完整版本。
					lite：标准版本，对召回规模和精排条数简化后的版本，时延表现更好，效果略弱于完整版。""") String edition,
			@JsonProperty(value = "search_source",
					defaultValue = "baidu_search_v2") @JsonPropertyDescription("使用的搜索引擎版本；固定值：baidu_search_v2") String searchSource,
			@JsonProperty(
					value = "resource_type_filter") @JsonPropertyDescription("""
							支持设置网页、视频、图片、阿拉丁搜索模态，网页top_k最大取值为50，视频top_k最大为10，图片top_k最大为30，阿拉丁top_k最大为5，默认值为：
							[{"type": "web","top_k": 20},{"type": "video","top_k": 0},{"type": "image","top_k": 0},{"type": "aladdin","top_k": 0}]
							使用阿拉丁时注意：
							1. 阿拉丁不支持站点、时效过滤。
							2. 建议搭配网页模态使用，增加搜索返回数量。
							3. 阿拉丁的返回参数为beta版本，后续可能变更。""") List<SearchResource> resourceTypeFilter,
			@JsonProperty(value = "search_filter") @JsonPropertyDescription("根据条件做检索过滤") SearchFilter searchFilter,
			@JsonProperty(value = "block_websites") @JsonPropertyDescription("需要屏蔽的站点列表") List<String> blockWebsites,
			@JsonProperty(value = "search_recency_filter") @JsonPropertyDescription("""
					根据网页发布时间进行筛选。
					枚举值:
					week:最近7天
					month：最近30天
					semiyear：最近180天
					year：最近365天""") String searchRecencyFilter) implements SearchService.Request {

		@Override
		public String getQuery() {
			if (messages != null && !messages.isEmpty()) {
				Message lastMessage = messages.get(messages.size() - 1);
				if ("user".equals(lastMessage.role)) {
					return lastMessage.content;
				}
			}
			return null;
		}

		public static Request simplyQuery(String query) {
			return new Request(List.of(new Message("user", query)), "standard", "baidu_search_v2",
					List.of(new SearchResource("web", 20)), null, null, null);
		}
	}

	public record Message(@JsonProperty("role") @JsonPropertyDescription("角色设定，可选值：user：用户;assistant：模型") String role,
			@JsonProperty("content") @JsonPropertyDescription("""
					content为文本时, 对应对话内容，即用户的query问题。说明：
					1.不能为空。
					2.多轮对话中，用户最后一次输入content不能为空字符，如空格、\"\\n\"、“\\r”、“\\f”等。""") String content) {
	}

	public record SearchResource(@JsonProperty("type") @JsonPropertyDescription("""
			搜索资源类型。可选值：
			web：网页
			video：视频
			image：图片
			aladdin：阿拉丁""") String type, @JsonProperty("top_k") @JsonPropertyDescription("指定模态最大返回个数。") Integer topK) {
	}

	public record SearchFilter(@JsonProperty("match") @JsonPropertyDescription("站点条件查询") Match match,
			@JsonProperty("range") @JsonPropertyDescription("时间范围查询") Range range) {
	}

	public record Match(
			@JsonProperty("site") @JsonPropertyDescription("支持设置指定站点的搜索条件，即仅在设置的站点中进行内容搜索。目前支持设置20个站点。示例：[\"tieba.baidu.com\"]") List<String> site) {
	}

	public record Range(@JsonProperty("page_time") PageTime pageTime) {
	}

	public record PageTime(
			@JsonProperty("gte") @JsonPropertyDescription("时间查询参数，大于或等于。支持的时间单位：y（年）、M（月）、w（周）、d（日）,例如\"now-1w/d\"，前一周、向下做舍入") String gte,
			@JsonProperty("gt") @JsonPropertyDescription("时间查询参数，大于。支持的时间单位：y（年）、M（月）、w（周）、d（日）,例如\"now-1w/d\"，前一周、向上做舍入") String gt,
			@JsonProperty("lte") @JsonPropertyDescription("时间查询参数，小于或等于。支持的时间单位：y（年）、M（月）、w（周）、d（日）,例如\"now-1w/d\"，前一周、向上做舍入") String lte,
			@JsonProperty("lt") @JsonPropertyDescription("时间查询参数，小于。支持的时间单位：y（年）、M（月）、w（周）、d（日）,例如\"now-1w/d\"，前一周、向下做舍入") String lt) {
	}

	@JsonClassDescription("Baidu AI Search Response")
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(@JsonProperty("requestId") @JsonPropertyDescription("请求ID") String requestId,
			@JsonProperty("code") @JsonPropertyDescription("错误码，当发生异常时返回") String code,
			@JsonProperty("message") @JsonPropertyDescription("错误信息，当发生异常时返回") String message,
			@JsonProperty("references") @JsonPropertyDescription("搜索结果列表") List<Reference> references)
			implements
				SearchService.Response {

		@Override
		public SearchResult getSearchResult() {
			if (references == null || references.isEmpty()) {
				return new SearchResult(List.of());
			}

			return new SearchResult(this.references()
				.stream()
				.map(item -> new SearchContent(item.title(), item.content(), item.url(), null))
				.toList());
		}

		public record Reference(@JsonProperty("icon") @JsonPropertyDescription("网站图标地址") String icon,
				@JsonProperty("id") @JsonPropertyDescription("引用编号") Integer id,
				@JsonProperty("title") @JsonPropertyDescription("标题") String title,
				@JsonProperty("url") @JsonPropertyDescription("网址") String url,
				@JsonProperty("web_anchor") @JsonPropertyDescription("锚点") String webAnchor,
				@JsonProperty("website") @JsonPropertyDescription("网站名称") String website,
				@JsonProperty("content") @JsonPropertyDescription("内容") String content,
				@JsonProperty("date") @JsonPropertyDescription("日期") String date,
				@JsonProperty("type") @JsonPropertyDescription("""
						检索资源类型。返回值：
						web:网页
						video:视频内容
						image：图片
						aladdin：阿拉丁""") String type,
				@JsonProperty("image") @JsonPropertyDescription("图片信息") ImageDetail image,
				@JsonProperty("video") @JsonPropertyDescription("视频信息") VideoDetail video,
				@JsonProperty("is_aladdin") @JsonPropertyDescription("是否是阿拉丁内容") Boolean isAladdin,
				@JsonProperty("aladdin") @JsonPropertyDescription("阿拉丁内容") Object aladdin) {
		}

		public record ImageDetail(String url, String height, String width) {
		}

		public record VideoDetail(String url, String height, String width, String size, String duration,
				String hoverPic) {
		}
	}

}
