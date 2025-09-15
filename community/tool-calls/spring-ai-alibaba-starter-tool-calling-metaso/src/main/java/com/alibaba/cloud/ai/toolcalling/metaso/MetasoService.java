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
package com.alibaba.cloud.ai.toolcalling.metaso;

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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Function;

/**
 * Metaso AI Search https://metaso.cn/search-api/playground
 *
 * @author HunterPorter
 * @author <a href="mailto:zongpeng_hzp@163.com">HunterPorter</a>
 */
public class MetasoService implements SearchService, Function<MetasoService.Request, MetasoService.Response> {

	private static final Logger log = LoggerFactory.getLogger(MetasoService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final MetasoProperties properties;

	public MetasoService(WebClientTool webClientTool, JsonParseTool jsonParseTool, MetasoProperties properties) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
	}

	@Override
	public SearchService.Response query(String query) {
		return this.apply(Request.simplyQuery(query));
	}

	@Override
	public Response apply(Request request) {
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new RuntimeException("Service Api Key is Invalid.");
		}
		try {
			String responseStr = webClientTool.post("search", request).block();
			log.debug("Response: {}", responseStr);
			return jsonParseTool.jsonToObject(responseStr, Response.class);
		}
		catch (Exception e) {
			log.error("Service Metaso Request Error: ", e);
			throw new RuntimeException(e);
		}
	}

	@JsonClassDescription("根据问题或者关键词，返回网页等数据源的实时搜索结果")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Request(@JsonProperty(required = true, value = "q") @JsonPropertyDescription("查询") String q,
			@JsonProperty(value = "scope",
					defaultValue = "webpage") @JsonPropertyDescription("搜索范围: `webpage` (网页), `document` (文库), `scholar` (学术), `image` (图片), `video` (视频), `podcast` (播客).") String scope,
			@JsonProperty(value = "size", defaultValue = "10") @JsonPropertyDescription("结果数量. 默认值: 10") Integer size,
			@JsonProperty(value = "includeSummary",
					defaultValue = "true") @JsonPropertyDescription("通过网页的摘要信息进行召回增强. 默认值: true") Boolean includeSummary,
			@JsonProperty(value = "includeRowContent",
					defaultValue = "false") @JsonPropertyDescription("抓取所有来源网页原文`. 默认值: false") Boolean includeRowContent)
			implements
				SearchService.Request {

		@Override
		public String getQuery() {
			return this.q();
		}

		public static Request simplyQuery(String query) {
			return new Request(query, "webpage", 10, true, false);
		}
	}

	@JsonClassDescription("Metaso AI Search Response")
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(@JsonProperty("credits") Integer credits, @JsonProperty("total") Integer total,
			@JsonProperty("searchParameters") @JsonIgnoreProperties(ignoreUnknown = true) Request searchParameters,
			@JsonProperty("webpages") List<WebPage> webpages, @JsonProperty("documents") List<WebPage> documents,
			@JsonProperty("scholars") List<WebPage> scholars, @JsonProperty("images") List<Image> images,
			@JsonProperty("videos") List<Video> videos,
			@JsonProperty("podcasts") List<Podcast> podcasts) implements SearchService.Response {

		@Override
		public SearchResult getSearchResult() {
			Assert.isTrue(StringUtils.hasText(this.searchParameters.scope), "searchParameters.scope is invalid");
			ResultType resultType = ResultType.getByType(this.searchParameters.scope);
			Assert.notNull(resultType, "resultType is null");
			switch (resultType) {
				case WEBPAGE -> {
					return new SearchResult(this.webpages()
						.stream()
						.map(item -> new SearchContent(item.title(), item.snippet(), item.link(), null))
						.toList());
				}
				case DOCUMENT -> {
					return new SearchResult(this.documents()
						.stream()
						.map(item -> new SearchContent(item.title(), item.snippet(), item.link(), null))
						.toList());
				}
				case SCHOLAR -> {
					return new SearchResult(this.scholars()
						.stream()
						.map(item -> new SearchContent(item.title(), item.snippet(), item.link(), null))
						.toList());
				}
				case IMAGE -> {
					return new SearchResult(this.images()
						.stream()
						.map(item -> new SearchContent(item.title(), null, item.imageUrl(), null))
						.toList());
				}
				case VIDEO -> {
					return new SearchResult(this.videos()
						.stream()
						.map(item -> new SearchContent(item.title(), item.snippet(), item.link(), item.coverImage))
						.toList());
				}
				case Podcast -> {
					return new SearchResult(this.podcasts()
						.stream()
						.map(item -> new SearchContent(item.title(), item.snippet(), item.link(), null))
						.toList());
				}
				default -> throw new IllegalStateException("Invalid result type: " + resultType);
			}
		}

		public record WebPage(String title, String link, String snippet, String content, Integer position, String score,
				String date, String summary, List<String> authors) {
		}

		public record Image(String title, Integer position, String score, String imageUrl, Integer imageWidth,
				Integer imageHeight) {
		}

		public record Video(String title, String link, String snippet, Integer position, String score, String date,
				String duration, String coverImage, List<String> authors) {
		}

		public record Podcast(String title, String link, String snippet, Integer position, String score, String date,
				String duration, List<String> authors) {
		}

		public enum ResultType {

			WEBPAGE("webpage"), DOCUMENT("document"), SCHOLAR("scholar"), IMAGE("image"), VIDEO("video"),
			Podcast("podcast");

			ResultType(String type) {
				this.type = type;
			}

			public static ResultType getByType(String type) {
				for (ResultType resultType : values()) {
					if (resultType.type.equals(type)) {
						return resultType;
					}
				}
				return null;
			}

			private final String type;

		}
	}

}
