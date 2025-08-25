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
import com.alibaba.cloud.ai.studio.runtime.enums.KnowledgeBaseType;
import com.alibaba.cloud.ai.studio.runtime.domain.app.FileSearchOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a knowledge base entity that stores and manages knowledge resources.
 *
 * @since 1.0.0.3
 */
@Data
public class KnowledgeBase implements Serializable {

	/** Unique identifier of the knowledge base */
	@JsonProperty("kb_id")
	private String kbId;

	/** Type of the knowledge base */
	private KnowledgeBaseType type;

	/** Current status of the knowledge base */
	private CommonStatus status;

	/** Name of the knowledge base */
	private String name;

	/** Description of the knowledge base */
	private String description;

	/** Configuration for document processing */
	@JsonProperty("process_config")
	private ProcessConfig processConfig;

	/** Configuration for indexing */
	@JsonProperty("index_config")
	private IndexConfig indexConfig;

	/** Configuration for file search */
	@JsonProperty("search_config")
	private FileSearchOptions searchConfig;

	/** Total number of documents in the knowledge base */
	@JsonProperty("total_docs")
	private Long totalDocs = 0L;

	/** Creation timestamp */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

	/** Creator of the knowledge base */
	private String creator;

	/** Last modifier of the knowledge base */
	private String modifier;

	/** ID of the workspace this knowledge base belongs to */
	@JsonProperty("workspace_id")
	private String workspaceId;

}
