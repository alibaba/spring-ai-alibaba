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
import com.alibaba.cloud.ai.example.manus.dynamic.cron.service.CronService;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.vo.CronConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;

public class CronTool extends AbstractBaseTool<CronTool.CronToolInput> {

	private static final Logger log = LoggerFactory.getLogger(CronTool.class);

	private final CronService cronService;

	public CronTool(CronService cronService) {
		this.cronService = cronService;
	}

	public static class CronToolInput {

		private String cronName; // 任务名称

		private String cronTime; // 定时时间

		private String planDesc; // 计划描述

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

	private static String PARAMETERS = """
			{
				"type": "object",
				"properties": {
					"cronName": {
						"type": "string",
						"description": "定时任务名称"
					},
					"cronTime": {
						"type": "string",
						"description": "cron格式的任务定时执行的时间(六位)，例如：0 0 0/2 * * ?"
					},
					"planDesc": {
						"type": "string",
						"description": "要执行的任务内容，不能包含时间相关信息"
					}
				},
				"required": ["cronTime","originTime","planDesc"]
			}
			""";

	private final String name = "cron_tool";

	private final String description = """
			    定时任务工具，能存储定时任务到db中。
			""";

	public OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public ToolExecuteResult run(CronToolInput input) {
		try {
			log.info("cron input:{}", objectMapper.writeValueAsString(input));

			// 创建CronConfig对象
			CronConfig cronConfig = new CronConfig();
			cronConfig.setCronName(input.getCronName());
			cronConfig.setCronTime(input.getCronTime());
			cronConfig.setPlanDesc(input.getPlanDesc());
			cronConfig.setStatus(0);

			// 保存到数据库
			CronConfig savedConfig = cronService.createCronTask(cronConfig);

			String result = String.format("OK 写入定时任务成功，任务ID: %d, 描述: %s, 定时时间: %s", savedConfig.getId(),
					savedConfig.getPlanDesc(), savedConfig.getCronTime());
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
		return description;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
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
				写入定时任务状态： %s
				""", "yes");
	}

	@Override
	public void cleanup(String planId) {
		log.info("Cleaned up resources for plan: {}", planId);
	}

}
