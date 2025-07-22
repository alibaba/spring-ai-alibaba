package com.alibaba.cloud.ai.example.deepresearch.controller;

import com.alibaba.cloud.ai.example.deepresearch.agents.AgentManager;
import com.alibaba.cloud.ai.example.deepresearch.repository.ModelParamRepositoryImpl;
import com.alibaba.cloud.ai.example.deepresearch.service.ModelConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agent-config")
public class AgentConfigController {

    private final ModelConfigService modelConfigService;
    private final AgentManager agentManager;

    private static final Logger logger = LoggerFactory.getLogger(AgentConfigController.class);

    public AgentConfigController(ModelConfigService modelConfigService, AgentManager agentManager) {
        this.modelConfigService = modelConfigService;
        this.agentManager = agentManager;
    }

    /**
     * 获取所有模型配置
     */
    @GetMapping("getModelConfigs")
    public ResponseEntity<List<ModelParamRepositoryImpl.AgentModel>> getModelConfigs() {
        try {
            return ResponseEntity.ok(modelConfigService.getModelConfigs());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量更新模型配置
     */
    @PostMapping("updateModelConfigs")
    public ResponseEntity<Void> updateModelConfigs(@RequestBody List<ModelParamRepositoryImpl.AgentModel> models) {
        try {
            modelConfigService.updateModelConfigs(models);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 调用指定Agent模型
     */
    @PostMapping(value = "/agent/call", produces = MediaType.APPLICATION_JSON_VALUE)
    public String call(@RequestBody Map<String, Object> message) {
        logger.info("Received chat request: {}", message);
        ChatClient chatClient = agentManager.getAgentByName((String) message.get("agentName"));
        return chatClient.prompt((String) message.get("message"))
                .call()
                .content();
    }
}
