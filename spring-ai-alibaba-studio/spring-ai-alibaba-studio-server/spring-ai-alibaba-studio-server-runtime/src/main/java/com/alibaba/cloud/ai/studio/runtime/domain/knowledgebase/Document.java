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

import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.DocumentIndexStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a document in the knowledge base system. Contains metadata and processing
 * information for documents.
 *
 * @since 1.0.0.3
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Document implements Serializable {

	/** Id for the document */
	@JsonProperty("doc_id")
	private String docId;

	/** Knowledge base id this document belongs to */
	@JsonProperty("kb_id")
	private String kbId;

	/** Type of the document */
	private DocumentType type;

	/** Current status of the document */
	private CommonStatus status;

	/** Name of the document */
	private String name;

	/** File format of the document */
	private String format;

	/** Size of the document in bytes */
	private Long size;

	/** Additional metadata for the document */
	private Metadata metadata;

	/** Whether the document is enabled */
	@Builder.Default
	private Boolean enabled = true;

	/** Current indexing status of the document */
	@JsonProperty("index_status")
	private DocumentIndexStatus indexStatus;

	/** Original path of the document */
	private String path;

	/** Path after document parsing */
	@JsonProperty("parsed_path")
	private String parsedPath;

	/** Configuration for document processing */
	@JsonProperty("process_config")
	private ProcessConfig processConfig;

	/** Error message if any processing failed */
	private String error;

	/** Creation timestamp */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

	/** Creator of the document */
	private String creator;

	/** Last modifier of the document */
	private String modifier;

	/**
	 * Inner class containing document metadata information
	 */
	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Metadata implements Serializable {

		/** MIME type of the document content */
		@JsonProperty("content_type")
		private String contentType;

	}

}
