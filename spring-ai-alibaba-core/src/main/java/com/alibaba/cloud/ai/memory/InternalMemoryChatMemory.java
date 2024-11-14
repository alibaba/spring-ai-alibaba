package com.alibaba.cloud.ai.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class InternalMemoryChatMemory implements ChatMemory{
    Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();

    public InternalMemoryChatMemory() {
    }
    @Override
    public void add(String conversationId, List<Message> messages){
        this.conversationHistory.putIfAbsent(conversationId, new ArrayList<>());
        (this.conversationHistory.get(conversationId)).addAll(messages);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> all = this.conversationHistory.get(conversationId);
        return all != null ? all.stream().skip(Math.max(0, all.size() - lastN)).toList() : List.of();
    }

    @Override
    public void clear(String conversationId) {
        this.conversationHistory.remove(conversationId);
    }

}
