package com.zhych.aimemory.controller;


import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.cloud.ai.memory.service.ChatMemoryService;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class MemoryController {

    @Autowired
    private ChatMemoryService memoryService;


    @PostMapping("/chat")
    public List<ChatMessage> chat(@RequestBody String question) throws NoApiKeyException, InputRequiredException {
        memoryService.completion("123", question);
        return memoryService.history("123", 0);
    }


    @PostMapping("/history")
    public List<ChatMessage> history() {
        return memoryService.history("123", 0);
    }


    @PostMapping("/clear")
    public void clear() {
        memoryService.clear("123");
    }

    @PostMapping("/update")
    public void update() {
        ChatMessage build = ChatMessage.builder()
                .role("user")
                .content("你好")
                .build();
        ChatMessage build1 = ChatMessage.builder()
                .role("assistant")
                .content("有什么可以帮助你")
                .build();
        List<ChatMessage> list = List.of(build, build1);
        memoryService.updateHistory("123", list);
    }
}
