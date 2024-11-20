package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.entity.ChatDTO;
import com.alibaba.cloud.ai.entity.ModelObservationDetailEntity;
import com.alibaba.cloud.ai.entity.ModelObservationEntity;
import com.alibaba.cloud.ai.service.impl.ModelObservationDetailServiceImpl;
import com.alibaba.cloud.ai.service.impl.ModelObservationServiceImpl;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/19
 */
@CrossOrigin
@RestController
@RequestMapping("studio/api/model_observation")
public class ModelObservationApiController {

    private final ModelObservationServiceImpl modelObservationServiceImpl;
    private final ModelObservationDetailServiceImpl modelObservationDetailServiceImpl;
    private final ChatClient chatClient;

    private final DashScopeChatModel dashScopeChatModel;

    public ModelObservationApiController(ModelObservationServiceImpl modelObservationServiceImpl,
                                         ModelObservationDetailServiceImpl modelObservationDetailServiceImpl,
                                          ObservationRegistry observationRegistry,
                                         DashScopeApi dashScopeApi) {
        this.modelObservationServiceImpl = modelObservationServiceImpl;
        this.modelObservationDetailServiceImpl = modelObservationDetailServiceImpl;
        this.dashScopeChatModel = new DashScopeChatModel(
                dashScopeApi,
                DashScopeChatOptions.builder()
                        .withModel(DashScopeApi.DEFAULT_CHAT_MODEL)
                        .withTemperature(0.7d)
                        .build(),
                null,
                new RetryTemplate(),
                observationRegistry
        );
        this.chatClient = ChatClient.create(dashScopeChatModel, observationRegistry);
    }

    @GetMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    public R<List<ModelObservationEntity>> list() {
        List<ModelObservationEntity> list = modelObservationServiceImpl.list();
        return R.success(list);
    }

    @GetMapping(value = "detail/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public R<List<ModelObservationDetailEntity>> detailList() {
        List<ModelObservationDetailEntity> list = modelObservationDetailServiceImpl.list();
        return R.success(list);
    }

    /**
     * 直接对话接口
     * @param chatDTO
     * @return
     */
    @PostMapping(value = "generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public R<String> generate(@RequestBody ChatDTO chatDTO) {
        String call = chatClient.prompt(chatDTO.getMessage()).call().content();
        return R.success(call);
    }
}
