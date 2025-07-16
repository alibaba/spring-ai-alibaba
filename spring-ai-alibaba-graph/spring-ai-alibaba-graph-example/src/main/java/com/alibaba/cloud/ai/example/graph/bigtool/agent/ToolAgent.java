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

import com.alibaba.cloud.ai.example.graph.bigtool.service.VectorStoreService;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.example.graph.bigtool.constants.Constant.HIT_TOOL;
import static com.alibaba.cloud.ai.example.graph.bigtool.constants.Constant.TOOL_LIST;

public class ToolAgent implements NodeAction {

	private List<Document> documents;

	private ChatClient chatClient;

	private String inputTextKey;

	private String inputText;

	private VectorStoreService vectorStoreService;

	public ToolAgent(ChatClient chatClient, String inputTextKey, VectorStoreService vectorStoreService) {
		this.chatClient = chatClient;
		this.inputTextKey = inputTextKey;
		this.vectorStoreService = vectorStoreService;
	}

	public ToolAgent(ChatClient chatClient, String inputTextKey, List<Document> documents) {
		this.documents = documents;
		this.chatClient = chatClient;
		this.inputTextKey = inputTextKey;
	}

	private static final String CLASSIFIER_PROMPT_TEMPLATE = """
			### Job Description
			You are a text keyword extraction engine that can analyze the questions passed in by users and extract the main keywords of this sentence.
			### Task
			You need to extract one or more keywords from this sentence, without missing the main body of the user description
			### Constraint
			Multiple keywords returned, separated by spaces
			""";

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {

		if (documents == null) {
			this.documents = (List<Document>) state.value(TOOL_LIST).orElseThrow();
		}

		if (StringUtils.hasLength(inputTextKey)) {
			this.inputText = (String) state.value(inputTextKey).orElse(this.inputText);
		}

		ChatResponse response = chatClient.prompt()
			.system(CLASSIFIER_PROMPT_TEMPLATE)
			.user(inputText)
			.call()
			.chatResponse();

		List<Document> hitTool = vectorStoreService.search(response.getResult().getOutput().getText(), 3);

		Map<String, Object> updatedState = new HashMap<>();
		updatedState.put(HIT_TOOL, hitTool);
		if (state.value(inputTextKey).isPresent()) {
			updatedState.put(inputTextKey, response.getResult().getOutput().getText());
		}

		return updatedState;
	}

}
