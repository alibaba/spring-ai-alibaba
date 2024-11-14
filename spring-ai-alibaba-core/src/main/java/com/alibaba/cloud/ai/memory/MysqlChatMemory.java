package com.alibaba.cloud.ai.memory;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class MysqlChatMemory extends InternalMemoryChatMemory {
    @Override
    public void add(String conversationId, List<Message> messages){

    }
    @Override
    public List<Message> get(String conversationId, int lastN){
        return null;
    }
    @Override
    public void clear(String conversationId) {

    }
}
