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

import com.alibaba.cloud.ai.studio.runtime.enums.PluginStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.PluginType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Entity class representing a plugin in the system.
 *
 * @since 1.0.0.3
 */
@Data
@TableName("plugin")
public class PluginEntity {

	/** Primary key */
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	/** Workspace identifier */
	@TableField("workspace_id")
	private String workspaceId;

	/** Unique identifier for the plugin */
	@TableField("plugin_id")
	private String pluginId;

	/** Type of the plugin */
	private PluginType type;

	/** Current status of the plugin */
	private PluginStatus status;

	/** Name of the plugin */
	private String name;

	/** Description of the plugin */
	private String description;

	/** Configuration settings for the plugin */
	private String config;

	/** Source information of the plugin */
	private String source;

	/** Creation timestamp */
	@TableField("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@TableField("gmt_modified")
	private Date gmtModified;

	/** Creator of the plugin */
	private String creator;

	/** Last modifier of the plugin */
	private String modifier;

}
