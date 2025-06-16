package com.alibaba.cloud.ai.example.deepresearch.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author XiaoYunTao
 * @since 2025/6/15
 */
@RestController
@RequestMapping("/chatClient")
@Slf4j
public class ChatClientController {

    private final ChatClient customizeAgent;

    public ChatClientController(@Lazy @Qualifier("customize1Agent") ChatClient customizeAgent) {
        this.customizeAgent = customizeAgent;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody(required = false) String msg) {
        return customizeAgent.prompt(msg)
                .options(DashScopeChatOptions.builder().withModel("qwen-long").build())
                .call()
                .content();
    }

    @Bean
    @Lazy
    public ObservationHandler<Observation.Context> customizeObservationHandler() {
        return new ObservationHandler<>() {
            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }
            @Override
            public void onStart(Observation.Context context) {
                try {
                    // 使用JSON.toJSONString的重载方法，设置最大深度和循环引用检测
                    log.info("开始执行 : {} ", JSON.toJSONString(context,
                        SerializerFeature.DisableCircularReferenceDetect,
                        SerializerFeature.WriteMapNullValue,
                        SerializerFeature.PrettyFormat,
                        SerializerFeature.WriteClassName,
                        SerializerFeature.DisableCheckSpecialChar));
                } catch (Exception e) {
                    // 如果序列化失败，至少打印一些基本信息
                    log.info("开始执行，context类型: {}, 详细信息: {}",
                        context.getClass().getName(),
                        context.toString());
                }
            }
        };
    }
}
