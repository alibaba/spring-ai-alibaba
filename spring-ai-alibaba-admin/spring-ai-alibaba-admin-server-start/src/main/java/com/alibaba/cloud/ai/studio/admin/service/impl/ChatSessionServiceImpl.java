package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.dto.ChatSession;
import com.alibaba.cloud.ai.studio.admin.dto.ModelConfigInfo;
import com.alibaba.cloud.ai.studio.admin.dto.MockTool;
import com.alibaba.cloud.ai.studio.admin.service.ChatSessionService;
import com.alibaba.cloud.ai.studio.admin.service.client.ChatClientFactoryDelegate;
import com.alibaba.cloud.ai.studio.admin.utils.ModelConfigParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {
    
    /**
     * 会话过期时间（30分钟）
     */
    private static final long SESSION_EXPIRE_TIME = 30 * 60 * 1000L;
    
    private final ChatClientFactoryDelegate chatClientFactoryDelegate;
    
    private final ModelConfigParser modelConfigParser;
    
    /**
     * 会话存储Map（生产环境建议使用Redis）
     */
    private final Map<String, ChatSession> sessionStore = new ConcurrentHashMap<>();
    
    /**
     * 会话与ModelClient的绑定关系
     */
    private final Map<String, ChatClient> sessionClients = new ConcurrentHashMap<>();
    
    @Override
    public ChatSession createSessionWithMockTools(String promptKey, String version, String template, String variables,
            String modelConfig, List<MockTool> mockTools) {
        String sessionId = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis();
        ModelConfigInfo modelConfigInfo = modelConfigParser.checkAndGetModelConfigInfo(modelConfig);
        ChatSession session = ChatSession.builder().sessionId(sessionId).promptKey(promptKey).version(version)
                .template(template).variables(variables).modelConfig(modelConfigInfo).createTime(currentTime)
                .lastUpdateTime(currentTime).mockTools(mockTools).build();
        sessionStore.put(sessionId, session);
        log.info("创建新会话: {}", sessionId);
        return session;
    }
    
    @Override
    public ChatSession createEvaluatorSession(String prompt, String variables, String modelConfig) {
        String sessionId = UUID.randomUUID().toString();
        ModelConfigInfo modelConfigInfo = modelConfigParser.checkAndGetModelConfigInfo(modelConfig);
        ChatSession session = ChatSession.builder().template(prompt).variables(variables).modelConfig(modelConfigInfo)
                .sessionId(sessionId).createTime(System.currentTimeMillis()).lastUpdateTime(System.currentTimeMillis())
                .build();
        sessionStore.put(sessionId, session);
        return session;
    }
    
    @Override
    public ChatSession getSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }
        
        ChatSession session = sessionStore.get(sessionId);
        if (session == null) {
            log.warn("会话不存在: {}", sessionId);
            return null;
        }
        
        // 检查会话是否过期
        if (isSessionExpired(session)) {
            log.info("会话已过期，删除: {}", sessionId);
            sessionStore.remove(sessionId);
            return null;
        }
        
        return session;
    }
    
    @Override
    public void updateSession(ChatSession session) {
        if (session != null && session.getSessionId() != null) {
            session.setLastUpdateTime(System.currentTimeMillis());
            sessionStore.put(session.getSessionId(), session);
            log.debug("更新会话: {}", session.getSessionId());
        }
    }
    
    @Override
    public void deleteSession(String sessionId) {
        if (sessionId != null) {
            sessionStore.remove(sessionId);
            sessionClients.remove(sessionId);
            log.info("删除会话及其ModelClient: {}", sessionId);
        }
    }
    
    @Override
    @Scheduled(fixedRate = 10 * 60 * 1000) // 每10分钟执行一次
    public void cleanExpiredSessions() {
        final int[] cleanedCount = {0}; // 使用数组来解决final限制
        
        sessionStore.entrySet().removeIf(entry -> {
            ChatSession session = entry.getValue();
            if (isSessionExpired(session)) {
                log.debug("清理过期会话: {}", entry.getKey());
                cleanedCount[0]++;
                return true;
            }
            return false;
        });
        
        if (cleanedCount[0] > 0) {
            log.info("清理了 {} 个过期会话", cleanedCount[0]);
        }
    }
    
    @Override
    public ChatClient getSessionChatClient(String sessionId) {
        return sessionClients.get(sessionId);
    }
    
    
    @Override
    public ChatClient getOrCreateSessionChatClient(String sessionId, Map<String, String> observationMetadata) {
        return sessionClients.computeIfAbsent(sessionId, key -> {
            ChatSession session = getSession(sessionId);
            if (session == null) {
                throw new RuntimeException("会话不存在: " + sessionId);
            }
            return chatClientFactoryDelegate.createChatClient(session.getModelConfig().getModelId(),
                    session.getModelConfig().getParameters(), observationMetadata);
        });
    }
    
    /**
     * 检查会话是否过期
     */
    private boolean isSessionExpired(ChatSession session) {
        long currentTime = System.currentTimeMillis();
        return (currentTime - session.getLastUpdateTime()) > SESSION_EXPIRE_TIME;
    }
    
    /**
     * 获取当前会话总数（用于监控）
     */
    public int getSessionCount() {
        return sessionStore.size();
    }
    
}
