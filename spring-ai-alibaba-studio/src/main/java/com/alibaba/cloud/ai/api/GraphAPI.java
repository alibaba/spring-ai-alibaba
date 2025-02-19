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
package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.param.GraphStreamParam;
import com.alibaba.cloud.ai.service.GraphService;
import com.alibaba.cloud.ai.graph.GraphInitData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import reactor.core.publisher.Flux;

@Tag(name = "Graph", description = "the graph API")
public interface GraphAPI {

	GraphService graphService();

	@Operation(summary = "init graph", description = "", tags = { "Graph" })
	@GetMapping(value = "init", produces = MediaType.APPLICATION_JSON_VALUE)
	default R<GraphInitData> init() {
		return R.success(graphService().getPrintableGraphData());
	}

	@Operation(summary = "stream", description = "", tags = { "Graph" })
	@PostMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	default Flux<ServerSentEvent<String>> stream(HttpServletRequest request, GraphStreamParam param) throws Exception {
		param.setSessionId(request.getSession().getId());
		return graphService().stream(param, request.getInputStream());
	}

}
