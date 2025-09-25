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
 * Provider for ParallelAgent code generation.
 */
@Component
public class ParallelAgentProvider extends AbstractAgentTypeProvider {

    @Override
    public String type() {
        return "ParallelAgent";
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
                  "title": "ParallelAgent Handle",
                  "type": "object",
                  "properties": {
                    "merge_strategy": { "type": "string", "enum": ["default", "list", "concat"] },
                    "separator": { "type": "string", "default": "\\n\\n" },
                    "max_concurrency": { "type": "integer", "minimum": 1 },
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
                "merge_strategy", "default",
                "separator", "\n\n",
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
        String var = ctx.nextVar("parallelAgent_");

        StringBuilder code = generateBasicBuilderCode("ParallelAgent", var, shell);

        if (shell.inputKeys() != null && !shell.inputKeys().isEmpty()) {
            String primaryInputKey = shell.inputKeys().get(0);
            code.append(".inputKey(\"").append(esc(primaryInputKey)).append("\")\n");
        }

        appendSubAgents(code, childVarNames);

        String mergeStrategy = str(handle.get("merge_strategy"));
        if (mergeStrategy == null || mergeStrategy.isBlank() || mergeStrategy.equalsIgnoreCase("default")) {
            code.append(".mergeStrategy(new ParallelAgent.DefaultMergeStrategy())\n");
        } else if (mergeStrategy.equalsIgnoreCase("list")) {
            code.append(".mergeStrategy(new ParallelAgent.ListMergeStrategy())\n");
        } else if (mergeStrategy.equalsIgnoreCase("concat")) {
            String separator = str(handle.get("separator"));
            if (separator == null) separator = "\\n\\n";
            code.append(".mergeStrategy(new ParallelAgent.ConcatenationMergeStrategy(\"").append(esc(separator)).append("\"))\n");
        }

        Integer maxConcurrency = toInt(handle.get("max_concurrency"));
        if (maxConcurrency != null && maxConcurrency > 0) {
            code.append(".maxConcurrency(").append(maxConcurrency).append(")\n");
        }

        StateStrategyResult stateResult = generateStateStrategyCode(handle, null);
        code.append(stateResult.code);

        code.append(".build();\n");

        return new CodeSections()
            .imports(
                "import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;",
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
        requireSubAgents(root, 2);

        // Verify max_concurrency, if it exists, it must be >= 1
        Object mc = ((Map<?, ?>) root.getOrDefault("handle", Map.of())).get("max_concurrency");
        if (mc != null) {
            requirePositiveNumber(mc, "max_concurrency", 1);
        }
    }
}


