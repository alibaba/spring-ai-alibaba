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
 * Provider for LLMRoutingAgent code generation.
 */
@Component
public class LlmRoutingAgentProvider extends AbstractAgentTypeProvider {

    @Override
    public String type() {
        return "LLMRoutingAgent";
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
                  "title": "LLMRoutingAgent Handle",
                  "type": "object",
                  "properties": {
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
                "state", Map.of("strategies", Map.of("messages", "append"))
        );
    }

    @Override
    public Map<String, Object> migrate(Map<String, Object> oldHandle, String fromVersion) {
        return oldHandle;
    }

    @Override
    public CodeSections render(AgentShell shell, Map<String, Object> handle, RenderContext ctx, List<String> childVarNames) {
        String var = ctx.nextVar("llmRoutingAgent_");

        StringBuilder code = generateBasicBuilderCode("LlmRoutingAgent", var, shell);

        if (shell.inputKeys() != null && !shell.inputKeys().isEmpty()) {
            String primaryInputKey = shell.inputKeys().get(0);
            code.append(".inputKey(\"").append(esc(primaryInputKey)).append("\")\n");
        }

        code.append(".model(chatModel)\n");

        appendSubAgents(code, childVarNames);

        StateStrategyResult stateResult = generateStateStrategyCode(handle, "new AppendStrategy()");
        code.append(stateResult.code);

        code.append(".build();\n");

        return new CodeSections()
            .imports(
                "import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;",
                "import com.alibaba.cloud.ai.graph.KeyStrategy;",
                "import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;",
                "import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;",
                "import org.springframework.ai.chat.model.ChatModel;",
                "import java.util.*;"
            )
            .code(code.toString())
            .var(var);
    }

    @Override
    protected void validateSpecific(Map<String, Object> root) {
        Map<String, Object> handle = requireHandle(root);

        if (handle.get("model") == null) {
            throw new IllegalArgumentException("LLMRoutingAgent requires model configuration in handle");
        }

        requireSubAgents(root, 1);
    }
}


