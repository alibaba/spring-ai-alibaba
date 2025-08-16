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

package com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase;

import com.alibaba.cloud.ai.studio.runtime.enums.ChunkType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Configuration for document processing. Defines how documents are split into chunks for
 * processing.
 *
 * @since 1.0.0.3
 */

@Data
public class ProcessConfig implements Serializable {

	/** Type of chunking strategy to use */
	@JsonProperty("chunk_type")
	private ChunkType chunkType;

	/** Regular expression used for splitting text into chunks */
	private String regex = "\\n\\n";

	/** Maximum size of each text chunk */
	@JsonProperty("chunk_size")
	private Integer chunkSize = 600;

	/** Number of characters to overlap between consecutive chunks */
	@JsonProperty("chunk_overlap")
	private Integer chunkOverlap = 100;

}
