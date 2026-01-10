/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.graph.agent.flow.agent.loop;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.core.convert.converter.Converter;

import java.util.List;
import java.util.Map;

/**
 * JSON array loop strategy that retrieves a JSON array from the current message state,
 * sends each array element as a message to the model, and returns the result.
 * By default, the text of the last message is treated as a JSON array, but users can customize the converter.
 *
 * @author vlsmb
 * @since 2025/11/1
 */
public class ArrayLoopStrategy implements LoopStrategy {

    private final Converter<List<Message>, List<?>> converter;

    public ArrayLoopStrategy(Converter<List<Message>, List<?>> converter) {
        this.converter = converter;
    }

    public ArrayLoopStrategy() {
        this(DEFAULT_MESSAGE_CONVERTER);
    }

    @Override
    public Map<String, Object> loopInit(OverAllState state) {
        @SuppressWarnings("unchecked")
        List<Message> messages = (List<Message>) state.value(LoopStrategy.MESSAGE_KEY).orElse(List.of());
        List<?> list = converter.convert(messages);
        if(list != null) {
            return Map.of(loopCountKey(), 0, loopFlagKey(), true, loopListKey(), list);
        }
        return Map.of(loopCountKey(), 0, loopFlagKey(), false, loopListKey(), List.of(),
                LoopStrategy.MESSAGE_KEY, new SystemMessage("Invalid json array format"));
    }

    @Override
    public Map<String, Object> loopDispatch(OverAllState state) {
        List<?> list = state.value(loopListKey(), List.class).orElse(List.of());
        int index = state.value(loopCountKey(), maxLoopCount());
        if(index < list.size()) {
            UserMessage message = new UserMessage(list.get(index).toString());
            return Map.of(loopCountKey(), index + 1, loopFlagKey(), true,
                    LoopStrategy.MESSAGE_KEY, message);
        } else {
            return Map.of(loopFlagKey(), false);
        }
    }

    /**
     * 默认的转换器，将最后一个消息的文本作为json数组
     */
    private static final Converter<List<Message>, List<?>> DEFAULT_MESSAGE_CONVERTER =
            messages -> {
                String lastMessage;
                if(!messages.isEmpty()) {
                    lastMessage = messages.get(messages.size() - 1).getText();
                } else {
                    lastMessage = null;
                }
                if(lastMessage == null) {
                    return null;
                }
                return JsonParser.fromJson(lastMessage, List.class);
            };

}
