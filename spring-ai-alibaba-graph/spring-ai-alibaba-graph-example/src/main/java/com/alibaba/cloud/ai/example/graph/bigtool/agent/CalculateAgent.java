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

package com.alibaba.cloud.ai.example.graph.bigtool.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import static com.alibaba.cloud.ai.example.graph.bigtool.constants.Constant.HIT_TOOL;
import static com.alibaba.cloud.ai.example.graph.bigtool.constants.Constant.METHOD_NAME;
import static com.alibaba.cloud.ai.example.graph.bigtool.constants.Constant.METHOD_PARAMETER_TYPES;
import static com.alibaba.cloud.ai.example.graph.bigtool.constants.Constant.SOLUTION;

public class CalculateAgent implements NodeAction {

	private List<Document> documents;

	private ChatClient chatClient;

	private String inputTextKey;

	private String inputText;

	public CalculateAgent(ChatClient chatClient, String inputTextKey) {
		this.chatClient = chatClient;
		this.inputTextKey = inputTextKey;
	}

	private static final String CLASSIFIER_PROMPT_TEMPLATE = """
			### Job Description
			Please use the tools to complete the task
			""";

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {

		if (documents == null) {
			this.documents = (List<Document>) state.value(HIT_TOOL).orElseThrow();
		}

		List<ToolCallback> toolCallbacks = new ArrayList<>();
		for (Document document : documents) {
			var toolMethod = ReflectionUtils.findMethod(Math.class, document.getMetadata().get(METHOD_NAME).toString(),
					(Class<?>[]) document.getMetadata().get(METHOD_PARAMETER_TYPES));

			DefaultToolDefinition.Builder toolDefinitionBuilder = DefaultToolDefinition.builder()
				.name(ToolUtils.getToolName(toolMethod))
				.description(ToolUtils.getToolDescription(toolMethod))
				.inputSchema(JsonSchemaGenerator.generateForMethodInput(toolMethod));

			MethodToolCallback build = MethodToolCallback.builder()
				.toolDefinition(toolDefinitionBuilder.build())
				.toolMethod(toolMethod)
				.build();

			toolCallbacks.add(build);
		}

		if (StringUtils.hasLength(inputTextKey)) {
			this.inputText = (String) state.value(inputTextKey).orElse(this.inputText);
		}

		ChatResponse response = chatClient.prompt()
			.system(CLASSIFIER_PROMPT_TEMPLATE)
			.user(inputText)
			.toolCallbacks(toolCallbacks)
			.call()
			.chatResponse();

		Map<String, Object> updatedState = new HashMap<>();
		updatedState.put(SOLUTION, response.getResult().getOutput().getText());
		return updatedState;
	}

}
