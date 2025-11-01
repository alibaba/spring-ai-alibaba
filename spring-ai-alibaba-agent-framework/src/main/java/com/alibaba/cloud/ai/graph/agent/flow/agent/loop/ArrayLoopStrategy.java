package com.alibaba.cloud.ai.graph.agent.flow.agent.loop;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.util.json.JsonParser;

import java.util.List;
import java.util.Map;

public class ArrayLoopStrategy implements LoopStrategy {

    @Override
    public Map<String, Object> loopInit(OverAllState state) {
        @SuppressWarnings("unchecked")
        List<Message> messages = (List<Message>) state.value(LoopStrategy.MESSAGE_KEY).orElse(List.of());
        String lastMessage;
        if(!messages.isEmpty()) {
            lastMessage = messages.get(messages.size() - 1).getText();
        } else {
            lastMessage = null;
        }
        if(lastMessage == null) {
            return Map.of(loopCountKey(), 0, loopFlagKey(), false, loopListKey(), List.of());
        }
        try {
            List<?> list = JsonParser.fromJson(lastMessage, List.class);
            return Map.of(loopCountKey(), 0, loopFlagKey(), true, loopListKey(), list);
        } catch (Exception e) {
            return Map.of(loopCountKey(), 0, loopFlagKey(), false, loopListKey(), List.of(),
            LoopStrategy.MESSAGE_KEY, new SystemMessage("Invalid json array format"));
        }
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
}
