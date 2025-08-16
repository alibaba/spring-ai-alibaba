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
package com.alibaba.cloud.ai.studio.runtime.domain.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Represents a file entity with basic file information and metadata.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class File implements Serializable {

	/**
	 * Type of the file, defaults to 'custom'
	 */
	private String type = TypeEnum.custom.name();

	/**
	 * Size of the file in bytes
	 */
	private Long size;

	/**
	 * Name of the file
	 */
	private String name;

	/**
	 * MIME type of the file
	 */
	@JsonProperty("mime_type")
	private String mimeType;

	/**
	 * Source of the file
	 * @see SourceEnum
	 */
	private String source;

	/**
	 * URL of the file
	 */
	private String url;

	/**
	 * File type enumeration. Currently only 'custom' is used without further
	 * classification
	 */
	enum TypeEnum {

		image, document, audio, video, custom

	}

	/**
	 * Source type enumeration for the file
	 */
	public enum SourceEnum {

		localFile, remoteUrl

	}

}
