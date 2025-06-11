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

package com.alibaba.cloud.ai.example.graph.plugin;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.PluginNode;
import com.alibaba.cloud.ai.graph.plugin.weather.WeatherPlugin;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class PluginAutoConfiguration {

	@Bean
	public StateGraph pluginGraph() throws GraphStateException {

		OverAllStateFactory stateFactory = () -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("weather_params", new ReplaceStrategy());
			state.registerKeyAndStrategy("plugin_result", new ReplaceStrategy());
			return state;
		};

		// 创建一个插件节点，使用 WeatherPlugin 插件
		PluginNode pluginNode = PluginNode.builder()
			.plugin(new WeatherPlugin()) // 使用 WeatherPlugin 插件
			.paramsKey("weather_params") // 输入参数键
			.outputKey("weather_result") // 输出参数键
			.build();

		StateGraph stateGraph = new StateGraph(stateFactory).addNode("plugin_node", node_async(pluginNode))
			.addEdge(START, "plugin_node")
			.addEdge("plugin_node", END);

		GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
				"plugin graph");

		System.out.println("\n\n");
		System.out.println(graphRepresentation.content());
		System.out.println("\n\n");

		return stateGraph;
	}

}
