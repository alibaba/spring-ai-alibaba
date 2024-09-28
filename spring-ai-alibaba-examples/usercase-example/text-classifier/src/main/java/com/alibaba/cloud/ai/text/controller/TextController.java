package com.alibaba.cloud.ai.text.controller;

import com.alibaba.cloud.ai.text.enums.TextClassifierTypes;
import com.alibaba.cloud.ai.text.service.TextService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/text")
public class TextController {

    @Resource
    private TextService textService;

    @GetMapping
    public String classify(String text) {
        return textService.classifyEmotion(text);
    }

    @GetMapping("/structured-output")
    public TextClassifierTypes classifyStructured(String text) {

        return textService.classifyEmotionStructured(text);
    }

    @GetMapping("/with-hints")
    public String classifyWithHints(String text) {
        return textService.classifyEmotionWithHints(text);
    }

    @GetMapping("/few-shots-prompt")
    public String classifyFewShotsPrompt(String text) {
        return textService.classifyEmotionWithFewShotsPrompt(text);
    }

    @GetMapping("/few-shots-history")
    public String classifyFewShotsHistory(String text) {
        return textService.classifyEmotionWithFewShotsHistory(text);
    }

}
