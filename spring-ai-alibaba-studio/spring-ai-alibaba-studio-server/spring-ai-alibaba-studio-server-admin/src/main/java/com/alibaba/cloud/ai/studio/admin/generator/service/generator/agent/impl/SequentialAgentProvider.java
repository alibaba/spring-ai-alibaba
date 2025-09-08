package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.impl;

import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.AbstractAgentTypeProvider;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.AgentShell;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.CodeSections;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.RenderContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
		if (shell.getInputKeys() != null && !shell.getInputKeys().isEmpty()) {
			String primaryInputKey = shell.getInputKeys().get(0);
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
