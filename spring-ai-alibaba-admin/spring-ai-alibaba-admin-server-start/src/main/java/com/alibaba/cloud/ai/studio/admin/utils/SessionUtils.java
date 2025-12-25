package com.alibaba.cloud.ai.studio.admin.utils;

import com.alibaba.cloud.ai.studio.admin.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public class SessionUtils {

    /**
     * 转换聊天历史为Spring AI消息格式
     *
     * @param messages 消息列表
     * @return Spring AI消息列表
     */
    public static List<Message> convertChatMessages(List<ChatMessage> messages) {
        List<Message> convertedMessages = new ArrayList<>();
        for (ChatMessage message : messages) {
            if ("user".equals(message.getRole())) {
                convertedMessages.add(new UserMessage(message.getContent()));
            } else if ("assistant".equals(message.getRole())) {
                convertedMessages.add(new AssistantMessage(message.getContent()));
            }
            // 忽略其他角色（如system等），根据需要可以扩展
        }
        return convertedMessages;
    }
}
