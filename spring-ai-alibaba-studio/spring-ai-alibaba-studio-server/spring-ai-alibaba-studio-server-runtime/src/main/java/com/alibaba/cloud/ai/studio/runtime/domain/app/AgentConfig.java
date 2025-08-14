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

package com.alibaba.cloud.ai.studio.runtime.domain.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Configuration for an agent in the system.
 *
 * @since 1.0.0.3
 */
@Data
public class AgentConfig implements AppConfig, Serializable {

	/** Provider of the LLM/VLM model */
	@JsonProperty("model_provider")
	private String modelProvider;

	/** Name of the LLM/VLM model to use */
	private String model;

	/** Type of modality supported by the model */
	@JsonProperty("modality_type")
	private String modalityType;

	/** System instructions for the agent */
	private String instructions;

	/** Memory configuration for the agent */
	private Memory memory;

	/** Model parameters for generation */
	private Parameter parameter;

	/** List of tools available to the agent */
	private List<Tool> tools;

	/** List of MCP servers for the agent */
	@JsonProperty("mcp_servers")
	private List<McpServer> mcpServers;

	/** List of agent component identifiers */
	@JsonProperty("agent_components")
	private List<String> agentComponents;

	/** List of workflow component identifiers */
	@JsonProperty("workflow_components")
	private List<String> workflowComponents;

	/** List of variables used in prompts */
	@JsonProperty("prompt_variables")
	private List<PromptVariable> promptVariables;

	/** File search configuration options */
	@JsonProperty("file_search")
	private FileSearchOptions fileSearch;

	/** Initial context and suggested questions */
	private Prologue prologue;

	/** Configuration for model generation parameters */
	@Data
	public static class Parameter implements Serializable {

		/** Maximum number of tokens to generate */
		@JsonProperty("max_tokens")
		private Integer maxTokens;

		/** Temperature for controlling randomness */
		@JsonProperty("temperature")
		private Double temperature;

		/** Top-p sampling parameter */
		@JsonProperty("top_p")
		private Double topP;

		/** Penalty for repeated tokens */
		@JsonProperty("repetition_penalty")
		private Double repetitionPenalty;

	}

	/** Configuration for agent's memory */
	@Data
	public static class Memory implements Serializable {

		/** Number of dialog rounds to remember */
		@JsonProperty("dialog_round")
		private Integer dialogRound;

	}

	/** Configuration for agent's tools */
	@Data
	public static class Tool implements Serializable {

		/** Unique identifier for the tool */
		private String id;

		/** Type of the tool */
		private String type;

	}

	/** Configuration for MCP servers */
	@Data
	public static class McpServer implements Serializable {

		/** Unique identifier for the server */
		private String id;

		/** Type of the server */
		private String type;

	}

	/** Configuration for prompt variables */
	@Data
	public static class PromptVariable implements Serializable {

		/** Name of the variable */
		private String name;

		/** Type of the variable */
		private String type;

		/** Description of the variable's purpose */
		private String description;

		/** Default value for the variable */
		@JsonProperty("default_value")
		private String defaultValue;

	}

	/** Configuration for agent's prologue */
	@Data
	public static class Prologue implements Serializable {

		/** Initial text shown to users */
		@JsonProperty("prologue_text")
		private String prologueText;

		/** List of suggested questions for users */
		@JsonProperty("suggested_questions")
		private List<String> suggestedQuestions;

	}

}
