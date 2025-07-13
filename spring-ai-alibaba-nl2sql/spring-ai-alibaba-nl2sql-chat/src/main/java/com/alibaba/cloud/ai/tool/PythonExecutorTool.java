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

package com.alibaba.cloud.ai.tool;

import com.alibaba.cloud.ai.config.ContainerProperties;
import com.alibaba.cloud.ai.service.container.ContainerPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * @author vlsmb
 * @since 2025/7/14
 */
public class PythonExecutorTool {

	private static final Logger log = LoggerFactory.getLogger(PythonExecutorTool.class);

	private final ContainerPoolExecutor containerPoolExecutor;

	public PythonExecutorTool(ContainerProperties properties) {
		this.containerPoolExecutor = ContainerPoolExecutor.getInstance(properties);
	}

	@Tool(description = "Execute Python code and return the result.")
	public String executePythonCode(@ToolParam(description = "python code") String code,
			@ToolParam(description = "requirements.txt", required = false) String requirements,
			@ToolParam(description = "input data for the python script", required = false) String data) {
		if (code == null || code.trim().isEmpty()) {
			return "Error: Code must be a non-empty string.";
		}
		return this.containerPoolExecutor.runTask(new ContainerPoolExecutor.TaskRequest(code, data, requirements))
			.output();
	}

}
