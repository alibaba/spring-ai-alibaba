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

    @Override public String type() { return "SequentialAgent"; }
    @Override public String handleVersion() { return "v1"; }

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
        return Map.of(
                "chat_options", Map.of(),
                "compile_config", Map.of(),
                "state", Map.of("strategies", Map.of())
        );
    }

    @Override
    public Map<String, Object> migrate(Map<String, Object> oldHandle, String fromVersion) {
        return oldHandle;
    }

    @Override
    public CodeSections render(AgentShell shell, Map<String, Object> handle, RenderContext ctx, List<String> childVarNames) {
        String var = ctx.nextVar("seqAgent_");

        StringBuilder code = new StringBuilder();
        code.append("SequentialAgent ").append(var).append(" = SequentialAgent.builder()\n")
                .append(tab(1)).append(".name(\"").append(esc(shell.getName())).append("\")\n")
                .append(tab(1)).append(".description(\"").append(esc(nvl(shell.getDescription()))).append("\")\n");
        if (shell.getOutputKey() != null) {
            code.append(tab(1)).append(".outputKey(\"").append(esc(shell.getOutputKey())).append("\")\n");
        }
        if (shell.getInputKey() != null) {
            code.append(tab(1)).append(".inputKey(\"").append(esc(shell.getInputKey())).append("\")\n");
        }
        if (childVarNames != null && !childVarNames.isEmpty()) {
            code.append(tab(1)).append(".subAgents(List.of(").append(String.join(", ", childVarNames)).append("))\n");
        }
        // state.strategies
        code.append(tab(1)).append(".state(() -> {\n")
                .append(tab(2)).append("Map<String, KeyStrategy> strategies = new HashMap<>();\n")
                .append(tab(2)).append("strategies.put(\"messages\", new AppendStrategy());\n")
                .append(tab(2)).append("// TODO: map additional strategies from handle.state.strategies\n")
                .append(tab(2)).append("return strategies;\n")
                .append(tab(1)).append("})\n")
                .append(tab(1)).append(".build();\n");

        return new CodeSections()
                .imports(
                        "import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;",
                        "import com.alibaba.cloud.ai.graph.KeyStrategy;",
                        "import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;",
                        "import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;",
                        "import java.util.*;"
                )
                .code(code.toString())
                .var(var);
    }

    private static String tab(int n) { return "\t".repeat(Math.max(0, n)); }
    private static String nvl(String s) { return s == null ? "" : s; }
    private static String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
