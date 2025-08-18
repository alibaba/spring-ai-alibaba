/*
 * Copyright 2024-2025 the original author or authors.
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

@Data
@TableName("provider")
public class ProviderEntity {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("workspace_id")
	private String workspaceId;

	private String icon;

	private String name;

	private String description;

	private String provider;

	private Boolean enable;

	/**
	 * 协议，默认openai协议
	 */
	private String protocol = "openai";

	private String source;

	// 支持的模型类型列表，逗号分隔
	@TableField("supported_model_types")
	private String supportedModelTypes;

	private String credential;

	@TableField("gmt_create")
	private Date gmtCreate;

	@TableField("gmt_modified")
	private Date gmtModified;

	private String creator;

	private String modifier;

}
