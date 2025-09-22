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
 * Provider for LoopAgent code generation.
 */
@Component
public class LoopAgentProvider extends AbstractAgentTypeProvider {

    @Override
    public String type() {
        return "LoopAgent";
    }

    @Override
    public String handleVersion() {
        return "v1";
    }

    @Override
    public String jsonSchema() {
        return """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "title": "LoopAgent Handle",
                  "type": "object",
                  "properties": {
                    "loop_mode": { "type": "string", "enum": ["COUNT", "CONDITION", "ITERABLE", "ARRAY", "JSON_ARRAY"] },
                    "loop_count": { "type": "integer", "minimum": 1 },
                    "loop_condition_bean": { "type": "string", "title": "Condition Predicate Bean Name" },
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
                "loop_mode", "COUNT",
                "loop_count", 1,
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
        String var = ctx.nextVar("loopAgent_");

        StringBuilder code = generateBasicBuilderCode("LoopAgent", var, shell);

        if (shell.inputKeys() != null && !shell.inputKeys().isEmpty()) {
            String primaryInputKey = shell.inputKeys().get(0);
            code.append(".inputKey(\"").append(esc(primaryInputKey)).append("\")\n");
        }

        appendSubAgents(code, childVarNames);

        String loopMode = str(handle.get("loop_mode"));
        if (loopMode != null && !loopMode.isBlank()) {
            code.append(".loopMode(LoopAgent.LoopMode.").append(loopMode.toUpperCase()).append(")\n");
        }

        Integer loopCount = toInt(handle.get("loop_count"));
        if (loopCount != null && loopCount > 0) {
            code.append(".loopCount(").append(loopCount).append(")\n");
        }

        // todo: do not render loopCondition at present. Support can be added by injecting a Bean into the template and assembling the call later.

        StateStrategyResult stateResult = generateStateStrategyCode(handle, null);
        code.append(stateResult.code);

        code.append(".build();\n");

        return new CodeSections()
            .imports(
                "import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;",
                "import com.alibaba.cloud.ai.graph.KeyStrategy;",
                "import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;",
                "import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;",
                "import java.util.*;"
            )
            .code(code.toString())
            .var(var);
    }

    @Override
    protected void validateSpecific(Map<String, Object> root) {
        requireSubAgents(root, 1);

        Map<String, Object> handle = requireHandle(root);
        Object mode = handle.get("loop_mode");
        if (mode == null) {
            throw new IllegalArgumentException("LoopAgent requires 'loop_mode'");
        }

        String loopMode = String.valueOf(mode);
        if ("COUNT".equalsIgnoreCase(loopMode)) {
            requirePositiveNumber(handle.get("loop_count"), "loop_count", 1);
        }
        if ("CONDITION".equalsIgnoreCase(loopMode)) {
            throw new IllegalArgumentException("LoopAgent CONDITION mode is not supported in codegen yet");
        }
    }
}


