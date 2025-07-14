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

import com.alibaba.cloud.ai.service.executor.ContainerPoolExecutor;
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

	public PythonExecutorTool(ContainerPoolExecutor containerPoolExecutor) {
		this.containerPoolExecutor = containerPoolExecutor;
	}

	@Tool(description = "Execute Python code and return the result. You **need to provide** the correct Python code and its standard input."
			+ "Stdin may be null if there's not necessary. "
			+ "If a third-party dependency is required, please follow the `requirements.txt` schema for pip. "
			+ "Otherwise, the corresponding field will be set to null.")
	public String executePythonCode(@ToolParam(description = "python code") String code,
			@ToolParam(description = "requirements.txt", required = false) String requirements,
			@ToolParam(description = "stdin for the python script", required = false) String data) {
		if (code == null || code.trim().isEmpty()) {
			return "Error: Code must be a non-empty string.";
		}
		return this.containerPoolExecutor.runTask(new ContainerPoolExecutor.TaskRequest(code, data, requirements))
			.output();
	}

}
