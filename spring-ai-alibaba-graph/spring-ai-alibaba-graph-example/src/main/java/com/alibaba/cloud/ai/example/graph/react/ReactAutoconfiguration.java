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
package com.alibaba.cloud.ai.example.graph.react;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.example.graph.workflow.CustomerServiceController;
import com.alibaba.cloud.ai.example.graph.workflow.RecordingNode;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.node.QuestionClassifierNode;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class ReactAutoconfiguration {

	@Bean
	public ReactAgent normalReactAgent(ChatModel chatModel, ToolCallbackResolver resolver) throws GraphStateException {
		ChatClient chatClient = ChatClient.builder(chatModel)
			// .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.build();

		return new ReactAgent("React Agent Demo", "请帮助用户完成他接下来输入的任务规划。", chatClient, resolver, 10);
	}

	@Bean
	public CompiledGraph reactAgentGraph(@Qualifier("normalReactAgent") ReactAgent reactAgent)
			throws GraphStateException {
		GraphRepresentation graphRepresentation = reactAgent.getStateGraph()
			.getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println("\n\n");
		System.out.println(graphRepresentation.content());
		System.out.println("\n\n");

		return reactAgent.getAndCompileGraph();
	}

}
