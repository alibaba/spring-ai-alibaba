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

package com.alibaba.cloud.ai.example.deepresearch.util;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.Collections;

/**
 * Utility class for registering parallel node step titles into the state.
 */
public class NodeStepTitleUtil {

	/**
	 * Registers the step title into the state and returns the nodeNum (i.e., prefix + "_"
	 * + executorNodeId).
	 * @param state The global state
	 * @param isReflectionNode Whether this is a reflection node
	 * @param executorNodeId The node ID
	 * @param nodeType The node type (e.g., "Coder", "Researcher")
	 * @param stepTitle The step title
	 * @param prefix The node prefix
	 * @return nodeNum (can be used as the startingNode for StreamingChatGenerator)
	 */
	public static String registerStepTitle(OverAllState state, boolean isReflectionNode, String executorNodeId,
			String nodeType, String stepTitle, String prefix) {
		String nodeNum = prefix + "_" + executorNodeId;
		String stepTitleKey = nodeNum + "_step_title";
		String stepTitleValue = (isReflectionNode ? "[反思]" : "") + "[并行节点_" + nodeType + "_" + executorNodeId + "]"
				+ stepTitle;
		state.registerKeyAndStrategy(stepTitleKey, new ReplaceStrategy());
		state.input(Collections.singletonMap(stepTitleKey, stepTitleValue));
		return nodeNum;
	}

}
