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

package com.alibaba.cloud.ai.studio.runtime.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum representing different types of text chunking strategies. Each type defines a
 * specific method for dividing text into chunks.
 *
 * @since 1.0.0.3
 */

@Getter
@AllArgsConstructor
public enum ChunkType {

	/** Chunks text based on character length */
	@JsonProperty("length")
	LENGTH("length"),

	/** Chunks text based on page boundaries */
	@JsonProperty("page")
	PAGE("page"),

	/** Chunks text based on title sections */
	@JsonProperty("title")
	TITLE("title"),

	/** Chunks text based on regex patterns */
	@JsonProperty("regex")
	REGEX("regex"),;

	/** The string value representing the chunk type */
	private final String value;

}
