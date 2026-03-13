/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.tool.multimodal;

/**
 * Output format for multimodal tool results.
 *
 * <p><b>url</b> (default): Returns URL/URI reference. Model-friendly, minimal token usage.
 * Recommended when tool result is sent to the model as context.
 *
 * <p><b>base64</b>: Returns inline base64 data URL. Use when the consumer needs inline data
 * (e.g., returnDirect to client). Note: base64 significantly increases token usage when sent
 * to the model; prefer url for agent reasoning loops.
 */
public enum OutputFormat {

	/**
	 * URL/URI reference. Default, model-friendly.
	 */
	url,

	/**
	 * Inline base64 data URL. Use for client display; increases token usage when sent to model.
	 */
	base64;

	public static OutputFormat from(String value) {
		if (value == null || value.isBlank()) {
			return url;
		}
		String normalized = value.trim().toLowerCase();
		return "base64".equals(normalized) ? base64 : url;
	}
}
