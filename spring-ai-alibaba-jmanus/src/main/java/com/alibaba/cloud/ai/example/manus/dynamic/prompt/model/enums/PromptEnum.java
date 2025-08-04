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
package com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.enums;

import com.alibaba.cloud.ai.example.manus.prompt.PromptDescriptionLoader;
import org.springframework.ai.chat.messages.MessageType;

public enum PromptEnum {

	PLANNING_PLAN_CREATION("PLANNING_PLAN_CREATION", MessageType.SYSTEM, PromptType.PLANNING, true,
			"planning/plan-creation.txt"),
	AGENT_CURRENT_STEP_ENV("AGENT_CURRENT_STEP_ENV", MessageType.USER, PromptType.AGENT, true,
			"agent/current-step-env.txt"),
	AGENT_STEP_EXECUTION("AGENT_STEP_EXECUTION", MessageType.USER, PromptType.AGENT, true, "agent/step-execution.txt"),
	PLANNING_PLAN_FINALIZER("PLANNING_PLAN_FINALIZER", MessageType.USER, PromptType.PLANNING, true,
			"planning/plan-finalizer.txt"),
	DIRECT_RESPONSE("DIRECT_RESPONSE", MessageType.USER, PromptType.PLANNING, true, "planning/direct-response.txt"),
	AGENT_STUCK_ERROR("AGENT_STUCK_ERROR", MessageType.SYSTEM, PromptType.AGENT, true, "agent/stuck-error.txt"),
	SUMMARY_PLAN_TEMPLATE("SUMMARY_PLAN_TEMPLATE", MessageType.SYSTEM, PromptType.PLANNING, true,
			"workflow/summary-plan-template.txt"),
	MAPREDUCE_TOOL_DESCRIPTION("MAPREDUCE_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.AGENT, true,
			"tool/mapreduce-tool-description.txt"),
	MAPREDUCE_TOOL_PARAMETERS("MAPREDUCE_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.AGENT, true,
			"tool/mapreduce-tool-parameters.txt"),
	AGENT_DEBUG_DETAIL_OUTPUT("AGENT_DEBUG_DETAIL_OUTPUT", MessageType.SYSTEM, PromptType.AGENT, true,
			"agent/debug-detail-output.txt"),
	AGENT_NORMAL_OUTPUT("AGENT_NORMAL_OUTPUT", MessageType.SYSTEM, PromptType.AGENT, true, "agent/normal-output.txt"),
	AGENT_PARALLEL_TOOL_CALLS_RESPONSE("AGENT_PARALLEL_TOOL_CALLS_RESPONSE", MessageType.SYSTEM, PromptType.AGENT, true,
			"agent/parallel-tool-calls-response.txt"),
	FORM_INPUT_TOOL_DESCRIPTION("FORM_INPUT_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.AGENT, true,
			"tool/form-input-tool-description.txt"),
	FORM_INPUT_TOOL_PARAMETERS("FORM_INPUT_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.AGENT, true,
			"tool/form-input-tool-parameters.txt"),

	// Bash Tool
	BASH_TOOL_DESCRIPTION("BASH_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.TOOL_DESCRIPTION, true,
			"tool/bash-tool-description.txt"),
	BASH_TOOL_PARAMETERS("BASH_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER, true,
			"tool/bash-tool-parameters.txt"),

	// Text File Operator Tool
	TEXTFILEOPERATOR_TOOL_DESCRIPTION("TEXTFILEOPERATOR_TOOL_DESCRIPTION", MessageType.SYSTEM,
			PromptType.TOOL_DESCRIPTION, true, "tool/textfileoperator-tool-description.txt"),
	TEXTFILEOPERATOR_TOOL_PARAMETERS("TEXTFILEOPERATOR_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER,
			true, "tool/textfileoperator-tool-parameters.txt"),

	// Browser Use Tool
	BROWSER_USE_TOOL_DESCRIPTION("BROWSER_USE_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.TOOL_DESCRIPTION, true,
			"tool/browser-use-tool-description.txt"),
	BROWSER_USE_TOOL_PARAMETERS("BROWSER_USE_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER, true,
			"tool/browser-use-tool-parameters.txt"),

	// Python Execute Tool
	PYTHON_EXECUTE_TOOL_DESCRIPTION("PYTHON_EXECUTE_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.TOOL_DESCRIPTION,
			true, "tool/python-execute-tool-description.txt"),
	PYTHON_EXECUTE_TOOL_PARAMETERS("PYTHON_EXECUTE_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER,
			true, "tool/python-execute-tool-parameters.txt"),

	// Database Use Tool
	DATABASE_USE_TOOL_DESCRIPTION("DATABASE_USE_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.TOOL_DESCRIPTION,
			true, "tool/database-use-tool-description.txt"),
	DATABASE_USE_TOOL_PARAMETERS("DATABASE_USE_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER, true,
			"tool/database-use-tool-parameters.txt"),

	// Cron Tool
	CRON_TOOL_TOOL_DESCRIPTION("CRON_TOOL_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.TOOL_DESCRIPTION, true,
			"tool/cron-tool-tool-description.txt"),
	CRON_TOOL_TOOL_PARAMETERS("CRON_TOOL_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER, true,
			"tool/cron-tool-tool-parameters.txt"),

	// Inner Storage Content Tool
	INNER_STORAGE_CONTENT_TOOL_TOOL_DESCRIPTION("INNER_STORAGE_CONTENT_TOOL_TOOL_DESCRIPTION", MessageType.SYSTEM,
			PromptType.TOOL_DESCRIPTION, true, "tool/inner-storage-content-tool-tool-description.txt"),
	INNER_STORAGE_CONTENT_TOOL_TOOL_PARAMETERS("INNER_STORAGE_CONTENT_TOOL_TOOL_PARAMETERS", MessageType.SYSTEM,
			PromptType.TOOL_PARAMETER, true, "tool/inner-storage-content-tool-tool-parameters.txt"),

	// Doc Loader Tool
	DOC_LOADER_TOOL_DESCRIPTION("DOC_LOADER_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.TOOL_DESCRIPTION, true,
			"tool/doc-loader-tool-description.txt"),
	DOC_LOADER_TOOL_PARAMETERS("DOC_LOADER_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER, true,
			"tool/doc-loader-tool-parameters.txt"),

	// File Merge Tool
	FILE_MERGE_TOOL_TOOL_DESCRIPTION("FILE_MERGE_TOOL_TOOL_DESCRIPTION", MessageType.SYSTEM,
			PromptType.TOOL_DESCRIPTION, true, "tool/file-merge-tool-description.txt"),
	FILE_MERGE_TOOL_TOOL_PARAMETERS("FILE_MERGE_TOOL_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER,
			true, "tool/file-merge-tool-parameters.txt"),

	// Data Split Tool
	DATA_SPLIT_TOOL_DESCRIPTION("DATA_SPLIT_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.TOOL_DESCRIPTION, true,
			"tool/data-split-tool-description.txt"),
	DATA_SPLIT_TOOL_PARAMETERS("DATA_SPLIT_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER, true,
			"tool/data-split-tool-parameters.txt"),

	// Map Output Tool
	MAP_OUTPUT_TOOL_DESCRIPTION("MAP_OUTPUT_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.TOOL_DESCRIPTION, true,
			"tool/map-output-tool-description.txt"),
	MAP_OUTPUT_TOOL_PARAMETERS("MAP_OUTPUT_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER, true,
			"tool/map-output-tool-parameters.txt"),

	// Reduce Operation Tool
	REDUCE_OPERATION_TOOL_DESCRIPTION("REDUCE_OPERATION_TOOL_DESCRIPTION", MessageType.SYSTEM,
			PromptType.TOOL_DESCRIPTION, true, "tool/reduce-operation-tool-description.txt"),
	REDUCE_OPERATION_TOOL_PARAMETERS("REDUCE_OPERATION_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER,
			true, "tool/reduce-operation-tool-parameters.txt"),

	// Finalize Tool
	FINALIZE_TOOL_DESCRIPTION("FINALIZE_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.TOOL_DESCRIPTION, true,
			"tool/finalize-tool-description.txt"),
	FINALIZE_TOOL_PARAMETERS("FINALIZE_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER, true,
			"tool/finalize-tool-parameters.txt"),

	// Terminate Tool
	TERMINATE_TOOL_DESCRIPTION("TERMINATE_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.TOOL_DESCRIPTION, true,
			"tool/terminate-tool-description.txt"),
	TERMINATE_TOOL_PARAMETERS("TERMINATE_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.TOOL_PARAMETER, true,
			"tool/terminate-tool-parameters.txt"),

	// PPT Generator Tool
	PPTGENERATOROPERATOR_TOOL_DESCRIPTION("PPTGENERATOROPERATOR_TOOL_DESCRIPTION", MessageType.SYSTEM,
			PromptType.TOOL_DESCRIPTION, true, "tool/ppt-generator-operator-tool-description.txt"),
	PPTGENERATOROPERATOR_TOOL_PARAMETERS("PPTGENERATOROPERATOR_TOOL_PARAMETERS", MessageType.SYSTEM,
			PromptType.TOOL_PARAMETER, true, "tool/ppt-generator-operator-tool-parameters.txt");

	private String promptName;

	private MessageType messageType;

	private PromptType type;

	private Boolean builtIn;

	private String promptPath;

	public static final String[] SUPPORTED_LANGUAGES = { "zh", "en" };

	private static PromptDescriptionLoader descriptionLoader;

	PromptEnum(String promptName, MessageType messageType, PromptType type, Boolean builtIn, String promptPath) {
		this.promptName = promptName;
		this.messageType = messageType;
		this.type = type;
		this.builtIn = builtIn;
		this.promptPath = promptPath;
	}

	public String getPromptPathForLanguage(String language) {
		if (language == null || language.trim().isEmpty()) {
			language = "en"; // Default to English
		}
		return language + "/" + this.promptPath;
	}

	public String getPromptDescriptionForLanguage(String language) {
		if (descriptionLoader == null) {
			// Fallback to empty string if loader is not initialized
			return "";
		}
		return descriptionLoader.loadDescription(this.promptName, language);
	}

	public static String[] getSupportedLanguages() {
		return SUPPORTED_LANGUAGES.clone();
	}

	/**
	 * Set the description loader for loading descriptions from files
	 * @param loader the PromptDescriptionLoader instance
	 */
	public static void setDescriptionLoader(PromptDescriptionLoader loader) {
		descriptionLoader = loader;
	}

	public String getPromptName() {
		return promptName;
	}

	public void setPromptName(String promptName) {
		this.promptName = promptName;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public Boolean getBuiltIn() {
		return builtIn;
	}

	public void setBuiltIn(Boolean builtIn) {
		this.builtIn = builtIn;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public PromptType getType() {
		return type;
	}

	public void setType(PromptType type) {
		this.type = type;
	}

	public String getPromptPath() {
		return promptPath;
	}

	public void setPromptPath(String promptPath) {
		this.promptPath = promptPath;
	}

	public boolean isBuiltIn() {
		return builtIn;
	}

	public void setBuiltIn(boolean builtIn) {
		this.builtIn = builtIn;
	}

}
