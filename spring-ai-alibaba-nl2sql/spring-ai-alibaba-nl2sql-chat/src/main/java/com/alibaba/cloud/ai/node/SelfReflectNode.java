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

package com.alibaba.cloud.ai.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.RESULT;

/**
 * 自反思节点，用于结果质量评估和优化建议
 *
 * @author zhangshenghang
 */
@Deprecated
public class SelfReflectNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(SelfReflectNode.class);

	private final ChatClient chatClient;

	public SelfReflectNode(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Entering {} node", this.getClass().getSimpleName());

		// This node currently has no specific logic implemented, reserved for future
		// extension
		// Can add result quality assessment, optimization suggestions and other features
		// here

		logger.info("Self-reflection node has no specific logic implemented yet, returning empty result directly");

		return Map.of(RESULT, "Self-reflection node execution completed");
	}

}
