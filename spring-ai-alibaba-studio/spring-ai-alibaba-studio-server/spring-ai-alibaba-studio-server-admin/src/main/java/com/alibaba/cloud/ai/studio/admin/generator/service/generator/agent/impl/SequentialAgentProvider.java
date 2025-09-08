package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.impl;

import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.AgentShell;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.AgentTypeProvider;
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
public class SequentialAgentProvider implements AgentTypeProvider {

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

		StringBuilder code = new StringBuilder();
		code.append("SequentialAgent ")
			.append(var)
			.append(" = SequentialAgent.builder()\n")
			.append(".name(\"")
			.append(esc(shell.getName()))
			.append("\")\n")
			.append(".description(\"")
			.append(esc(nvl(shell.getDescription())))
			.append("\")\n");
		if (shell.getOutputKey() != null) {
			code.append(".outputKey(\"").append(esc(shell.getOutputKey())).append("\")\n");
		}
		if (shell.getInputKeys() != null && !shell.getInputKeys().isEmpty()) {
			String primaryInputKey = shell.getInputKeys().get(0);
			code.append(".inputKey(\"").append(esc(primaryInputKey)).append("\")\n");
		}
		if (childVarNames != null && !childVarNames.isEmpty()) {
			code.append(".subAgents(List.of(").append(String.join(", ", childVarNames)).append("))\n");
		}
		// state.strategies（全量映射，默认 messages=Append）
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

		// 只有当 messages 未被显式定义时，才添加默认策略
		if (!hasMessagesStrategy) {
			code.append("strategies.put(\"messages\", new AppendStrategy());\n");
		}

		code.append("return strategies;\n").append("})\n").append(".build();\n");

		return new CodeSections()
			.imports("import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;",
					"import com.alibaba.cloud.ai.graph.KeyStrategy;",
					"import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;",
					"import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;", "import java.util.*;")
			.code(code.toString())
			.var(var);
	}

	private static String nvl(String s) {
		return s == null ? "" : s;
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

}
