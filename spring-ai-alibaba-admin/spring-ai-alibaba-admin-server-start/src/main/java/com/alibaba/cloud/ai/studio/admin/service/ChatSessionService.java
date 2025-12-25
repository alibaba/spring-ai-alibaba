package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.dto.ChatSession;
import com.alibaba.cloud.ai.studio.admin.dto.MockTool;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

public interface ChatSessionService {
    
    /**
     * 创建新会话
     *
     * @param promptKey   Prompt Key
     * @param version     版本号
     * @param template    Prompt模板
     * @param variables   变量配置
     * @param modelConfig 模型配置
     * @return 会话对象
     */
    default ChatSession createSession(String promptKey, String version, String template, String variables, String modelConfig){
        return createSessionWithMockTools(promptKey, version, template, variables, modelConfig, null);
    }
    
    
    /**
     * 创建新会话
     *
     * @param promptKey   Prompt Key
     * @param version     版本号
     * @param template    Prompt模板
     * @param variables   变量配置
     * @param modelConfig 模型配置
     * @param mockTools   模拟工具列表
     * @return 会话对象
     */
    ChatSession createSessionWithMockTools(String promptKey, String version, String template, String variables, String modelConfig, List<MockTool> mockTools);


    /**
     * 创建新会话
     *
     * @param variables   变量配置
     * @param modelConfig 模型配置
     * @return 会话对象
     */
    ChatSession createEvaluatorSession(String prompt, String variables, String modelConfig);
    
    /**
     * 获取会话
     *
     * @param sessionId 会话ID
     * @return 会话对象，如果不存在返回null
     */
    ChatSession getSession(String sessionId);
    
    /**
     * 更新会话
     *
     * @param session 会话对象
     */
    void updateSession(ChatSession session);
    
    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     */
    void deleteSession(String sessionId);
    
    /**
     * 清理过期会话
     */
    void cleanExpiredSessions();
    
    /**
     * 获取会话绑定的ChatClient
     *
     * @param sessionId 会话ID
     * @return ChatClient或null
     */
    ChatClient getSessionChatClient(String sessionId);
    
    /**
     * 获取或创建会话的ChatClient
     *
     * @param sessionId 会话ID
     * @return ChatClient实例
     */
    ChatClient getOrCreateSessionChatClient(String sessionId, Map<String, String> observationMetadata);
}
