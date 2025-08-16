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

package com.alibaba.cloud.ai.studio.runtime.domain.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.alibaba.cloud.ai.studio.runtime.enums.UploadType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * Policy for file upload operations.
 *
 * @since 1.0.0.3
 */

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class UploadPolicy implements Serializable {

	/** Name of the file to be uploaded */
	private String name;

	/** Target path for the file upload */
	private String path;

	/** File extension */
	private String extension;

	/** MIME type of the file */
	@JsonProperty("content_type")
	private String contentType;

	/** Size of the file in bytes */
	private Long size;

	/** upload file */
	@JsonProperty("upload_type")
	@Builder.Default
	private UploadType uploadType = UploadType.FILE;

}
