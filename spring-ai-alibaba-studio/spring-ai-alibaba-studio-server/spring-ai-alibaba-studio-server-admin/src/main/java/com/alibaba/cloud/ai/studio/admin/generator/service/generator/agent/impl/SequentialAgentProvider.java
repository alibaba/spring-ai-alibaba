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
package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.impl;

import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.AbstractAgentTypeProvider;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.AgentShell;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.CodeSections;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.RenderContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.studio.admin.generator.utils.CodeGenUtils.*;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/28 17:59
 */
@Component
public class SequentialAgentProvider extends AbstractAgentTypeProvider {

	@Override
	public String type() {
		return "SequentialAgent";
	}

	@Override
	public String handleVersion() {
		return "v1";
	}

	@Override
	public String jsonSchema() {
		// 顺序编排本身无需太多专属字段（先预留 chat_options/compile_config/state）
		return """
				{
				  "$schema": "https://json-schema.org/draft/2020-12/schema",
				  "title": "SequentialAgent Handle",
				  "type": "object",
				  "properties": {
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
				    }
				  }
				}
				""";
	}

	@Override
	public Map<String, Object> defaultHandle() {
		return Map.of("chat_options", Map.of(), "compile_config", Map.of(), "state", Map.of("strategies", Map.of()));
	}

	@Override
	public Map<String, Object> migrate(Map<String, Object> oldHandle, String fromVersion) {
		return oldHandle;
	}

	@Override
	public CodeSections render(AgentShell shell, Map<String, Object> handle, RenderContext ctx,
			List<String> childVarNames) {
		String var = ctx.nextVar("seqAgent_");

		// 使用基类方法生成基础 builder 代码
		StringBuilder code = generateBasicBuilderCode("SequentialAgent", var, shell);

		// SequentialAgent 特有的字段
		if (shell.inputKeys() != null && !shell.inputKeys().isEmpty()) {
			String primaryInputKey = shell.inputKeys().get(0);
			code.append(".inputKey(\"").append(esc(primaryInputKey)).append("\")\n");
		}

		// 使用基类方法添加子代理
		appendSubAgents(code, childVarNames);

		// 使用基类方法生成状态策略代码
		StateStrategyResult stateResult = generateStateStrategyCode(handle, "new AppendStrategy()");
		code.append(stateResult.code);

		code.append(".build();\n");

		return new CodeSections()
			.imports("import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;",
					"import com.alibaba.cloud.ai.graph.KeyStrategy;",
					"import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;",
					"import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;", "import java.util.*;")
			.code(code.toString())
			.var(var);
	}

	@Override
	protected void validateSpecific(Map<String, Object> root) {
		// SequentialAgent 必须有至少一个子代理
		requireSubAgents(root, 1);
	}

}
