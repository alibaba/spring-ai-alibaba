/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.examples.documentation.framework.tutorials.mcp;

import org.springframework.ai.model.openai.autoconfigure.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI Alibaba Documentation Examples Application
 * <p>
 * 本应用演示基于 Spring AI + React Agent 的 MCP 调用，包括：
 * <p>
 * - 基于 Spring Boot 的 React Agent 示例
 * <p>
 * - 非 Spring Boot 的 React Agent 示例
 * <p>
 * - 基于 DashScope API 的 ChatClient 示例
 */
@SpringBootApplication(exclude = {  //移除一些无须使用的不相关配置加载
        com.alibaba.cloud.ai.agent.studio.SaaStudioWebModuleAutoConfiguration.class,
        org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration.class,
        OpenAiAudioSpeechAutoConfiguration.class,
        OpenAiImageAutoConfiguration.class,
        OpenAiAudioTranscriptionAutoConfiguration.class,
        OpenAiEmbeddingAutoConfiguration.class,
        OpenAiModerationAutoConfiguration.class,
        OpenAiChatAutoConfiguration.class})
public class RemoteMcpToolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RemoteMcpToolsApplication.class, args);
    }


}
