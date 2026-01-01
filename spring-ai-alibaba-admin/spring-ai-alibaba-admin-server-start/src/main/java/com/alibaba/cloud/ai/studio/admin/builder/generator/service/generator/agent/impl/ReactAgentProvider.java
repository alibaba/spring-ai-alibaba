/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.studio.admin.builder.generator.service.generator.agent.impl;

import com.alibaba.cloud.ai.studio.admin.builder.generator.service.generator.agent.AbstractAgentTypeProvider;
import com.alibaba.cloud.ai.studio.admin.builder.generator.service.generator.agent.AgentShell;
import com.alibaba.cloud.ai.studio.admin.builder.generator.service.generator.agent.CodeSections;
import com.alibaba.cloud.ai.studio.admin.builder.generator.service.generator.agent.RenderContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.studio.admin.builder.generator.utils.CodeGenUtils.*;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/28 17:57
 */
@Component
public class ReactAgentProvider extends AbstractAgentTypeProvider {

	@Override
	public String type() {
		return "ReactAgent";
	}

	@Override
	public String handleVersion() {
		return "v1";
	}

	@Override
	public String jsonSchema() {
		// 最�?JSON Schema（可逐步完善�?
		return """
				{
				  "$schema": "https://json-schema.org/draft/2020-12/schema",
				  "title": "ReactAgent Handle",
				  "type": "object",
				  "properties": {
				    "instruction": { "type": "string", "title": "System Instruction" },
				    "model": {
				      "type": "object",
				      "title": "Model Override",
				      "properties": {
				        "provider": { "type": "string" },
				        "name": { "type": "string" },
				        "options": { "type": "object" },
				        "chat_client_bean": { "type": "string" }
				      }
				    },
				    "resolver": { "type": "string", "title": "Tool Resolver Bean" },
				    "tools": { "type": "array", "items": { "type": "string" } },
				    "max_iterations": { "type": "integer", "minimum": 1, "default": 6 },
				    "chat_options": { "type": "object" },
				    "compile_config": { "type": "object" },
				    "state": {
				      "type": "object",
				      "properties": {
				        "strategies": {
				          "type": "object",
				          "additionalProperties": { "type": "string", "enum": ["append", "replace"] }
				        }
				      }
				    },
				    "hooks": {
				      "type": "object",
				      "properties": {
				        "pre_llm": { "type": "array", "items": { "type": "string" } },
				        "post_llm": { "type": "array", "items": { "type": "string" } },
				        "pre_tool": { "type": "array", "items": { "type": "string" } },
				        "post_tool": { "type": "array", "items": { "type": "string" } }
				      }
				    }
				  }
				}
				""";
	}

	@Override
	public Map<String, Object> defaultHandle() {
		return Map.of("instruction", "You are a helpful AI assistant.", "max_iterations", 6, "tools", List.of(),
				"chat_options", Map.of(), "compile_config", Map.of(), "state",
				Map.of("strategies", Map.of("messages", "append")));
	}

	@Override
	public Map<String, Object> migrate(Map<String, Object> oldHandle, String fromVersion) {
		// 初版不做迁移
		return oldHandle;
	}

	@Override
	public CodeSections render(AgentShell shell, Map<String, Object> handle, RenderContext ctx,
			List<String> childVarNames) {
		String var = ctx.nextVar("reactAgent_");

		// instruction 优先使用壳层
		String instruction = shell.instruction() != null && !shell.instruction().isBlank() ? shell.instruction()
				: str(handle.get("instruction"));
		Integer maxIter = toInt(handle.get("max_iterations"));
		boolean hasResolver = handle.containsKey("resolver") && str(handle.get("resolver")) != null;

		StringBuilder code = generateBasicBuilderCode("ReactAgent", var, shell);

		// ReactAgent 特有的字�?
		if (shell.inputKeys() != null && !shell.inputKeys().isEmpty()) {
			// todo: 目前取第一个作为主输入键， 后续计划将多个inputKey通过占位符注入到instruction�?
			String primaryInputKey = shell.inputKeys().get(0);
			code.append(".inputKey(\"").append(esc(primaryInputKey)).append("\")\n");
		}
		code.append(".model(chatModel)\n");

		if (instruction != null && !instruction.isBlank()) {
			code.append(".instruction(\"").append(esc(instruction)).append("\")\n");
		}
		if (maxIter != null && maxIter > 0) {
			code.append(".maxIterations(").append(maxIter).append(")\n");
		}
		if (hasResolver) {
			code.append(".resolver(toolCallbackResolver)\n");
		}

		StateStrategyResult stateResult = generateStateStrategyCode(handle, "new AppendStrategy()");
		code.append(stateResult.code);

		code.append(".build();\n");

		return new CodeSections().imports("import com.alibaba.cloud.ai.graph.CompiledGraph;",
				"import com.alibaba.cloud.ai.graph.agent.ReactAgent;", "import com.alibaba.cloud.ai.graph.KeyStrategy;",
				"import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;",
				"import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;",
				"import org.springframework.ai.chat.model.ChatModel;",
				"import org.springframework.context.annotation.Bean;",
				"import org.springframework.stereotype.Component;",
				hasResolver ? "import org.springframework.beans.factory.ObjectProvider;" : null,
				hasResolver ? "import org.springframework.ai.tool.resolution.ToolCallbackResolver;" : null,
				"import java.util.*;")
			.code(code.toString())
			.var(var)
			.resolver(hasResolver);
	}

	@Override
	protected void validateSpecific(Map<String, Object> root) {
		// ReactAgent 必须�?model 配置
		Map<String, Object> handle = requireHandle(root);

		if (handle.get("model") == null) {
			throw new IllegalArgumentException("ReactAgent requires model configuration in handle");
		}

		// 如果�?tools，检查相关配�?
		if (handle.get("tools") instanceof List<?> tools && !tools.isEmpty()) {
			// 检�?tools 是否为空字符�?
			for (Object tool : tools) {
				if (tool instanceof String s && s.trim().isEmpty()) {
					throw new IllegalArgumentException("ReactAgent tool names cannot be empty");
				}
			}
		}

		// 检�?max_iterations 如果存在，必须是正数
		Object maxIterations = handle.get("max_iterations");
		if (maxIterations != null) {
			requirePositiveNumber(maxIterations, "max_iterations", 1);
		}
	}

}
