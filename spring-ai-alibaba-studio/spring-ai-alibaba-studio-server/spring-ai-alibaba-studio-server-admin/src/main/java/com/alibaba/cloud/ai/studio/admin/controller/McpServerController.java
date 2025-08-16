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

package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerDetail;
import com.alibaba.cloud.ai.studio.core.base.service.McpServerService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.admin.annotation.ApiModelAttribute;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Title: CreateDate: 2025/4/24 15:13
 *
 * @author guning.lt
 * @since 1.0.0.3
 **/
@Slf4j
@RestController
@Tag(name = "mcp")
@RequestMapping("/console/v1/mcp-servers")
public class McpServerController {

	private final McpServerService mcpServerService;

	public McpServerController(McpServerService mcpServerService) {
		this.mcpServerService = mcpServerService;
	}

	/**
	 * Creates a new MCP server with the provided configuration.
	 * @param detail Detailed configuration of the MCP server
	 * @return Result containing the server code of the newly created MCP server
	 */
	@PostMapping()
	public Result<String> createMcpServer(@RequestBody McpServerDetail detail) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(detail.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("serverName"));
		}
		if (StringUtils.isBlank(detail.getDeployConfig())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("deployConfig"));
		}
		String serverCode = mcpServerService.createMcp(detail);
		return Result.success(context.getRequestId(), serverCode);
	}

	/**
	 * Updates an existing MCP server with new configuration.
	 * @param detail Updated configuration for the MCP server
	 * @return Result indicating the success of the update operation
	 */
	@PutMapping()
	public Result<String> updateMcpServer(@RequestBody McpServerDetail detail) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(detail.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("serverName"));
		}
		if (StringUtils.isBlank(detail.getDeployConfig())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("deployConfig"));
		}
		if (StringUtils.isBlank(detail.getServerCode())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("serverCode"));
		}
		mcpServerService.updateMcp(detail);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Deletes an MCP server.
	 * @param serverCode Unique identifier of the MCP server to be deleted
	 * @return Result indicating the success of the deletion operation
	 */
	@DeleteMapping("/{serverCode}")
	public Result<Void> deleteMcpServer(@PathVariable("serverCode") String serverCode) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(serverCode)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("serverCode"));
		}
		mcpServerService.deleteMcp(serverCode);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Get detailed information about a specific MCP server.
	 * @param serverCode Unique identifier of the MCP server
	 * @param needTools Flag indicating whether to include tool information in the
	 * response
	 * @return Result containing the detailed information of the MCP server
	 */
	@GetMapping("/{serverCode}")
	public Result<McpServerDetail> getMcpServer(@PathVariable("serverCode") String serverCode,
			@RequestParam(value = "need_tools") Boolean needTools) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(serverCode)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("serverCode"));
		}
		McpServerDetail mcpServerDetail = mcpServerService.getMcp(serverCode, needTools);
		return Result.success(context.getRequestId(), mcpServerDetail);
	}

	/**
	 * Get a paginated list of MCP servers based on the provided query criteria.
	 * @param query Query parameters for filtering and pagination
	 * @return Result containing a paginated list of MCP server details
	 */
	@GetMapping()
	public Result<PagingList<McpServerDetail>> listMcpServers(@ApiModelAttribute McpQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		PagingList<McpServerDetail> mcpServerDetails = mcpServerService.list(query);
		return Result.success(context.getRequestId(), mcpServerDetails);
	}

	/**
	 * Get a list of MCP servers based on a list of server codes.
	 * @param query Query parameters containing the list of server codes
	 * @return Result containing the list of MCP server details
	 */
	@PostMapping("/query-by-codes")
	public Result<List<McpServerDetail>> listMcpServersByCodes(@RequestBody McpQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		List<McpServerDetail> mcpServerDetails = mcpServerService.listByCodes(query);
		return Result.success(context.getRequestId(), mcpServerDetails);
	}

	/**
	 * Debugs a tool on a specific MCP server.
	 * @param request Request object containing details about the tool to be debugged
	 * @return Result containing the response from the tool execution
	 */
	@PostMapping("/debug-tools")
	public Result<McpServerCallToolResponse> debugTools(@RequestBody McpServerCallToolRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		request.setAccountId(context.getAccountId());
		request.setWorkspaceId(context.getWorkspaceId());
		request.setRequestId(context.getRequestId());
		if (StringUtils.isBlank(request.getToolName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("toolName"));
		}
		if (StringUtils.isBlank(request.getServerCode())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("serverCode"));
		}
		Result<McpServerCallToolResponse> mcpServerCallToolResponseResult = mcpServerService.callTool(request);
		return Result.success(context.getRequestId(), mcpServerCallToolResponseResult.getData());
	}

}
