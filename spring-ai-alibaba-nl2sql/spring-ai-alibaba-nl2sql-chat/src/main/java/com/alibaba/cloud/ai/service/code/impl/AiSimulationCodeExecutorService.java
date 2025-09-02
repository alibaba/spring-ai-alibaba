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

package com.alibaba.cloud.ai.service.code.impl;

import com.alibaba.cloud.ai.service.code.CodePoolExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 使用AI模拟运行Python代码（便于在无Docker环境测试）
 *
 * @author vlsmb
 * @since 2025/7/30
 */
public class AiSimulationCodeExecutorService implements CodePoolExecutorService {

	private static final Logger log = LoggerFactory.getLogger(AiSimulationCodeExecutorService.class);

	private static final String SYSTEM_PROMPT = """
			你将模拟Python的执行，根据我提供的代码和输入数据，并给出最终的数据结果。
			在模拟运行时，请按照以下要求操作：
			1. 仔细理解代码和输入数据的内容。
			2. 输出模拟运行结果。
			**要求**：仅输出模拟运行结果，禁止包含任何额外说明或自然语言。
			""";

	private final ChatClient chatClient;

	public AiSimulationCodeExecutorService(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
	}

	@Override
	public TaskResponse runTask(TaskRequest request) {
		String userPrompt = String.format("""
				【代码】
				```python
				%s
				```
				【标准输入】
				```json
				%s
				```
				""", request.code(), request.input());
		String output = chatClient.prompt().user(userPrompt).call().content();
		return TaskResponse.success(output);
	}

}
