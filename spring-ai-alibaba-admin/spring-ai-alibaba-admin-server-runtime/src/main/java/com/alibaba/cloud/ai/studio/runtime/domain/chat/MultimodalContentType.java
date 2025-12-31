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

package com.alibaba.cloud.ai.studio.runtime.domain.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum representing different types of multimodal content. Defines the supported content
 * types for multimodal messages.
 *
 * @since 1.0.0.3
 */

@Getter
@AllArgsConstructor
public enum MultimodalContentType {

	/**
	 * Text content type
	 */
	@JsonProperty("text")
	TEXT("text"),

	/**
	 * Image content type
	 */
	@JsonProperty("image")
	IMAGE("image"),;

	/**
	 * The string value of the content type
	 */
	private final String value;

}
