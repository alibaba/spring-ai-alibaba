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
package com.alibaba.cloud.ai.studio.core.workflow;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.CommonParam;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Workflow configuration information
 *
 *
 */
@Data
public class WorkflowConfig implements Serializable {

	/**
	 * List of workflow nodes
	 */
	private List<Node> nodes;

	/**
	 * List of workflow edges
	 */
	private List<Edge> edges;

	/**
	 * Global configuration settings
	 */
	@JsonProperty("global_config")
	private GlobalConfig globalConfig;

	@Data
	public static class GlobalConfig implements Serializable {

		@JsonProperty("history_config")
		private HistoryConfig historyConfig;

		@JsonProperty("variable_config")
		private VariableConfig variableConfig;

	}

	/**
	 * Configuration for workflow variables
	 */
	@Data
	public static class VariableConfig implements Serializable {

		/**
		 * Conversation parameters shared within the session
		 */
		@JsonProperty("conversation_params")
		private List<CommonParam> conversationParams;

	}

	/**
	 * Configuration for conversation history
	 */
	@Data
	public static class HistoryConfig implements Serializable {

		/**
		 * Toggle for conversation history
		 */
		@JsonProperty("history_switch")
		private Boolean historySwitch = false;

		/**
		 * Maximum number of conversation rounds, defaults to 5
		 */
		@JsonProperty("history_max_round")
		private Integer historyMaxRound = 5;

	}

}
