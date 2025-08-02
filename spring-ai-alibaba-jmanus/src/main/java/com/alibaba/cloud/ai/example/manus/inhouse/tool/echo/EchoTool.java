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
package com.alibaba.cloud.ai.example.manus.inhouse.tool.echo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.inhouse.annotation.McpTool;
import com.alibaba.cloud.ai.example.manus.inhouse.annotation.McpToolSchema;
import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 回显工具
 *
 * 使用注解驱动自动注册到 MCP 服务器
 */
@McpTool
@Component
public class EchoTool extends AbstractBaseTool<Map<String, Object>> {

	private final ObjectMapper objectMapper;

	public EchoTool(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	private static final Logger log = LoggerFactory.getLogger(EchoTool.class);

	@Override
	public String getServiceGroup() {
		return "echo";
	}

	@Override
	public String getName() {
		return "echo";
	}

	@Override
	public String getDescription() {
		return "回显工具，返回输入的消息";
	}

	@Override
	public String getParameters() {
		return """
			{
			    "type": "object",
			    "properties": {
			        "message": {
			            "type": "string",
			            "description": "要回显的消息"
			        }
			    },
			    "required": ["message"]
			}
			""";
	}

	@Override
	public Class<Map<String, Object>> getInputType() {
		return (Class<Map<String, Object>>) (Class<?>) Map.class;
	}

	@Override
	public void cleanup(String planId) {
		// 无需清理资源
	}

	@Override
	public String getCurrentToolStateString() {
		return "EchoTool is ready";
	}

	@Override
	public ToolExecuteResult run(Map<String, Object> input) {
		try {
			String message = (String) input.get("message");
			if (message == null) {
				message = "";
			}

			log.info("回显消息: {}", message);

			return new ToolExecuteResult("回显: " + message);

		} catch (Exception e) {
			log.error("回显失败: {}", e.getMessage(), e);
			return new ToolExecuteResult("回显失败: " + e.getMessage());
		}
	}

}