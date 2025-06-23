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

package com.alibaba.cloud.ai.example.graph.mcp;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {

	private static final Logger logger = LoggerFactory.getLogger(McpController.class);

	private StateGraph stateGraph;

	public McpController(@Qualifier("mcpGraph") StateGraph stateGraph) throws GraphStateException {
		this.stateGraph = stateGraph;
	}

	@GetMapping("/weather")
	public String simpleChat(String latitude, String longitude) throws GraphStateException, GraphRunnerException {
		return stateGraph.compile()
			.invoke(Map.of("latitude", latitude, "longitude", longitude))
			.get()
			.value("mcp_result")
			.get()
			.toString();
	}

}
