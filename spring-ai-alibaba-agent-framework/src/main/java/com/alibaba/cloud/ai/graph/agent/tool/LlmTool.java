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

package com.alibaba.cloud.ai.graph.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class LlmTool extends AbstractTool<String, String> implements BaseTool<String, String> {

    private final ChatClient chatClient;

    private final Consumer<? super String> streamConsumer;

    private static final Logger log = LoggerFactory.getLogger(LlmTool.class);

    public LlmTool(ExecutorService executor, ChatClient chatClient, Consumer<? super String> streamConsumer) {
        super(executor);
        this.chatClient = chatClient;
        this.streamConsumer = streamConsumer;
    }

    @Override
    protected Consumer<? super String> streamConsumer() {
        return streamConsumer;
    }

    @Override
    public String applyInterruptible(String s, ToolContext toolContext, Consumer<? super String> streamConsumer) throws Exception {
        Flux<String> flux = applyStream(s, toolContext);
        CompletableFuture<String> future = flux
                .doOnNext(streamConsumer)
                .collect(StringBuilder::new, StringBuilder::append)
                .map(StringBuilder::toString)
                .toFuture();
        return future.get();
    }

    @Override
    public ToolCallback toolCallback() {
        return FunctionToolCallback
                .builder("llm", this)
                .description("llm")
                .inputType(String.class)
                .build();
    }

    @Override
    public String fallback(String s, ToolContext toolContext, Throwable throwable) {
        log.error("LLM工具无法使用，原因：{}：{}", throwable.getClass().getSimpleName(), throwable.getMessage());
        return "LLM工具无法使用，原因：" + throwable.getClass().getSimpleName() + "：" + throwable.getMessage();
    }


    @Override
    public Flux<String> applyStream(String s, ToolContext toolContext) {
        return chatClient.prompt().user(s).stream().content();
    }
}
