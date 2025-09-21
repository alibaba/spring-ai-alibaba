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

import com.alibaba.cloud.ai.studio.runtime.enums.agent.AgentStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.agent.AgentType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * Entity class representing an agent schema in the system.
 *
 * @since 1.0.0.3
 */
@Data
@TableName("agent_schema")
public class AgentSchemaEntity {

	/** Primary key */
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	/** Unique identifier for the agent */
	@TableField("agent_id")
	private String agentId;

	/** Workspace identifier where the agent belongs */
	@TableField("workspace_id")
	private String workspaceId;

	/** Name of the agent */
	private String name;

	/** Description of the agent */
	private String description;

	/** Type of the agent */
	private AgentType type;

	/** System instruction for the agent */
	@TableField("instruction")
	private String instruction;

	/** Input keys for the agent */
	@TableField("input_keys")
	@JsonProperty("inputKeys")
	private String inputKeys;

	/** Output key for the agent */
	@TableField("output_key")
	@JsonProperty("outputKey")
	private String outputKey;

	/** Handle configuration in JSON format */
	private String handle;

	/** Sub-agents configuration in JSON format */
	@TableField("sub_agents")
	@JsonProperty("subAgents")
	private String subAgents;

	/** Generated YAML schema */
	@TableField("yaml_schema")
	@JsonProperty("yamlSchema")
	private String yamlSchema;

	/** Current status of the agent */
	private AgentStatus status;

	/** Whether the agent is enabled */
	private Boolean enabled;

	/** Creation timestamp */
	@TableField("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@TableField("gmt_modified")
	private Date gmtModified;

	/** Creator of the agent */
	private String creator;

	/** Last modifier of the agent */
	private String modifier;

}
