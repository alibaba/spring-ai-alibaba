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
package com.alibaba.cloud.ai.manus.agent.model.enums;

/**
 * Agent enumeration, defining all built-in agents and their configurations
 */
public enum AgentEnum {

	DEFAULT_AGENT("DEFAULT_AGENT", "default_agent"), BROWSER_AGENT("BROWSER_AGENT", "browser_agent"),
	DATABASE_AGENT("DATABASE_AGENT", "database_agent"), TEXT_FILE_AGENT("TEXT_FILE_AGENT", "text_file_agent"),
	CRON_AGENT("CRON_AGENT", "cron_agent"),
	MAPREDUCE_DATA_PREPARE_AGENT("MAPREDUCE_DATA_PREPARE_AGENT", "mapreduce_data_prepare_agent"),
	MAPREDUCE_FIN_AGENT("MAPREDUCE_FIN_AGENT", "mapreduce_fin_agent"),
	MAPREDUCE_MAP_TASK_AGENT("MAPREDUCE_MAP_TASK_AGENT", "mapreduce_map_task_agent"),
	MAPREDUCE_REDUCE_TASK_AGENT("MAPREDUCE_REDUCE_TASK_AGENT", "mapreduce_reduce_task_agent"),
	PPT_GENERATOR_AGENT("PPT_GENERATOR_AGENT", "ppt_generator_agent"),
	JSX_GENERATOR_AGENT("JSX_GENERATOR_AGENT", "jsx_generator_agent"),
	INTELLIGENT_FORM_AGENT("INTELLIGENT_FORM_AGENT", "intelligent_form_agent"),
	FILE_MANAGER_AGENT("FILE_MANAGER_AGENT", "file_manager_agent");

	private String agentName;

	private String agentPath;

	public static final String[] SUPPORTED_LANGUAGES = { "zh", "en" };

	AgentEnum(String agentName, String agentPath) {
		this.agentName = agentName;
		this.agentPath = agentPath;
	}

	public static String[] getSupportedLanguages() {
		return SUPPORTED_LANGUAGES.clone();
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getAgentPath() {
		return agentPath;
	}

	public void setAgentPath(String agentPath) {
		this.agentPath = agentPath;
	}

}
