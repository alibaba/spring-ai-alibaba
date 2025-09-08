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

package com.alibaba.cloud.ai.example.deepresearch.model.enums;

/**
 * Streaming node prefix enumeration.
 * Used to identify type prefixes of streaming output nodes, provided to frontend for display purposes.
 */
public enum StreamNodePrefixEnum {

	PLANNER_LLM_STREAM("planner_llm_stream", false), RESEARCHER_LLM_STREAM("researcher_llm_stream", true),
	RESEARCHER_REFLECT_LLM_STREAM("researcher_reflect_llm_stream", true),
	CODER_REFLECT_LLM_STREAM("coder_reflect_llm_stream", true), CODER_LLM_STREAM("coder_llm_stream", true),
	REPORTER_LLM_STREAM("reporter_llm_stream", true);

	/** Node prefix string */
	private final String prefix;

	/** Whether frontend display is required */
	private final boolean visible;

	/**
	 * Constructor
	 * @param prefix Node prefix string
	 * @param visible Whether frontend display is required
	 */
	StreamNodePrefixEnum(String prefix, boolean visible) {
		this.prefix = prefix;
		this.visible = visible;
	}

	/**
	 * Get prefix string
	 * @return prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Get visibility status
	 * @return visible
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Checks whether the specified node name begins with any predefined prefix from the enumeration
	 * @param nodeName name of the node to check
	 * @return the matching enumeration instance, or null if no prefix matches
	 */
	public static StreamNodePrefixEnum match(String nodeName) {
		for (StreamNodePrefixEnum p : values()) {
			if (nodeName != null && nodeName.startsWith(p.prefix)) {
				return p;
			}
		}
		return null;
	}

}
