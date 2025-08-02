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
package com.alibaba.cloud.ai.example.manus.inhouse.tool.calculator;

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
 * 计算器工具
 *
 * 使用注解驱动自动注册到 MCP 服务器
 */
@McpTool
@Component
public class CalculatorTool extends AbstractBaseTool<Map<String, Object>> {

	private final ObjectMapper objectMapper;

	public CalculatorTool(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	private static final Logger log = LoggerFactory.getLogger(CalculatorTool.class);

	@Override
	public String getServiceGroup() {
		return "calculator";
	}

	@Override
	public String getName() {
		return "calculator";
	}

	@Override
	public String getDescription() {
		return "数学计算工具，支持基本数学运算";
	}

	@Override
	public String getParameters() {
		return """
			{
			    "type": "object",
			    "properties": {
			        "expression": {
			            "type": "string",
			            "description": "数学表达式，如：2 + 3 * 4"
			        }
			    },
			    "required": ["expression"]
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
		return "CalculatorTool is ready";
	}

	@Override
	public ToolExecuteResult run(Map<String, Object> input) {
		try {
			String expression = (String) input.get("expression");
			if (expression == null || expression.trim().isEmpty()) {
				return new ToolExecuteResult("错误：表达式不能为空");
			}

			log.info("计算表达式: {}", expression);

			// 简单的表达式计算（实际项目中可以使用更安全的表达式解析器）
			double result = evaluateExpression(expression);

			return new ToolExecuteResult(String.format("计算结果: %s = %.2f", expression, result));

		} catch (Exception e) {
			log.error("计算失败: {}", e.getMessage(), e);
			return new ToolExecuteResult("计算失败: " + e.getMessage());
		}
	}

	/**
	 * 简单的表达式计算
	 */
	private double evaluateExpression(String expression) {
		// 这里使用简单的计算逻辑，实际项目中建议使用安全的表达式解析器
		expression = expression.replaceAll("\\s+", ""); // 移除空格

		// 简单的加减乘除计算
		if (expression.contains("+")) {
			String[] parts = expression.split("\\+");
			return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
		}
		else if (expression.contains("-")) {
			String[] parts = expression.split("-");
			return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
		}
		else if (expression.contains("*")) {
			String[] parts = expression.split("\\*");
			return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
		}
		else if (expression.contains("/")) {
			String[] parts = expression.split("/");
			return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
		}
		else {
			return Double.parseDouble(expression);
		}
	}

}