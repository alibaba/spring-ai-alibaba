/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.cli.clidebugexample;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Objects;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatClient chatClient;

    private final ChatModel chatModel;

    public ChatController(ChatClient chatClient, ChatModel chatModel) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
    }

    @GetMapping("/chat")
    public String chat(String input) {
        return this.chatClient.prompt().user(input).call().content();
    }

    @GetMapping("/stream")
    public String stream(String input) {

        Flux<String> content = this.chatClient.prompt().user(input).stream().content();
        return Objects.requireNonNull(content.collectList().block()).stream().reduce((a, b) -> a + b).get();
    }

    @GetMapping("/chatByModel")
    public String chatByModel(String input) {
        ChatResponse response = chatModel.call(new Prompt(input));
        return response.getResult().getOutput().getContent();
    }

    @GetMapping("/streamByModel")
    public String streamByModel(String input) {
        StringBuilder res = new StringBuilder();
        Flux<ChatResponse> stream = chatModel.stream(new Prompt(input));
        stream.toStream().toList().forEach(resp -> {
            res.append(resp.getResult().getOutput().getContent());
        });

        return res.toString();
    }

}
