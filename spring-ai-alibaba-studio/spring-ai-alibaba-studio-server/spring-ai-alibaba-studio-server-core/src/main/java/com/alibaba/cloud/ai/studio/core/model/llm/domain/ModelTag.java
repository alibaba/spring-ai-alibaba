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
package com.alibaba.cloud.ai.studio.core.model.llm.domain;

/**
 * Enum representing different capabilities of LLM models.
 */
public enum ModelTag {

	/** Vision capability for image processing */
	vision("视觉"),
	/** Web search capability for internet access */
	web_search("联网"),
	/** Embedding capability for vector representations */
	embedding("嵌入"),
	/** Reasoning capability for logical thinking */
	reasoning("推理"),
	/** Function call capability for tool usage */
	function_call("工具调用");

	/** Display name in Chinese */
	private final String displayName;

	ModelTag(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Gets the Chinese display name of the model tag.
	 * @return the display name
	 */
	public String getDisplayName() {
		return this.displayName;
	}

}
