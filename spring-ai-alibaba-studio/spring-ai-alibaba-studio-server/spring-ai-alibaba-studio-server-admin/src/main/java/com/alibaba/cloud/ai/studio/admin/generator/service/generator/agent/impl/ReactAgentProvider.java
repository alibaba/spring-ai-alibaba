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
 * @since 2025/8/28 17:57
 */
@Component
public class ReactAgentProvider implements AgentTypeProvider {

    @Override public String type() { return "ReactAgent"; }
    @Override public String handleVersion() { return "v1"; }

    @Override
    public String jsonSchema() {
        // 最小 JSON Schema（可逐步完善）
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
        return Map.of(
                "instruction", "You are a helpful AI assistant.",
                "max_iterations", 6,
                "tools", List.of(),
                "chat_options", Map.of(),
                "compile_config", Map.of(),
                "state", Map.of("strategies", Map.of("messages", "append"))
        );
    }

    @Override
    public Map<String, Object> migrate(Map<String, Object> oldHandle, String fromVersion) {
        // v1 初版不做迁移
        return oldHandle;
    }

    @Override
    public CodeSections render(AgentShell shell, Map<String, Object> handle, RenderContext ctx, List<String> childVarNames) {
        String var = ctx.nextVar("reactAgent_");

        // instruction 优先使用壳层，兼容旧 handle.instruction
        String instruction = shell.getInstruction() != null && !shell.getInstruction().isBlank() ? shell.getInstruction() : str(handle.get("instruction"));
        Integer maxIter = toInt(handle.get("max_iterations"));
        boolean hasResolver = handle.containsKey("resolver") && str(handle.get("resolver")) != null;

        StringBuilder code = new StringBuilder();
        code.append("ReactAgent ").append(var).append(" = ReactAgent.builder()\n")
                .append(tab(1)).append(".name(\"").append(esc(shell.getName())).append("\")\n")
                .append(tab(1)).append(".description(\"").append(esc(nvl(shell.getDescription()))).append("\")\n");
        if (shell.getOutputKey() != null) {
            code.append(tab(1)).append(".outputKey(\"").append(esc(shell.getOutputKey())).append("\")\n");
        }
        if (shell.getInputKey() != null) {
            code.append(tab(1)).append(".llmInputMessagesKey(\"").append(esc(shell.getInputKey())).append("\")\n");
        }
        code.append(tab(1)).append(".model(chatModel)\n");

        if (instruction != null && !instruction.isBlank()) {
            code.append(tab(1)).append(".instruction(\"").append(esc(instruction)).append("\")\n");
        }
        if (maxIter != null && maxIter > 0) {
            code.append(tab(1)).append(".maxIterations(").append(maxIter).append(")\n");
        }
        if (hasResolver) {
            code.append(tab(1)).append(".resolver(toolCallbackResolver)\n");
        }
        // state.strategies → KeyStrategy（全量映射，缺省时为 messages 追加策略）
        code.append(tab(1)).append(".state(() -> {\n")
                .append(tab(2)).append("Map<String, KeyStrategy> strategies = new HashMap<>();\n");

        // 解析 handle.state.strategies 生成代码
        Object stateObj = handle.get("state");
        if (stateObj instanceof Map<?,?> stateMap) {
            Object strategiesObj = stateMap.get("strategies");
            if (strategiesObj instanceof Map<?,?> strategiesMap) {
                for (Map.Entry<?,?> e : strategiesMap.entrySet()) {
                    String k = String.valueOf(e.getKey());
                    String v = String.valueOf(e.getValue());
                    String strategyNew = (v != null && v.equalsIgnoreCase("append")) ? "new AppendStrategy()" : "new ReplaceStrategy()";
                    code.append(tab(2)).append("strategies.put(\"").append(esc(k)).append("\", ").append(strategyNew).append(");\n");
                }
            }
        }
        // 若未显式指定 messages 策略，则默认替换
        code.append(tab(2)).append("strategies.putIfAbsent(\"messages\", new ReplaceStrategy());\n")
                .append(tab(2)).append("return strategies;\n")
                .append(tab(1)).append("})\n")
                .append(tab(1)).append(".build();\n");

        return new CodeSections()
                .imports(
                        "import com.alibaba.cloud.ai.graph.agent.ReactAgent;",
                        "import com.alibaba.cloud.ai.graph.KeyStrategy;",
                        "import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;",
                        "import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;",
                        "import org.springframework.ai.chat.model.ChatModel;",
                        hasResolver ? "import org.springframework.ai.tool.resolution.ToolCallbackResolver;" : null,
                        "import java.util.*;"
                )
                .code(code.toString())
                .var(var)
                .resolver(hasResolver);
    }

    private static String tab(int n) { return "\t".repeat(Math.max(0, n)); }
    private static String nvl(String s) { return s == null ? "" : s; }
    private static String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static Integer toInt(Object v) {
        if (v instanceof Integer i) return i;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) try { return Integer.parseInt(s.trim()); } catch (Exception ignore) {}
        return null;
    }
}
