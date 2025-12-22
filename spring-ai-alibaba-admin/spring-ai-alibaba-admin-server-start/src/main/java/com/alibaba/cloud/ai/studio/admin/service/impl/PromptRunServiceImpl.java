package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.dto.ChatMessage;
import com.alibaba.cloud.ai.studio.admin.dto.ChatMessageMetrics;
import com.alibaba.cloud.ai.studio.admin.dto.ChatSession;
import com.alibaba.cloud.ai.studio.admin.dto.MockTool;
import com.alibaba.cloud.ai.studio.admin.dto.PromptRunResponse;
import com.alibaba.cloud.ai.studio.admin.dto.request.PromptRunRequest;
import com.alibaba.cloud.ai.studio.admin.service.ChatSessionService;
import com.alibaba.cloud.ai.studio.admin.service.PromptRunService;
import com.alibaba.cloud.ai.studio.admin.service.advisors.TraceIdEnrichAdvisor;
import com.alibaba.cloud.ai.studio.admin.service.client.ChatClientFactoryDelegate;
import com.alibaba.cloud.ai.studio.admin.utils.ModelConfigParser;
import com.alibaba.cloud.ai.studio.admin.utils.SessionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AdvisorUtils;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptRunServiceImpl implements PromptRunService {
    
    private final ChatSessionService chatSessionService;
    
    private final ChatClientFactoryDelegate chatClientFactoryDelegate;
    
    private final ModelConfigParser modelConfigParser;
    
    private final ObjectMapper objectMapper;
    
    private final ObservationRegistry observationRegistry;
    
    @Override
    public Flux<PromptRunResponse> run(PromptRunRequest request) {
        log.info("运行带会话的Prompt调试: {}", request);
        
        try {
            // 1. 获取或创建会话
            ChatSession session = getOrCreateSession(request);
            
            // 2. 添加用户消息到会话
            session.addUserMessage(request.getMessage());
            chatSessionService.updateSession(session);
            if (StringUtils.hasText(request.getPromptKey())) {
                session.setPromptKey(request.getPromptKey());
            }
            if (StringUtils.hasText(request.getVersion())) {
                session.setVersion(request.getVersion());
            }
            
            session.setTemplate(request.getTemplate());
            session.setVariables(request.getVariables());
            session.setMockTools(request.getMockTools());
            session.setModelConfig(modelConfigParser.checkAndGetModelConfigInfo(request.getModelConfig()));
            
            // 3. 返回会话信息
            PromptRunResponse sessionInfo = PromptRunResponse.createSessionInfoResponse(session);
            
            return Flux.concat(
                    // 首先返回会话信息
                    Flux.just(sessionInfo),
                    
                    // 然后返回真实的AI流式响应
                    generateRealAIResponse(session, request).onErrorResume(error -> {
                        log.error("模型调用失败，返回错误响应", error);
                        return Flux.just(PromptRunResponse.createErrorResponse(session.getSessionId(),
                                "模型调用失败: " + error.getMessage()));
                    }));
            
        } catch (Exception e) {
            log.error("处理会话请求失败", e);
            return Flux.just(PromptRunResponse.createErrorResponse(null, "处理请求失败: " + e.getMessage()));
        }
    }
    
    @Override
    public ChatSession getSession(String sessionId) {
        return chatSessionService.getSession(sessionId);
    }
    
    @Override
    public void deleteSession(String sessionId) {
        chatSessionService.deleteSession(sessionId);
    }
    
    /**
     * 获取或创建会话
     */
    private ChatSession getOrCreateSession(PromptRunRequest request) {
        // 如果强制创建新会话或没有提供sessionId，创建新会话
        if (Boolean.TRUE.equals(request.getNewSession()) || request.getSessionId() == null || request.getSessionId()
                .trim().isEmpty()) {
            return chatSessionService.createSessionWithMockTools(request.getPromptKey(), request.getVersion(),
                    request.getTemplate(), request.getVariables(), request.getModelConfig(), request.getMockTools());
        }
        
        // 尝试获取现有会话
        ChatSession existingSession = chatSessionService.getSession(request.getSessionId());
        if (existingSession != null) {
            return existingSession;
        }
        
        // 会话不存在，创建新会话
        log.warn("会话 {} 不存在，创建新会话", request.getSessionId());
        return chatSessionService.createSessionWithMockTools(request.getPromptKey(), request.getVersion(),
                request.getTemplate(), request.getVariables(), request.getModelConfig(), request.getMockTools());
    }
    
    /**
     * 生成真实的AI流式响应
     *
     * @param session 会话对象
     * @return 流式响应
     */
    private Flux<PromptRunResponse> generateRealAIResponse(ChatSession session, PromptRunRequest request) {
        // 用于收集完整响应的容器
        AtomicReference<StringBuilder> completeResponse = new AtomicReference<>(new StringBuilder());
        AtomicReference<ChatMessageMetrics> metrics = new AtomicReference<>(ChatMessageMetrics.builder().build());
        
        String fullPrompt = modelConfigParser.replaceVariables(session.getTemplate(), session.getVariables());
        Map<String, String> observationMetadata = new HashMap<>(4);
        if (StringUtils.hasText(request.getPromptKey()) && !"playground".equals(request.getPromptKey())) {
            observationMetadata.put("promptKey", request.getPromptKey());
            observationMetadata.put("promptVersion", request.getVersion());
            observationMetadata.put("promptTemplate", request.getTemplate());
            observationMetadata.put("promptVariables", request.getVariables());
            observationMetadata.put("studioSource", "prompt");
        } else {
            observationMetadata.put("promptKey", "playground-" + System.currentTimeMillis());
            observationMetadata.put("promptTemplate", request.getTemplate());
            observationMetadata.put("promptVariables", request.getVariables());
            observationMetadata.put("studioSource", "playground");
        }
        List<Advisor> advisors = new ArrayList<>();
        advisors.add(new TraceIdEnrichAdvisor(observationRegistry));
        ChatClient client = chatClientFactoryDelegate.createChatClient(session.getModelConfig().getModelId(),
                session.getModelConfig().getParameters(), advisors, observationMetadata);
        
        List<ToolCallback> functionToolCallbacks = buildMockToolBacks(request.getMockTools());
        
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(fullPrompt));
        messages.addAll(SessionUtils.convertChatMessages(session.getMessages()));
        Prompt prompt = new Prompt(messages);
        return client.prompt(prompt).toolCallbacks(functionToolCallbacks).stream().chatClientResponse()
                .map(response -> {
                    // 收集完整响应
                    ChatResponse chatResponse = response.chatResponse();
                    assert chatResponse != null;
                    if (AdvisorUtils.onFinishReason().test(response)) {
                        Usage usage = chatResponse.getMetadata().getUsage();
                        String traceId = (String) response.context().get("traceId");
                        metrics.set(ChatMessageMetrics.builder().usage(usage).traceId(traceId).build());
                        return PromptRunResponse.createMetricsResponse(session.getSessionId(), metrics.get());
                    } else if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                        String content = chatResponse.getResult().getOutput().getText();
                        completeResponse.get().append(content);
                        return PromptRunResponse.createMessageResponse(session.getSessionId(), content);
                    } else {
                        return PromptRunResponse.createMessageResponse(session.getSessionId(), "");
                    }
                }).doOnComplete(() -> {
                    // 响应完成后，将完整响应添加到会话历史
                    try {
                        String fullResponse = completeResponse.get().toString();
                        if (StringUtils.hasText(fullResponse)) {
                            ChatMessage assistantMessage = ChatMessage.createAssistantMessage(fullResponse);
                            assistantMessage.setMetrics(metrics.get());
                            session.addMessage(assistantMessage);
                            chatSessionService.updateSession(session);
                            log.info("会话 {} 完成AI响应，响应长度: {}", session.getSessionId(), fullResponse.length());
                        }
                    } catch (Exception e) {
                        log.error("更新会话历史失败: sessionId={}", session.getSessionId(), e);
                    }
                });
    }
    
    public List<ToolCallback> buildMockToolBacks(List<MockTool> mockTools) {
        List<ToolCallback> mockToolCallbacks = new ArrayList<>();
        if (mockTools == null) {
            return mockToolCallbacks;
        }
        for (MockTool mockTool : mockTools) {
            String name = mockTool.getToolDefinition().getName();
            String description = mockTool.getToolDefinition().getDescription();
            String output = mockTool.getOutput();
            String inputSchema = mockTool.getToolDefinition().getParameters();
            MockFunction mockFunction = new MockFunction(output, inputSchema);
            ToolCallback functionToolCallback = FunctionToolCallback.builder(name, mockFunction)
                    .description(description).inputSchema(inputSchema).inputType(Map.class).build();
            mockToolCallbacks.add(functionToolCallback);
        }
        return mockToolCallbacks;
    }
    
    
    public class MockFunction implements Function<Map<String, Object>, String> {
        
        private final String output;
        
        private final String inputSchema;
        
        public MockFunction(String output, String inputSchema) {
            this.output = output;
            this.inputSchema = inputSchema;
        }
        
        @Override
        public String apply(Map<String, Object> inputMap) {
            try {
                JsonNode schemaNode = objectMapper.readTree(inputSchema);
                JsonNode dataNode = objectMapper.valueToTree(inputMap);
                
                JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
                JsonSchema schema = factory.getSchema(schemaNode);
                Set<ValidationMessage> errors = schema.validate(dataNode);
                
                if (!errors.isEmpty()) {
                    throw new IllegalArgumentException("Tool Calls Invalid input data: " + errors);
                }
                return this.output;
                
            } catch (JsonProcessingException e) {
                log.error("JSON 处理失败: ", e);
                throw new RuntimeException("JSON 处理失败: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("JSON 处理失败: ", e);
                throw new RuntimeException("Schema 校验失败: " + e.getMessage(), e);
            }
        }
        
    }
    
}
