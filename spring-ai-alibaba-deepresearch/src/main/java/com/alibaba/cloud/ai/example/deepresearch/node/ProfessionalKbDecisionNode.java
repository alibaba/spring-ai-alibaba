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

package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.example.deepresearch.config.rag.RagProperties;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 专业知识库决策节点，根据查询内容和知识库描述智能判断是否需要查询专业知识库
 *
 * @author hupei
 */
public class ProfessionalKbDecisionNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ProfessionalKbDecisionNode.class);

	private final ChatClient chatClient;

	private final RagProperties ragProperties;

	public ProfessionalKbDecisionNode(ChatClient chatClient, RagProperties ragProperties) {
		this.chatClient = chatClient;
		this.ragProperties = ragProperties;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Professional KB decision node is running.");
		String query = StateUtil.getQuery(state);
		Map<String, Object> updated = new HashMap<>();

		// 如果没有启用专业知识库决策，直接返回不使用
		if (!ragProperties.getProfessionalKnowledgeBases().isDecisionEnabled()) {
			updated.put("use_professional_kb", false);
			updated.put("selected_knowledge_bases", Collections.emptyList());
			return updated;
		}

		// 获取已启用的专业知识库列表
		List<RagProperties.ProfessionalKnowledgeBases.KnowledgeBase> enabledKbs = ragProperties
			.getProfessionalKnowledgeBases()
			.getKnowledgeBases()
			.stream()
			.filter(RagProperties.ProfessionalKnowledgeBases.KnowledgeBase::isEnabled)
			.sorted(Comparator.comparingInt(RagProperties.ProfessionalKnowledgeBases.KnowledgeBase::getPriority))
			.collect(Collectors.toList());

		if (enabledKbs.isEmpty()) {
			logger.info("No enabled professional knowledge bases found.");
			updated.put("use_professional_kb", false);
			updated.put("selected_knowledge_bases", Collections.emptyList());
			return updated;
		}

		// 构建决策提示词
		String decisionPrompt = buildDecisionPrompt(query, enabledKbs);

		// 调用大模型进行决策
		ChatResponse response = chatClient.prompt().user(decisionPrompt).call().chatResponse();

		// 解析响应
		List<String> selectedKbIds = parseDecisionResponse(response, enabledKbs);

		boolean useKb = !selectedKbIds.isEmpty();
		updated.put("use_professional_kb", useKb);
		updated.put("selected_knowledge_bases", selectedKbIds);

		if (useKb) {
			logger.info("Decision: Use professional knowledge bases: {}", selectedKbIds);
		}
		else {
			logger.info("Decision: No professional knowledge base needed for this query.");
		}

		return updated;
	}

	/**
	 * 构建决策提示词
	 */
	private String buildDecisionPrompt(String query, List<RagProperties.ProfessionalKnowledgeBases.KnowledgeBase> kbs) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("请根据用户查询和可用的专业知识库，判断哪些知识库适合回答这个问题。\n\n");
		prompt.append("用户查询：").append(query).append("\n\n");
		prompt.append("可用的专业知识库：\n");

		for (int i = 0; i < kbs.size(); i++) {
			RagProperties.ProfessionalKnowledgeBases.KnowledgeBase kb = kbs.get(i);
			prompt.append(String.format("%d. [%s] %s\n", i + 1, kb.getId(), kb.getName()));
			prompt.append(String.format("   描述：%s\n", kb.getDescription()));
			prompt.append("\n");
		}

		prompt.append("请分析用户查询的内容，判断是否需要查询以上专业知识库。\n");
		prompt.append("如果需要，请返回格式：SELECTED: [知识库ID1, 知识库ID2, ...]\n");
		prompt.append("如果不需要，请返回：SELECTED: []\n");
		prompt.append("请只返回SELECTED行，不要包含其他内容。");

		return prompt.toString();
	}

	/**
	 * 解析大模型的决策响应
	 */
	private List<String> parseDecisionResponse(ChatResponse response,
			List<RagProperties.ProfessionalKnowledgeBases.KnowledgeBase> kbs) {
		if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
			return Collections.emptyList();
		}

		String responseText = response.getResult().getOutput().getText().trim();
		logger.debug("Decision response: {}", responseText);

		// 解析SELECTED格式的响应
		if (responseText.contains("SELECTED:")) {
			String selectedPart = responseText.substring(responseText.indexOf("SELECTED:") + 9).trim();
			// 移除方括号
			selectedPart = selectedPart.replaceAll("[\\[\\]]", "").trim();

			if (selectedPart.isEmpty()) {
				return Collections.emptyList();
			}

			// 分割知识库ID
			String[] kbIds = selectedPart.split(",");
			List<String> validKbIds = new ArrayList<>();
			Set<String> availableKbIds = kbs.stream()
				.map(RagProperties.ProfessionalKnowledgeBases.KnowledgeBase::getId)
				.collect(Collectors.toSet());

			for (String kbId : kbIds) {
				String trimmedId = kbId.trim();
				if (!trimmedId.isEmpty() && availableKbIds.contains(trimmedId)) {
					validKbIds.add(trimmedId);
				}
			}

			return validKbIds;
		}

		// 兜底策略：如果响应包含"yes"或"是"，选择第一个知识库
		if (responseText.toLowerCase().contains("yes") || responseText.contains("是")) {
			return kbs.isEmpty() ? Collections.emptyList() : Collections.singletonList(kbs.get(0).getId());
		}

		return Collections.emptyList();
	}

}
