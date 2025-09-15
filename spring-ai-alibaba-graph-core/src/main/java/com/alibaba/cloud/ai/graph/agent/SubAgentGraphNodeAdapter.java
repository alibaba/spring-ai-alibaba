/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

public class SubAgentGraphNodeAdapter implements NodeAction {

	private List<String> inputKeysFromParent;

	private String outputKeyToParent;

	private String inputKeyToChild;

	// This graph can be ReactAgent graph or Embedded FlowAgent graph.
	private CompiledGraph childGraph;

	public SubAgentGraphNodeAdapter(List<String> inputKeyFromParent, String outputKeyToParent,
			CompiledGraph childGraph) {
		this.inputKeysFromParent = inputKeyFromParent;
		this.outputKeyToParent = outputKeyToParent;
		this.childGraph = childGraph;
	}

	@Override
	public Map<String, Object> apply(OverAllState parentState) throws Exception {
		inputKeysFromParent
		String input = (String) parentState.value(inputKeyFromParent).orElseThrow();
		Message message = new UserMessage(input);
		List<Message> messages = List.of(message);

		Flux<GraphResponse<NodeOutput>> subGraphFlux = childGraph.fluxDataStream(Map.of("messages", messages),
				RunnableConfig.builder().build());

		return Map.of(outputKeyToParent, subGraphFlux);
	}

}
