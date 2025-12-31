/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph.observation;

import com.alibaba.cloud.ai.graph.observation.metric.SpringAiAlibabaObservationMetricAttributes;
import io.micrometer.common.KeyValues;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Spring AI Alibaba Chat Model Observation Convention.
 * <p>
 * Extends default convention to include standard Langfuse/OpenTelemetry
 * attributes
 * for prompt (gen_ai.prompt) and completion (gen_ai.completion), enabling full
 * visibility in observability platforms.
 *
 * @author sixiyida
 * @since 2025/7/3
 */
public class SpringAiAlibabaChatModelObservationConvention extends DefaultChatModelObservationConvention {

    private static final Logger logger = LoggerFactory.getLogger(SpringAiAlibabaChatModelObservationConvention.class);

    @Override
    public KeyValues getHighCardinalityKeyValues(ChatModelObservationContext context) {
        KeyValues keyValues = super.getHighCardinalityKeyValues(context);

        try {
            // Capture Input (Prompt)
            if (context.getRequest() != null && context.getRequest().getInstructions() != null) {
                String prompt = context.getRequest().getInstructions().stream()
                        .map(this::extractText)
                        .collect(Collectors.joining("\n"));

                if (prompt != null && !prompt.isEmpty()) {
                    keyValues = keyValues.and(SpringAiAlibabaObservationMetricAttributes.LANGFUSE_INPUT.value(), prompt)
                            .and(SpringAiAlibabaObservationMetricAttributes.GEN_AI_PROMPT.value(), prompt);
                }
            }

            // Capture Output (Completion)
            if (context.getResponse() != null && context.getResponse().getResult() != null) {
                AssistantMessage outputMsg = context.getResponse().getResult().getOutput();
                if (outputMsg != null) {
                    String output = extractText(outputMsg);
                    if (output != null && !output.isEmpty()) {
                        keyValues = keyValues
                                .and(SpringAiAlibabaObservationMetricAttributes.LANGFUSE_OUTPUT.value(), output)
                                .and(SpringAiAlibabaObservationMetricAttributes.GEN_AI_COMPLETION.value(), output);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract attributes in SpringAiAlibabaChatModelObservationConvention", e);
        }

        return keyValues;
    }

    private String extractText(Message message) {
        if (message == null)
            return "";

        if (message instanceof UserMessage) {
            return ((UserMessage) message).getText();
        }
        if (message instanceof AssistantMessage) {
            return ((AssistantMessage) message).getText();
        }
        if (message instanceof SystemMessage) {
            return ((SystemMessage) message).getText();
        }

        try {
            java.lang.reflect.Method getText = message.getClass().getMethod("getText");
            Object result = getText.invoke(message);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            return message.toString();
        }
    }
}
