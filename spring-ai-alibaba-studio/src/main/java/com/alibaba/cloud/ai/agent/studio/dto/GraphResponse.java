/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.agent.studio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for graph representation response. Contains the graph diagram in Mermaid or DOT format
 * for UI visualization.
 */
public class GraphResponse {

	@JsonProperty("dotSrc")
	public String dotSrc;

	@JsonProperty("mermaidSrc")
	public String mermaidSrc;

	/**
	 * Constructs a GraphResponse with Mermaid source (preferred for UI).
	 *
	 * @param mermaidSrc The graph source in Mermaid format.
	 */
	public GraphResponse(String mermaidSrc, boolean useMermaid) {
		if (useMermaid) {
			this.mermaidSrc = mermaidSrc;
			this.dotSrc = null;
		}
		else {
			this.dotSrc = mermaidSrc;
			this.mermaidSrc = null;
		}
	}

	/**
	 * Constructs a GraphResponse with DOT source (legacy).
	 *
	 * @param dotSrc The graph source string (e.g., in DOT format).
	 */
	public GraphResponse(String dotSrc) {
		this.dotSrc = dotSrc;
		this.mermaidSrc = null;
	}

	public GraphResponse() {
	}

	public String getDotSrc() {
		return dotSrc;
	}

	public String getMermaidSrc() {
		return mermaidSrc;
	}
}
