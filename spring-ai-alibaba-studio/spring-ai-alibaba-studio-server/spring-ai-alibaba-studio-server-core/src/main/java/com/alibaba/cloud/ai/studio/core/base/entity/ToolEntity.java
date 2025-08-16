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

import com.alibaba.cloud.ai.studio.runtime.enums.ToolStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ToolTestStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Entity class representing a tool in the system.
 *
 * @since 1.0.0.3
 */
@Data
@TableName("tool")
public class ToolEntity {

	/** Primary key */
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	/** Unique identifier for the tool */
	@TableField("tool_id")
	private String toolId;

	/** Associated plugin identifier */
	@TableField("plugin_id")
	private String pluginId;

	/** Workspace identifier where the tool belongs */
	@TableField("workspace_id")
	private String workspaceId;

	/** Current status of the tool */
	private ToolStatus status;

	/** Whether the tool is enabled */
	private Boolean enabled;

	/** Test status of the tool */
	private ToolTestStatus testStatus;

	/** Name of the tool */
	private String name;

	/** Description of the tool */
	private String description;

	/** Configuration settings for the tool */
	private String config;

	/** API schema definition */
	@TableField("api_schema")
	private String apiSchema;

	/** Creation timestamp */
	@TableField("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@TableField("gmt_modified")
	private Date gmtModified;

	/** Creator of the tool */
	private String creator;

	/** Last modifier of the tool */
	private String modifier;

}
