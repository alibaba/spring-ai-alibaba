package com.alibaba.cloud.ai.plugin.crawler.entity;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@JsonClassDescription("Jina Reader API JinaResponse")
public record JinaResponse(

		@JsonProperty(required = true, value = "title")
		@JsonPropertyDescription("Target url title")
		String title,

		@JsonProperty(required = true, value = "url")
		@JsonPropertyDescription("Target url")
		String url,

		@JsonProperty(required = true, value = "content")
		@JsonPropertyDescription("Content of the target url")
		String content,

		@JsonProperty(required = true, value = "description")
		@JsonPropertyDescription("Target url website description")
		String description,

		@JsonProperty(required = true, value = "usage")
		@JsonPropertyDescription("Jina Reader API JinaUsage")
		JinaUsage usage
) {
}