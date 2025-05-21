/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author yingzi
 * @date 2025/5/18 14:52
 */
@Data
@Builder
public class TavilySearchRequest {

	@JsonProperty("query")
	private String query;

	@JsonProperty("topic")
	private String topic;

	@JsonProperty("search_depth")
	private String searchDepth;

	@JsonProperty("chunks_per_source")
	private int chunksPerSource;

	@JsonProperty("max_results")
	private int maxResults;

	@JsonProperty("time_range")
	private String timeRange;

	@JsonProperty("days")
	private int days;

	@JsonProperty("include_answer")
	private boolean includeAnswer;

	@JsonProperty("include_raw_content")
	private boolean includeRawContent;

	@JsonProperty("include_images")
	private boolean includeImages;

	@JsonProperty("include_image_descriptions")
	private boolean includeImageDescriptions;

	@JsonProperty("include_domains")
	private List<String> includeDomains;

	@JsonProperty("exclude_domains")
	private List<String> excludeDomains;

}
