package com.alibaba.cloud.ai.graph.agent.flow.agent.loop;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ConditionLoopStrategy implements LoopStrategy {

    private final Predicate<List<Message>> messagePredicate;

    private final int maxCount = maxLoopCount();

    public ConditionLoopStrategy(Predicate<List<Message>> messagePredicate) {
        this.messagePredicate = messagePredicate;
    }

    @Override
    public Map<String, Object> loopInit(OverAllState state) {
        return Map.of(loopCountKey(), 0, loopFlagKey(), true);
    }

    @Override
    public Map<String, Object> loopDispatch(OverAllState state) {
        @SuppressWarnings("unchecked")
        List<Message> messages = (List<Message>) state.value(LoopStrategy.MESSAGE_KEY).orElse(List.of());
        if(messagePredicate.test(messages)) {
            return Map.of(loopFlagKey(), false);
        } else {
            int count = state.value(loopCountKey(), maxCount);
            if(count < maxCount) {
                return Map.of(loopCountKey(), count + 1, loopFlagKey(), true);
            } else {
                return Map.of(LoopStrategy.MESSAGE_KEY, new SystemMessage("Max loop count reached"), loopFlagKey(), false);
            }
        }
    }
}
