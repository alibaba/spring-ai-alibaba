package com.alibaba.cloud.ai.functioncalling.crawler;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@JsonClassDescription("JinaUsage information for the Jina Reader API")
public record JinaUsage(

		@JsonProperty(required = true, value = "tokens") @JsonPropertyDescription("Number of tokens used") int tokens) {
}
