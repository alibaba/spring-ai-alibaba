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

import com.alibaba.cloud.ai.studio.runtime.enums.AppStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.AppType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Entity class representing an application in the system.
 *
 * @since 1.0.0.3
 */
@Data
@TableName("application")
public class AppEntity {

	/** Primary key */
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	/** Workspace identifier */
	@TableField("workspace_id")
	private String workspaceId;

	/** Application identifier */
	@TableField("app_id")
	private String appId;

	/** Application type */
	private AppType type;

	/** Application status */
	private AppStatus status;

	/** Application name */
	private String name;

	/** Application description */
	private String description;

	/** Application icon */
	private String icon;

	/** Application source */
	private String source;

	/** Creation timestamp */
	@TableField("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@TableField("gmt_modified")
	private Date gmtModified;

	/** Creator of the application */
	private String creator;

	/** Last modifier of the application */
	private String modifier;

	/** Latest version of the application */
	@TableField(exist = false)
	private AppVersionEntity latestVersion;

	/** Published version of the application */
	@TableField(exist = false)
	private AppVersionEntity publishedVersion;

}
