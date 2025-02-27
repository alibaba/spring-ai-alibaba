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
package com.alibaba.cloud.ai.dashscope.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * @author kevinlin09
 */
public class DashScopeAgentRagOptions {

	/** knowledge base ids */
	@JsonProperty("pipeline_ids")
	private List<String> pipelineIds;

	/** file ids of knowledge base */
	@JsonProperty("file_ids")
	private List<String> fileIds;

	/** tags of knowledge base */
	@JsonProperty("tags")
	private List<String> tags;

	/** metadata filter of knowledge base query */
	@JsonProperty("metadata_filter")
	private JsonNode metadataFilter;

	/** structured filter of knowledge base query */
	@JsonProperty("structured_filter")
	private JsonNode structuredFilter;

	/** file ID is a temporary file associated with the current session */
	@JsonProperty("session_file_ids")
	private List<String> sessionFileIds;

	public List<String> getPipelineIds() {
		return pipelineIds;
	}

	public void setPipelineIds(List<String> pipelineIds) {
		this.pipelineIds = pipelineIds;
	}

	public List<String> getFileIds() {
		return fileIds;
	}

	public void setFileIds(List<String> fileIds) {
		this.fileIds = fileIds;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public JsonNode getMetadataFilter() {
		return metadataFilter;
	}

	public void setMetadataFilter(JsonNode metadataFilter) {
		this.metadataFilter = metadataFilter;
	}

	public JsonNode getStructuredFilter() {
		return structuredFilter;
	}

	public void setStructuredFilter(JsonNode structuredFilter) {
		this.structuredFilter = structuredFilter;
	}

	public List<String> getSessionFileIds() {
		return sessionFileIds;
	}

	public void setSessionFileIds(List<String> sessionFileIds) {
		this.sessionFileIds = sessionFileIds;
	}

	public static DashScopeAgentRagOptions.Builder builder() {

		return new DashScopeAgentRagOptions.Builder();
	}

	public static class Builder {

		protected DashScopeAgentRagOptions options;

		public Builder() {
			this.options = new DashScopeAgentRagOptions();
		}

		public Builder(DashScopeAgentRagOptions options) {
			this.options = options;
		}

		public Builder withPipelineIds(List<String> pipelineIds) {
			this.options.pipelineIds = pipelineIds;
			return this;
		}

		public Builder withFileIds(List<String> fileIds) {
			this.options.fileIds = fileIds;
			return this;
		}

		public Builder withTags(List<String> tags) {
			this.options.tags = tags;
			return this;
		}

		public Builder withMetadataFilter(JsonNode metadataFilter) {
			this.options.metadataFilter = metadataFilter;
			return this;
		}

		public Builder withStructuredFilter(JsonNode structuredFilter) {
			this.options.structuredFilter = structuredFilter;
			return this;
		}

		public Builder withSessionFileIds(List<String> sessionFileIds) {
			this.options.sessionFileIds = sessionFileIds;
			return this;
		}

		public DashScopeAgentRagOptions build() {
			return this.options;
		}

	}

}
