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

package com.alibaba.cloud.ai.example.deepresearch.service;

import com.alibaba.cloud.ai.example.deepresearch.node.RagNode;
import com.alibaba.cloud.ai.example.deepresearch.rag.core.HybridRagProcessor;
import com.alibaba.cloud.ai.example.deepresearch.rag.strategy.FusionStrategy;
import com.alibaba.cloud.ai.example.deepresearch.rag.strategy.ProfessionalKbEsStrategy;
import com.alibaba.cloud.ai.example.deepresearch.rag.strategy.UserFileRetrievalStrategy;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * @author yingzi
 * @since 2025/8/10
 */
@Service
public class RagNodeService {

	@Autowired(required = false)
	private ChatClient ragAgent;

	@Autowired(required = false)
	private UserFileRetrievalStrategy userFileRetrievalStrategy;

	@Autowired(required = false)
	private ProfessionalKbEsStrategy professionalKbEsStrategy;

	@Autowired(required = false)
	private FusionStrategy fusionStrategy;

	@Autowired(required = false)
	private HybridRagProcessor hybridRagProcessor;

	/**
	 * 创建用户文件RAG节点，优先使用统一的HybridRagProcessor
	 */
	public AsyncNodeAction createUserFileRagNode() {
		if (hybridRagProcessor != null) {
			// 使用统一的RAG处理器，包含完整的前后处理和混合查询逻辑
			return node_async(new RagNode(hybridRagProcessor, ragAgent));
		}
		else {
			// 回退到传统的策略模式
			return node_async(
					new RagNode(userFileRetrievalStrategy != null ? List.of(userFileRetrievalStrategy) : List.of(),
							fusionStrategy, ragAgent));
		}
	}

	/**
	 * 创建专业知识库RAG节点，优先使用统一的HybridRagProcessor
	 */
	public AsyncNodeAction createProfessionalKbRagNode() {
		if (hybridRagProcessor != null) {
			// 使用统一的RAG处理器，包含完整的前后处理和混合查询逻辑
			return node_async(new RagNode(hybridRagProcessor, ragAgent));
		}
		else {
			// 回退到传统的策略模式
			return node_async(
					new RagNode(professionalKbEsStrategy != null ? List.of(professionalKbEsStrategy) : List.of(),
							fusionStrategy, ragAgent));
		}
	}

}
