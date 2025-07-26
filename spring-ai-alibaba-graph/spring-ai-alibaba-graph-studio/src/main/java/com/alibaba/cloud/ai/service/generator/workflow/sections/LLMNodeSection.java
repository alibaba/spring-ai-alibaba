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

package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.LLMNodeData;
import com.alibaba.cloud.ai.model.workflow.nodedata.LLMNodeData.PromptTemplate;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LLMNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.LLM.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		LLMNodeData d = (LLMNodeData) node.getData();
		List<String> promptList = new ArrayList<>();

		List<PromptTemplate> promptTemplates = null;
		if (d.getPromptTemplate() != null) {
			promptTemplates = d.getPromptTemplate();
			for (PromptTemplate promptTemplate : promptTemplates) {
				if (promptTemplate.getRole() != null && promptTemplate.getText() != null) {
					promptList.add(transformPlaceholders(promptTemplate.getText()));
				}
			}
		}

		if (d.getSystemPromptTemplate() != null) {
			promptList.add(transformPlaceholders(d.getSystemPromptTemplate()));
		}
		if (d.getUserPromptTemplate() != null) {
			promptList.add(transformPlaceholders(d.getUserPromptTemplate()));
		}

		String id = node.getId();
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("// —— LlmNode [%s] ——%n", id));
		sb.append(String.format("LlmNode %s = LlmNode.builder()%n", varName));

		for (PromptTemplate promptTemplate : promptTemplates) {
			if (promptTemplate.getRole() != null && promptTemplate.getText() != null) {
				if (promptTemplate.getRole().equals("system")) {
					sb.append(String.format(".systemPromptTemplate(\"%s\")%n",
							escape(transformPlaceholders(promptTemplate.getText()))));
				}
				else if (promptTemplate.getRole().equals("user")) {
					sb.append(String.format(".userPromptTemplate(\"%s\")%n",
							escape(transformPlaceholders(promptTemplate.getText()))));
				}
			}
		}

		if (d.getSystemPromptTemplate() != null) {
			sb.append(String.format(".systemPromptTemplate(\"%s\")%n",
					escape(transformPlaceholders(d.getSystemPromptTemplate()))));
		}

		if (d.getUserPromptTemplate() != null) {
			sb.append(String.format(".userPromptTemplate(\"%s\")%n",
					escape(transformPlaceholders(d.getUserPromptTemplate()))));
		}

		if (d.getSystemPromptTemplateKey() != null) {
			sb.append(String.format(".systemPromptTemplateKey(\"%s\")%n", escape(d.getSystemPromptTemplateKey())));
		}

		if (d.getUserPromptTemplateKey() != null) {
			sb.append(String.format(".userPromptTemplateKey(\"%s\")%n", escape(d.getUserPromptTemplateKey())));
		}

		List<String> params = extractKeysFromList(promptList);
		if (!params.isEmpty()) {
			Map<String, String> paramMap = params.stream().distinct().collect(Collectors.toMap(k -> k, k -> ""));

			String joined = paramMap.entrySet()
				.stream()
				.map(e -> String.format("\"%s\", \"%s\"", escape(e.getKey()), "null"))
				.collect(Collectors.joining(", "));

			sb.append(String.format(".params(Map.of(%s))%n", joined));
		}

		if (d.getParamsKey() != null) {
			sb.append(String.format(".paramsKey(\"%s\")%n", escape(d.getParamsKey())));
		}

		List<?> messages = d.getMessages();
		if (messages != null && !messages.isEmpty()) {
			String joined = messages.stream()
				.map(Object::toString)
				.map(this::escape)
				.map(s -> "\"" + s + "\"")
				.collect(Collectors.joining(", "));
			sb.append(String.format(".messages(List.of(%s))%n", joined));
		}

		if (d.getMessagesKey() != null) {
			sb.append(String.format(".messagesKey(\"%s\")%n", escape(d.getMessagesKey())));
		}

		List<?> advisors = d.getAdvisors();
		if (advisors != null && !advisors.isEmpty()) {
			String joined = advisors.stream()
				.map(Object::toString)
				.map(this::escape)
				.map(s -> "\"" + s + "\"")
				.collect(Collectors.joining(", "));
			sb.append(String.format(".advisors(List.of(%s))%n", joined));
		}

		List<?> toolCallbacks = d.getToolCallbacks();
		if (toolCallbacks != null && !toolCallbacks.isEmpty()) {
			String joined = toolCallbacks.stream()
				.map(Object::toString)
				.map(this::escape)
				.map(s -> "\"" + s + "\"")
				.collect(Collectors.joining(", "));
			sb.append(String.format(".toolCallbacks(List.of(%s))%n", joined));
		}

		sb.append(".chatClient(chatClient)\n");

		if (d.getOutputKey() != null) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(d.getOutputKey())));
		}

		sb.append(".build();\n");
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", varName, varName));

		return sb.toString();
	}

	// Extract variable
	private static List<String> extractKeysFromList(List<String> inputList) {
		List<String> result = new ArrayList<>();
		Pattern pattern = Pattern.compile("\\{(\\w+)}");

		for (String input : inputList) {
			Matcher matcher = pattern.matcher(input);
			while (matcher.find()) {
				result.add(matcher.group(1));
			}
		}
		return result;
	}

	// Format prompt
	private static String transformPlaceholders(String input) {
		if (input == null)
			return null;

		Pattern pattern = Pattern.compile("\\{\\{#.*?\\.(.*?)#}}");
		Matcher matcher = pattern.matcher(input);

		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String key = matcher.group(1);
			matcher.appendReplacement(sb, "{" + key + "}");
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

}
