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
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * Workspace entity representing a user's workspace in the system.
 *
 * @since 1.0.0.3
 */

@Data
@TableName("workspace")
public class WorkspaceEntity {

	/** Primary key */
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	/** Associated account identifier */
	@TableField("account_id")
	private String accountId;

	/** Unique workspace identifier */
	@TableField("workspace_id")
	private String workspaceId;

	/** Current status of the workspace */
	private CommonStatus status;

	/** Name of the workspace */
	private String name;

	/** Description of the workspace */
	private String description;

	/** Workspace configuration in JSON format */
	private String config;

	/** Creation timestamp */
	@TableField("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@TableField("gmt_modified")
	private Date gmtModified;

	/** Username of the creator */
	private String creator;

	/** Username of the last modifier */
	private String modifier;

}
