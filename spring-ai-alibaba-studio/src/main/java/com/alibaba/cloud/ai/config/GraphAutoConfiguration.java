/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.GraphInitData;
import com.alibaba.cloud.ai.graph.InitDataSerializer;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.NodeOutputSerializer;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class GraphAutoConfiguration {

	// @Bean
	// public GraphInitData initData(ApplicationContext context) {
	// String title = "AGENT EXECUTOR";
	// Map<String, GraphInitData.ArgumentMetadata> inputArgs = new HashMap<>();
	// inputArgs.put("input", new GraphInitData.ArgumentMetadata("string", true));
	//
	// StateGraph stateGraph = context.getBean(StateGraph.class);
	// var graph = stateGraph.getGraph(GraphRepresentation.Type.MERMAID, title, false);
	// return new GraphInitData(title, graph.getContent(), inputArgs);
	// }

	@Bean
	public GraphInitData initData(StateGraph stateGraph) throws GraphStateException {
		String title = "Agent Executor";
		String name = "input";
		boolean required = true;

		List<GraphInitData.ArgumentMetadata> inputArgs = new ArrayList<>();
		inputArgs.add(
				new GraphInitData.ArgumentMetadata(name, GraphInitData.ArgumentMetadata.ArgumentType.STRING, required));

		CompiledGraph compiledGraph = stateGraph.compile();
		var graph = compiledGraph.getGraph(GraphRepresentation.Type.MERMAID, title, false);
		return new GraphInitData(title, graph.content(), inputArgs);
	}

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer customJackson() {
		return jackson2ObjectMapperBuilder -> jackson2ObjectMapperBuilder
			.modules(new SimpleModule().addSerializer(GraphInitData.class, new InitDataSerializer(GraphInitData.class))
				.addSerializer(NodeOutput.class, new NodeOutputSerializer()));
	}

}
