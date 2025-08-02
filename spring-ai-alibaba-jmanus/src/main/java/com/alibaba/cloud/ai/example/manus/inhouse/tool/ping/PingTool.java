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
package com.alibaba.cloud.ai.example.manus.inhouse.tool.ping;

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
 * Ping 工具
 * 
 * 使用注解驱动自动注册到 MCP 服务器
 */
@McpTool
@Component
public class PingTool extends AbstractBaseTool<Map<String, Object>> {

	private final ObjectMapper objectMapper;

	public PingTool(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
    
    	private static final Logger log = LoggerFactory.getLogger(PingTool.class);

	@Override
	public String getServiceGroup() {
		return "ping";
	}

	@Override
	public String getName() {
		return "ping";
	}

	@Override
	public String getDescription() {
		return "Ping 工具，测试连接状态";
	}

	@Override
	public String getParameters() {
		return """
			{
			    "type": "object",
			    "properties": {
			        "message": {
			            "type": "string",
			            "description": "要 ping 的消息，可选",
			            "default": "pong"
			        }
			    }
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
		return "PingTool is ready";
	}

	@Override
	public ToolExecuteResult run(Map<String, Object> input) {
		try {
			String message = (String) input.get("message");
			if (message == null || message.trim().isEmpty()) {
				message = "pong";
			}

			log.info("Ping 请求: {}", message);

			return new ToolExecuteResult(String.format("Ping 响应: %s (时间: %s)", message, 
				java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

		} catch (Exception e) {
			log.error("Ping 失败: {}", e.getMessage(), e);
			return new ToolExecuteResult("Ping 失败: " + e.getMessage());
		}
		}
} 