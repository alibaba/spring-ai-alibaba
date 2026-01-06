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

package com.alibaba.cloud.ai.studio.core.base.entity;

import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.KnowledgeBaseType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Entity class representing a knowledge base in the system.
 *
 * @since 1.0.0.3
 */

@Data
@TableName("knowledge_base")
public class KnowledgeBaseEntity {

	/** Primary key ID */
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	/** Unique identifier for the knowledge base */
	@TableField("kb_id")
	private String kbId;

	/** ID of the workspace this knowledge base belongs to */
	@TableField("workspace_id")
	private String workspaceId;

	/** Type of the knowledge base */
	private KnowledgeBaseType type;

	/** Current status of the knowledge base */
	private CommonStatus status;

	/** Name of the knowledge base */
	private String name;

	/** Description of the knowledge base */
	private String description;

	/** Configuration for document processing */
	@TableField("process_config")
	private String processConfig;

	/** Configuration for indexing */
	@TableField("index_config")
	private String indexConfig;

	/** Configuration for search functionality */
	@TableField("search_config")
	private String searchConfig;

	/** Total number of documents in the knowledge base */
	@TableField("total_docs")
	private Long totalDocs;

	/** Creation timestamp */
	@TableField("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@TableField("gmt_modified")
	private Date gmtModified;

	/** Creator of the knowledge base */
	private String creator;

	/** Last modifier of the knowledge base */
	private String modifier;

}
