package com.alibaba.cloud.ai.example.zhipuai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/ai")
public class ZhiPuAiController {

    private final ChatModel chatModel;

    public ZhiPuAiController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/chat")
    public String chat(String input) {
        ChatResponse response = chatModel.call(new Prompt(input));
        return response.getResult().getOutput().getContent();
    }


    @GetMapping("/stream")
    public String stream(String input) {

        StringBuilder res = new StringBuilder();
        Flux<ChatResponse> stream = chatModel.stream(new Prompt(input));
        stream.toStream().toList().forEach(resp -> {
            res.append(resp.getResult().getOutput().getContent());
        });

        return res.toString();
    }

}
