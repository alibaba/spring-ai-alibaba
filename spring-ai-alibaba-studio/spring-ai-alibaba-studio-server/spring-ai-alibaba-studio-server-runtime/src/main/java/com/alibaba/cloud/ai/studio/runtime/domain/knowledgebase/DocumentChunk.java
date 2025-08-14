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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Represents a chunk of a document in the knowledge base. Each chunk contains a portion
 * of the document's content along with metadata.
 *
 * @since 1.0.0.3
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentChunk implements Serializable {

	/** Unique identifier of the document */
	@JsonProperty("doc_id")
	private String docId;

	/** Name of the document */
	@JsonProperty("doc_name")
	private String docName;

	/** Title of the document chunk */
	private String title;

	/** Content text of the chunk */
	private String text;

	/** Relevance score of the chunk */
	private Double score;

	/** Page number in the original document */
	@JsonProperty("page_number")
	private Integer pageNumber;

	/** Unique identifier of the chunk */
	@JsonProperty("chunk_id")
	private String chunkId;

	/** Whether the chunk is enabled */
	private Boolean enabled;

	/** ID of the workspace this chunk belongs to */
	@JsonProperty("workspace_id")
	private String workspaceId;

}
