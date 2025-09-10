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
package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.studio.admin.generator.utils.CodeGenUtils.*;

/**
 * AgentTypeProvider 的抽象基类，提供通用的校验逻辑和渲染工具
 *
 * @author yHong
 * @version 1.0
 * @since 2025/9/8 18:31
 */
public abstract class AbstractAgentTypeProvider implements AgentTypeProvider {

	/**
	 * 提供默认的校验实现，子类可以重写以添加特定的校验逻辑
	 */
	@Override
	public void validateDSL(Map<String, Object> root) {
		// 基础校验：检查必需字段
		if (root == null) {
			throw new IllegalArgumentException(type() + " requires valid configuration");
		}

		String name = (String) root.get("name");
		if (isBlank(name)) {
			throw new IllegalArgumentException(type() + " requires 'name' field");
		}

		// 调用子类特定的校验逻辑
		validateSpecific(root);
	}

	/**
	 * 子类实现特定的校验逻辑
	 * @param root DSL 根对象
	 */
	protected abstract void validateSpecific(Map<String, Object> root);

	/**
	 * 校验 handle 是否存在
	 * @param root DSL 根对象
	 * @return handle Map
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> requireHandle(Map<String, Object> root) {
		Map<String, Object> handle = (Map<String, Object>) root.get("handle");
		if (handle == null) {
			throw new IllegalArgumentException(type() + " requires 'handle' configuration");
		}
		return handle;
	}

	/**
	 * 校验必须有子代理
	 * @param root DSL 根对象
	 * @param minCount 最小数量
	 */
	@SuppressWarnings("unchecked")
	protected List<Map<String, Object>> requireSubAgents(Map<String, Object> root, int minCount) {
		Object subs = root.get("sub_agents");
		if (!(subs instanceof List)) {
			throw new IllegalArgumentException(type() + " requires 'sub_agents' (array)");
		}
		List<Map<String, Object>> subAgents = (List<Map<String, Object>>) subs;
		if (subAgents.size() < minCount) {
			throw new IllegalArgumentException(
					type() + " requires at least " + minCount + " sub-agent(s), got: " + subAgents.size());
		}
		return subAgents;
	}

	/**
	 * 校验数值字段
	 * @param value 字段值
	 * @param fieldName 字段名
	 * @param minValue 最小值（包含）
	 * @return 数值
	 */
	protected int requirePositiveNumber(Object value, String fieldName, int minValue) {
		if (value == null) {
			throw new IllegalArgumentException(type() + " requires '" + fieldName + "'");
		}
		if (!(value instanceof Number)) {
			throw new IllegalArgumentException(fieldName + " must be a number");
		}
		int num = ((Number) value).intValue();
		if (num < minValue) {
			throw new IllegalArgumentException(fieldName + " must be at least " + minValue + ", got: " + num);
		}
		return num;
	}

	/**
	 * 检查字符串是否为空
	 */
	protected boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	/**
	 * 检查是否有有效的输入键
	 */
	protected boolean hasValidInputKey(Map<String, Object> root) {
		String inputKey = (String) root.get("input_key");
		List<?> inputKeys = (List<?>) root.get("input_keys");
		return !isBlank(inputKey) || (inputKeys != null && !inputKeys.isEmpty());
	}

	/**
	 * 生成基础 builder 代码（name, description, outputKey）
	 * @param builderName builder 类名（如 "ReactAgent", "SequentialAgent"）
	 * @param varName 变量名
	 * @param shell Agent 基础信息
	 * @return 生成的代码
	 */
	protected StringBuilder generateBasicBuilderCode(String builderName, String varName, AgentShell shell) {
		StringBuilder code = new StringBuilder();
		code.append(builderName)
			.append(" ")
			.append(varName)
			.append(" = ")
			.append(builderName)
			.append(".builder()\n")
			.append(".name(\"")
			.append(esc(shell.name()))
			.append("\")\n")
			.append(".description(\"")
			.append(esc(nvl(shell.description())))
			.append("\")\n");

		if (shell.outputKey() != null) {
			code.append(".outputKey(\"").append(esc(shell.outputKey())).append("\")\n");
		}

		return code;
	}

	/**
	 * 生成状态策略代码 todo: 目前渲染的每个子agent都有自己的state注册， 需要确认flowAgent的state是全局统一的还是子agent隔离的
	 * @param handle Agent handle 配置
	 * @param defaultMessagesStrategy 当 messages 策略未定义时的默认值（null 表示不添加默认值）
	 * @return 生成的状态策略代码和是否有 messages 策略的标志
	 */
	protected StateStrategyResult generateStateStrategyCode(Map<String, Object> handle,
			String defaultMessagesStrategy) {
		StringBuilder code = new StringBuilder();
		code.append(".state(() -> {\n").append("Map<String, KeyStrategy> strategies = new HashMap<>();\n");

		boolean hasMessagesStrategy = false;
		Object stateObj = handle.get("state");
		if (stateObj instanceof Map<?, ?> stateMap) {
			Object strategiesObj = stateMap.get("strategies");
			if (strategiesObj instanceof Map<?, ?> strategiesMap) {
				for (Map.Entry<?, ?> e : strategiesMap.entrySet()) {
					String k = String.valueOf(e.getKey());
					String v = String.valueOf(e.getValue());
					String strategyNew = (v != null && v.equalsIgnoreCase("append")) ? "new AppendStrategy()"
							: "new ReplaceStrategy()";
					code.append("strategies.put(\"").append(esc(k)).append("\", ").append(strategyNew).append(");\n");

					if ("messages".equals(k)) {
						hasMessagesStrategy = true;
					}
				}
			}
		}

		// 添加默认 messages 策略（如果需要）
		if (!hasMessagesStrategy && defaultMessagesStrategy != null) {
			code.append("strategies.put(\"messages\", ").append(defaultMessagesStrategy).append(");\n");
		}

		code.append("return strategies;\n").append("})\n");

		return new StateStrategyResult(code.toString(), hasMessagesStrategy);
	}

	/**
	 * 状态策略生成结果
	 */
	protected static class StateStrategyResult {

		public final String code;

		public final boolean hasMessagesStrategy;

		public StateStrategyResult(String code, boolean hasMessagesStrategy) {
			this.code = code;
			this.hasMessagesStrategy = hasMessagesStrategy;
		}

	}

	/**
	 * 添加子代理列表
	 */
	protected void appendSubAgents(StringBuilder code, List<String> childVarNames) {
		if (childVarNames != null && !childVarNames.isEmpty()) {
			code.append(".subAgents(List.of(").append(String.join(", ", childVarNames)).append("))\n");
		}
	}

}
