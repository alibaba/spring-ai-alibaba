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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Title: CreateDate: 2025/4/24 14:41
 *
 * @author guning.lt
 * @since 1.0.0.3
 **/

@Data
@TableName("application_component")
public class AppComponentEntity {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("gmt_create")
	private Date gmtCreate;

	@TableField("gmt_modified")
	private Date gmtModified;

	private String code;

	private String name;

	@TableField("workspace_id")
	private String workspaceId;

	private String type;

	@TableField("app_id")
	private String appId;

	private String config;

	private String description;

	private Integer status;

	private String creator;

	private String modifier;

	@TableField("need_update")
	private Integer needUpdate;

}
