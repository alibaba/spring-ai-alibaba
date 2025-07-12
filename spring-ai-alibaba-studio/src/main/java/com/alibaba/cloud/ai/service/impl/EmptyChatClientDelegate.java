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
package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.model.ChatClient;
import com.alibaba.cloud.ai.param.ClientRunActionParam;
import com.alibaba.cloud.ai.service.ChatClientDelegate;
import com.alibaba.cloud.ai.vo.ChatClientRunResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 空的 ChatClientDelegate 实现，用于在没有实际实现时提供默认行为
 */
@Component("emptyChatClientDelegate")
public class EmptyChatClientDelegate implements ChatClientDelegate {

    @Override
    public List<ChatClient> list() {
        return new ArrayList<>();
    }

    @Override
    public ChatClient get(String clientName) {
        return null;
    }

    @Override
    public ChatClientRunResult run(ClientRunActionParam runActionParam) {
        return null;
    }
} 