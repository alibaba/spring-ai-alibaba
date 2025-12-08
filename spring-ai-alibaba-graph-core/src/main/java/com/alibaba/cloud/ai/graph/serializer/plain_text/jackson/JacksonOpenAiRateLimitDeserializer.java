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
package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;


import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.ai.openai.metadata.OpenAiRateLimit;

import java.io.IOException;
import java.time.Duration;

/**
 * auth: dahua
 */
public class JacksonOpenAiRateLimitDeserializer extends StdDeserializer<OpenAiRateLimit> {

    public JacksonOpenAiRateLimitDeserializer() {
        super(OpenAiRateLimit.class);
    }

    @Override
    public OpenAiRateLimit deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JacksonException {
        TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);
        String requestsLimitNode = treeNode.get("requestsLimit").toString();
        String requestsRemainingNode = treeNode.get("requestsRemaining").toString();
        String tokensLimitNode = treeNode.get("tokensLimit").toString();
        String tokensRemainingNode = treeNode.get("tokensRemaining").toString();
        String requestsResetNode = treeNode.get("requestsReset").toString();
        String tokensResetNode = treeNode.get("tokensReset").toString();
        Long requestsLimit = requestsLimitNode == null || "null".equals(requestsLimitNode) ? null : Long.parseLong(requestsLimitNode);
        Long requestsRemaining = requestsRemainingNode == null || "null".equals(requestsRemainingNode) ? null : Long.parseLong(requestsRemainingNode);
        Long tokensLimit = tokensLimitNode == null || "null".equals(tokensLimitNode) ? null : Long.parseLong(tokensLimitNode);
        Long tokensRemaining = tokensRemainingNode == null || "null".equals(tokensRemainingNode) ? null : Long.parseLong(tokensRemainingNode);
        Duration requestsReset = requestsResetNode == null || "null".equals(requestsResetNode) ? null : Duration.parse(requestsResetNode);
        Duration tokensReset = tokensResetNode == null || "null".equals(tokensResetNode) ? null : Duration.parse(tokensResetNode);
        return new OpenAiRateLimit(requestsLimit, requestsRemaining, requestsReset, tokensLimit, tokensRemaining, tokensReset);
    }
}
