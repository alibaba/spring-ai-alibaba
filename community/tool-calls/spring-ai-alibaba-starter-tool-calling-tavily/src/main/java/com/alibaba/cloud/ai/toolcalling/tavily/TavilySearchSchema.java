package com.alibaba.cloud.ai.toolcalling.tavily;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class TavilySearchSchema {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Request(
			@JsonProperty("query") String query,
			@JsonProperty("topic") String topic,
			@JsonProperty("search_depth") String searchDepth,
			@JsonProperty("chunks_per_source") Integer chunksPerSource,
			@JsonProperty("max_results") Integer maxResults,
			@JsonProperty("time_range") String timeRange,
			@JsonProperty("days") Integer days,
			@JsonProperty("include_answer") Boolean includeAnswer,
			@JsonProperty("include_raw_content") Boolean includeRawContent,
			@JsonProperty("include_images") Boolean includeImages,
			@JsonProperty("include_image_descriptions") Boolean includeImageDescriptions,
			@JsonProperty("include_domains") List<String> includeDomains,
			@JsonProperty("exclude_domains") List<String> excludeDomains
	) implements Serializable {
	}

	public record ResponseImage(
			@JsonProperty("url") String url,
			@JsonProperty("description") String description
	) implements Serializable {
	}

	public record ResponseResult(
			@JsonProperty("title") String title,
			@JsonProperty("url") String url,
			@JsonProperty("content") String content,
			@JsonProperty("score") Double score,
			@JsonProperty("raw_content") String rawContent
	) implements Serializable {
	}

	public record ResponseErrorDetail(
			@JsonProperty("error") String error
	) implements Serializable {
	}

	public record Response(
			@JsonProperty("query") String query,
			@JsonProperty("answer") String answer,
			@JsonProperty("images") List<ResponseImage> images,
			@JsonProperty("results") List<ResponseResult> results,
			@JsonProperty("response_time") String responseTime,
			@JsonProperty("detail") ResponseErrorDetail detail
	) implements Serializable {
	}

	public static Request simpleQuery(String query) {
		return new Request(query, null, null, null, null, null, null, null, null, null, null, null, null);
	}
}
