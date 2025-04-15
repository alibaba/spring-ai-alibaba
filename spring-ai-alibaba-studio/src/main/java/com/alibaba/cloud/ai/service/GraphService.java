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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.graph.GraphInitData;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.param.GraphStreamParam;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface GraphService {

	Map<String, StateGraph> getStateGraphs();

	/**
	 * @return Init printable graph data (of MERMAID or PlantUML format) using StateGraph
	 * definition
	 */
	GraphInitData getPrintableGraphData(String name) throws GraphStateException;

	Flux<ServerSentEvent<String>> stream(String name, GraphStreamParam param, InputStream inputStream) throws Exception;

}
