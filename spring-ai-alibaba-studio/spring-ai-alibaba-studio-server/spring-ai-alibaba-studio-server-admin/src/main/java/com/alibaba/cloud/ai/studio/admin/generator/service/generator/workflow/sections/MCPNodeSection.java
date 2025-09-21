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

package com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.sections;

import java.util.List;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.MCPNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;
import com.alibaba.cloud.ai.studio.core.base.entity.McpServerEntity;
import com.alibaba.cloud.ai.studio.core.base.service.McpServerService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerDeployConfig;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MCPNodeSection implements NodeSection<MCPNodeData> {

	private final McpServerService mcpServerService;

	public MCPNodeSection(@Autowired(required = false) McpServerService mcpServerService) {
		this.mcpServerService = mcpServerService;
	}

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.MCP.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		if (this.mcpServerService == null) {
			throw new IllegalArgumentException(
					"The current mode does not support Studio's MCP node code generation. Please start the complete StudioApplication class");
		}
		MCPNodeData nodeData = (MCPNodeData) node.getData();

		RequestContext context = RequestContextHolder.getRequestContext();
		McpServerEntity mcpServerEntity = mcpServerService.getMcpByCode(context.getWorkspaceId(),
				nodeData.getServerCode(), null);
		if (mcpServerEntity == null) {
			throw new IllegalArgumentException("MCP Server [" + nodeData.getServerCode() + "] not found!");
		}

		McpServerDeployConfig deployConfig = JsonUtils.fromJson(mcpServerEntity.getDeployConfig(),
				McpServerDeployConfig.class);
		String endPoint = deployConfig.getRemoteEndpoint();
		String baseUri = deployConfig.getRemoteAddress();

		return String.format("""
				// -- MCP Node [%s] --
				stateGraph.addNode("%s", AsyncNodeAction.node_async(
				    createMcpNodeAction(%s, %s, %s, %s, %s, %s)
				));

				""", nodeData.getServerName(), varName, ObjectToCodeUtil.toCode(baseUri),
				ObjectToCodeUtil.toCode(endPoint), ObjectToCodeUtil.toCode(nodeData.getToolName()),
				ObjectToCodeUtil.toCode(nodeData.getInputJsonTemplate()),
				ObjectToCodeUtil.toCode(nodeData.getInputKeys()), ObjectToCodeUtil.toCode(nodeData.getOutputKey()));
	}

	@Override
	public String assistMethodCode(DSLDialectType dialectType) {
		return switch (dialectType) {
			case STUDIO ->
				"""
						private NodeAction createMcpNodeAction(String baseUri, String endPoint, String toolName,
						                                       String inputTemplate, List<String> keys, String outputKey) {
						    return state -> {
						        // create client
						        McpSyncClient client = McpClient.sync(
						                HttpClientSseClientTransport
						                        .builder(baseUri)
						                        .sseEndpoint(endPoint)
						                        .build()
						        ).build();
						        client.initialize();
						        SyncMcpToolCallbackProvider provider = new SyncMcpToolCallbackProvider(client);

						        // get tools
						        ToolCallback[] toolCallbacks = provider.getToolCallbacks();
						        ToolCallback toolCallback = Arrays.stream(toolCallbacks)
						                .filter(t -> t.getToolDefinition().name().contains(toolName))
						                .findFirst()
						                .orElseThrow(() -> new IllegalStateException("toolName [" + toolName + "] not found!"));

						        // prepare input
						        String input = inputTemplate;
						                          for(String key : keys) {
						                              input = input.replace("{" + key + "}", state.value(key).orElse("").toString());
						                          }

						        // call
						        String call = toolCallback.call(input);
						        System.out.println(call);
						        client.close();
						        return Map.of(outputKey, call);
						    };
						}
						""";
			default -> NodeSection.super.assistMethodCode(dialectType);
		};
	}

	@Override
	public List<String> getImports() {
		return List.of("io.modelcontextprotocol.client.McpSyncClient", "io.modelcontextprotocol.client.McpClient",
				"io.modelcontextprotocol.client.transport.HttpClientSseClientTransport",
				"org.springframework.ai.mcp.SyncMcpToolCallbackProvider", "org.springframework.ai.tool.ToolCallback",
				"java.util.Arrays");
	}

}
