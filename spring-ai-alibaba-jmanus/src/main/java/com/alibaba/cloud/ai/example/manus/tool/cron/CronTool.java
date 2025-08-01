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
package com.alibaba.cloud.ai.example.manus.tool.cron;

import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.ToolPromptManager;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.service.CronService;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.vo.CronConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;

public class CronTool extends AbstractBaseTool<CronTool.CronToolInput> {

	private final ObjectMapper objectMapper;

	private final ToolPromptManager toolPromptManager;

	private static final Logger log = LoggerFactory.getLogger(CronTool.class);

	private final CronService cronService;

	public CronTool(CronService cronService, ObjectMapper objectMapper, ToolPromptManager toolPromptManager) {
		this.cronService = cronService;
		this.objectMapper = objectMapper;
		this.toolPromptManager = toolPromptManager;
	}

	public static class CronToolInput {

		private String cronName; // Task name

		private String cronTime; // Scheduled time

		private String planDesc; // Plan description

		CronToolInput() {
		}

		CronToolInput(String cronTime, String cronName, String planDesc) {
			this.cronTime = cronTime;
			this.cronName = cronName;
			this.planDesc = planDesc;
		}

		public String getCronTime() {
			return cronTime;
		}

		public String getPlanDesc() {
			return planDesc;
		}

		public void setCronTime(String cronTime) {
			this.cronTime = cronTime;
		}

		public void setPlanDesc(String planDesc) {
			this.planDesc = planDesc;
		}

		public String getCronName() {
			return cronName;
		}

		public void setCronName(String cronName) {
			this.cronName = cronName;
		}

	}

	private final String name = "cron_tool";

	@Override
	public ToolExecuteResult run(CronToolInput input) {
		try {
			log.info("cron input:{}", objectMapper.writeValueAsString(input));

			// Create CronConfig object
			CronConfig cronConfig = new CronConfig();
			cronConfig.setCronName(input.getCronName());
			cronConfig.setCronTime(input.getCronTime());
			cronConfig.setPlanDesc(input.getPlanDesc());
			cronConfig.setStatus(0);

			// Save to database
			CronConfig savedConfig = cronService.createCronTask(cronConfig);

			String result = String.format(
					"OK Scheduled task created successfully, Task ID: %d, Description: %s, Schedule time: %s",
					savedConfig.getId(), savedConfig.getPlanDesc(), savedConfig.getCronTime());
			return new ToolExecuteResult(objectMapper.writeValueAsString(result));
		}
		catch (Exception e) {
			log.error("Error executing cron", e);
			return new ToolExecuteResult("Error executing cron: " + e.getMessage());
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return toolPromptManager.getToolDescription("cron_tool");
	}

	@Override
	public String getParameters() {
		return toolPromptManager.getToolParameters("cron_tool");
	}

	@Override
	public Class<CronToolInput> getInputType() {
		return CronToolInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	@Override
	public String getCurrentToolStateString() {
		return String.format("""
				Write scheduled task status: %s
				""", "yes");
	}

	@Override
	public void cleanup(String planId) {
		log.info("Cleaned up resources for plan: {}", planId);
	}

}
