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
import com.alibaba.cloud.ai.studio.runtime.enums.DocumentIndexStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.DocumentType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Document entity class representing a document in the system.
 *
 * @since 1.0.0.3
 */

@Data
@TableName("document")
public class DocumentEntity {

	/** Primary key */
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	/** Workspace identifier */
	@TableField("workspace_id")
	private String workspaceId;

	/** Knowledge base identifier */
	@TableField("kb_id")
	private String kbId;

	/** Document identifier */
	@TableField("doc_id")
	private String docId;

	/** Document status */
	private CommonStatus status;

	/** Document type */
	private DocumentType type;

	/** Whether the document is enabled */
	private Boolean enabled;

	/** Document name */
	private String name;

	/** Document format */
	private String format;

	/** Document size in bytes */
	private Long size;

	/** Document metadata in JSON format */
	private String metadata;

	/** Document indexing status */
	@TableField("index_status")
	private DocumentIndexStatus indexStatus;

	/** Document storage path */
	private String path;

	/** Path to parsed document content */
	@TableField("parsed_path")
	private String parsedPath;

	/** Document processing configuration */
	@TableField("process_config")
	private String processConfig;

	/** Document source information */
	private String source;

	/** Error message if any */
	private String error;

	/** Creation timestamp */
	@TableField("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@TableField("gmt_modified")
	private Date gmtModified;

	/** Creator's identifier */
	private String creator;

	/** Last modifier's identifier */
	private String modifier;

}
