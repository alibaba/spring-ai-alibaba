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
package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.domain.McpConnectRequest;
import com.alibaba.cloud.ai.service.McpInspectorService;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("studio/api/mcpInspector")
public class McpInspectorAPIController {

	private final McpInspectorService mcpInspectorService;

	public McpInspectorAPIController(McpInspectorService mcpInspectorService) {
		this.mcpInspectorService = mcpInspectorService;
	}

	@PostMapping("/init")
	public R<String> mcpClientInit(@RequestBody McpConnectRequest request) {
		return mcpInspectorService.init(request);
	}

	@PostMapping(value = "/list")
	public R<McpSchema.ListToolsResult> mcpClientList(@RequestBody String clientName) {
		return mcpInspectorService.listTools(clientName);
	}

}
