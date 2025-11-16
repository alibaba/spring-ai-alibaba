package com.alibaba.cloud.ai.examples.documentation.framework.advanced;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 上下文工程（Context Engineering）示例
 *
 * 演示如何通过上下文工程提高Agent的可靠性，包括：
 * 1. 模型上下文：系统提示、消息历史、工具、模型选择、响应格式
 * 2. 工具上下文：工具访问和修改状态
 * 3. 生命周期上下文：Hook机制
 *
 * 参考文档: advanced_doc/context-engineering.md
 */
public class ContextEngineeringExample {

    private final ChatModel chatModel;

    public ContextEngineeringExample(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 示例1：基于状态的动态提示
     *
     * 根据对话长度调整系统提示
     */
    public void example1_stateAwarePrompt() throws GraphRunnerException {
        // 创建一个模型拦截器，根据对话长度调整系统提示
        class StateAwarePromptInterceptor extends ModelInterceptor {
            @Override
            public ModelResponse interceptModel(ModelRequest request, ModelCallHandler next) {
                List<Message> messages = request.getMessages();
                int messageCount = messages.size();

                // 基础提示
                String basePrompt = "你是一个有用的助手。";

                // 根据消息数量调整提示
                if (messageCount > 10) {
                    basePrompt += "\n这是一个长对话 - 请尽量保持精准简捷。";
                }

                // 更新系统消息
                List<Message> updatedMessages = updateSystemMessage(messages, basePrompt);

                // 创建新的请求并继续
                ModelRequest updatedRequest = ModelRequest.builder(request)
                    .messages(updatedMessages)
                    .build();

                return next.call(updatedRequest);
            }

            private List<Message> updateSystemMessage(List<Message> messages, String newPrompt) {
                List<Message> updated = new ArrayList<>();
                updated.add(new SystemMessage(newPrompt));
                // 添加非系统消息
                messages.stream()
                    .filter(m -> !(m instanceof SystemMessage))
                    .forEach(updated::add);
                return updated;
            }

            @Override
            public String getName() {
                return "";
            }
        }

        // 使用拦截器创建Agent
        ReactAgent agent = ReactAgent.builder()
                .name("context_aware_agent")
                .model(chatModel)
                .interceptors(new StateAwarePromptInterceptor())
                .build();

        // 测试
        agent.invoke("你好");
        System.out.println("基于状态的动态提示示例执行完成");
    }

    /**
     * 示例2：基于存储的个性化提示
     *
     * 从长期记忆加载用户偏好并生成个性化提示
     */
    public void example2_personalizedPrompt() throws GraphRunnerException {
        // 用户偏好类
        class UserPreferences {
            private String communicationStyle;
            private String language;
            private List<String> interests;

            public UserPreferences(String style, String lang, List<String> interests) {
                this.communicationStyle = style;
                this.language = lang;
                this.interests = interests;
            }

            public String getCommunicationStyle() { return communicationStyle; }
            public String getLanguage() { return language; }
            public List<String> getInterests() { return interests; }
        }

        // 简单的用户偏好存储
        class UserPreferenceStore {
            private Map<String, UserPreferences> store = new HashMap<>();

            public UserPreferences getPreferences(String userId) {
                return store.getOrDefault(userId,
                    new UserPreferences("专业", "中文", List.of()));
            }

            public void savePreferences(String userId, UserPreferences prefs) {
                store.put(userId, prefs);
            }
        }

        UserPreferenceStore store = new UserPreferenceStore();
        store.savePreferences("user_001",
            new UserPreferences("友好轻松", "中文", List.of("技术", "阅读")));

        // 从长期记忆加载用户偏好
        class PersonalizedPromptInterceptor extends ModelInterceptor {
            private final UserPreferenceStore store;

            public PersonalizedPromptInterceptor(UserPreferenceStore store) {
                this.store = store;
            }

            @Override
            public ModelResponse interceptModel(ModelRequest request, ModelCallHandler next) {
                // 从运行时上下文获取用户ID
                String userId = getUserIdFromContext(request);

                // 从存储加载用户偏好
                UserPreferences prefs = store.getPreferences(userId);

                // 构建个性化提示
                String systemPrompt = buildPersonalizedPrompt(prefs);

                // 更新请求
                List<Message> updatedMessages = updateSystemMessage(
                    request.getMessages(),
                    systemPrompt
                );

                ModelRequest updatedRequest = ModelRequest.builder(request)
                    .messages(updatedMessages)
                    .build();

                return next.call(updatedRequest);
            }

            private String getUserIdFromContext(ModelRequest request) {
                // 从请求上下文提取用户ID
                return "user_001"; // 简化示例
            }

            private String buildPersonalizedPrompt(UserPreferences prefs) {
                StringBuilder prompt = new StringBuilder("你是一个有用的助手。");

                if (prefs.getCommunicationStyle() != null) {
                    prompt.append("\n沟通风格：").append(prefs.getCommunicationStyle());
                }

                if (prefs.getLanguage() != null) {
                    prompt.append("\n使用语言：").append(prefs.getLanguage());
                }

                if (!prefs.getInterests().isEmpty()) {
                    prompt.append("\n用户兴趣：").append(String.join(", ", prefs.getInterests()));
                }

                return prompt.toString();
            }

            private List<Message> updateSystemMessage(List<Message> messages, String newPrompt) {
                List<Message> updated = new ArrayList<>();
                updated.add(new SystemMessage(newPrompt));
                messages.stream()
                    .filter(m -> !(m instanceof SystemMessage))
                    .forEach(updated::add);
                return updated;
            }

            @Override
            public String getName() {
                return "PersonalizedPromptInterceptor";
            }
        }

        ReactAgent agent = ReactAgent.builder()
                .name("personalized_agent")
                .model(chatModel)
                .interceptors(new PersonalizedPromptInterceptor(store))
                .build();

        agent.invoke("介绍一下最新的AI技术");
        System.out.println("个性化提示示例执行完成");
    }

    /**
     * 示例3：消息过滤
     *
     * 只保留最近的N条消息，避免上下文过长
     */
    public void example3_messageFilter() {
        class MessageFilterInterceptor extends ModelInterceptor {
            private final int maxMessages;

            public MessageFilterInterceptor(int maxMessages) {
                this.maxMessages = maxMessages;
            }

            @Override
            public ModelResponse interceptModel(ModelRequest request, ModelCallHandler next) {
                List<Message> messages = request.getMessages();

                // 只保留最近的N条消息
                if (messages.size() > maxMessages) {
                    List<Message> filtered = new ArrayList<>();

                    // 添加系统消息
                    messages.stream()
                        .filter(m -> m instanceof SystemMessage)
                        .findFirst()
                        .ifPresent(filtered::add);

                    // 添加最近的消息
                    int startIndex = Math.max(0, messages.size() - maxMessages + 1);
                    filtered.addAll(messages.subList(startIndex, messages.size()));

                    messages = filtered;
                }

                ModelRequest updatedRequest = ModelRequest.builder(request)
                    .messages(messages)
                    .build();

                return next.call(updatedRequest);
            }

            @Override
            public String getName() {
                return "MessageFilterInterceptor";
            }
        }

        ReactAgent agent = ReactAgent.builder()
                .name("message_filter_agent")
                .model(chatModel)
                .interceptors(new MessageFilterInterceptor(10))
                .build();

        System.out.println("消息过滤示例执行完成");
    }

    /**
     * 示例4：基于上下文的工具选择
     *
     * 根据用户角色动态选择可用工具
     */
    public void example4_contextualToolSelection() {
        class ContextualToolInterceptor extends ModelInterceptor {
            private final Map<String, List<ToolCallback>> roleBasedTools;

            public ContextualToolInterceptor(Map<String, List<ToolCallback>> roleBasedTools) {
                this.roleBasedTools = roleBasedTools;
            }

            @Override
            public ModelResponse interceptModel(ModelRequest request, ModelCallHandler next) {
                // 从上下文获取用户角色
                String userRole = getUserRole(request);

                // 根据角色选择工具
                List<ToolCallback> allowedTools = roleBasedTools.getOrDefault(
                    userRole,
                    Collections.emptyList()
                );

                // 更新工具选项（注：实际实现需要根据框架API调整）
                // 这里展示概念性代码
                System.out.println("为角色 " + userRole + " 选择了 " + allowedTools.size() + " 个工具");

                return next.call(request);
            }

            private String getUserRole(ModelRequest request) {
                // 从请求上下文提取用户角色
                return "user"; // 简化示例
            }

            @Override
            public String getName() {
                return "ContextualToolInterceptor";
            }
        }

        // 配置基于角色的工具（示例）
        Map<String, List<ToolCallback>> roleTools = Map.of(
            "admin", List.of(/* readTool, writeTool, deleteTool */),
            "user", List.of(/* readTool */),
            "guest", List.of()
        );

        ReactAgent agent = ReactAgent.builder()
                .name("role_based_agent")
                .model(chatModel)
                .interceptors(new ContextualToolInterceptor(roleTools))
                .build();

        System.out.println("基于上下文的工具选择示例执行完成");
    }

    /**
     * 示例5：日志记录 Hook
     *
     * 使用Hook在模型调用前后记录日志
     */
    public void example5_loggingHook() throws GraphRunnerException {
        class LoggingHook extends ModelHook {
            @Override
            public String getName() {
                return "logging_hook";
            }

            @Override
            public HookPosition[] getHookPositions() {
                return new HookPosition[]{
                    HookPosition.BEFORE_MODEL,
                    HookPosition.AFTER_MODEL
                };
            }

            @Override
            public List<JumpTo> canJumpTo() {
                return List.of();
            }

            @Override
            public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
                // 在模型调用前记录
                List<?> messages = (List<?>) state.value("messages").orElse(List.of());
                System.out.println("模型调用前 - 消息数: " + messages.size());
                return CompletableFuture.completedFuture(Map.of());
            }

            @Override
            public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
                // 在模型调用后记录
                System.out.println("模型调用后 - 响应已生成");
                return CompletableFuture.completedFuture(Map.of());
            }
        }

        // 使用Hook
        ReactAgent agent = ReactAgent.builder()
                .name("logged_agent")
                .model(chatModel)
                .hooks(new LoggingHook())
                .build();

        agent.invoke("测试日志记录");
        System.out.println("日志记录Hook示例执行完成");
    }

    /**
     * 示例6：消息摘要 Hook
     *
     * 当对话过长时自动生成摘要
     */
    public void example6_summarizationHook() {
        class SummarizationHook extends ModelHook {
            private final ChatModel summarizationModel;
            private final int triggerLength;

            public SummarizationHook(ChatModel model, int triggerLength) {
                this.summarizationModel = model;
                this.triggerLength = triggerLength;
            }

            @Override
            public String getName() {
                return "summarization_hook";
            }

            @Override
            public HookPosition[] getHookPositions() {
                return new HookPosition[]{HookPosition.BEFORE_MODEL};
            }

            @Override
            public List<JumpTo> canJumpTo() {
                return List.of();
            }

            @Override
            public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
                List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());

                if (messages.size() > triggerLength) {
                    // 生成对话摘要
                    String summary = generateSummary(messages);

                    // 用摘要替换旧消息
                    List<Message> newMessages = new ArrayList<>();
                    newMessages.add(new SystemMessage("之前对话摘要：" + summary));

                    // 保留最近的几条消息
                    int recentCount = Math.min(5, messages.size());
                    newMessages.addAll(messages.subList(messages.size() - recentCount, messages.size()));

                    return CompletableFuture.completedFuture(Map.of("messages", newMessages));
                }

                return CompletableFuture.completedFuture(Map.of());
            }

            private String generateSummary(List<Message> messages) {
                // 使用另一个模型生成摘要
                String conversation = messages.stream()
                    .map(Message::getText)
                    .collect(Collectors.joining("\n"));

                // 简化示例：返回固定摘要
                return "之前讨论了多个主题...";
            }
        }

        ReactAgent agent = ReactAgent.builder()
                .name("summarizing_agent")
                .model(chatModel)
                .hooks(new SummarizationHook(chatModel, 20))
                .build();

        System.out.println("消息摘要Hook示例执行完成");
    }

    /**
     * 运行所有示例
     */
    public void runAllExamples() {
        System.out.println("=== 上下文工程（Context Engineering）示例 ===\n");

        try {
            System.out.println("示例1: 基于状态的动态提示");
            example1_stateAwarePrompt();
            System.out.println();

            System.out.println("示例2: 基于存储的个性化提示");
            example2_personalizedPrompt();
            System.out.println();

            System.out.println("示例3: 消息过滤");
            example3_messageFilter();
            System.out.println();

            System.out.println("示例4: 基于上下文的工具选择");
            example4_contextualToolSelection();
            System.out.println();

            System.out.println("示例5: 日志记录Hook");
            example5_loggingHook();
            System.out.println();

            System.out.println("示例6: 消息摘要Hook");
            example6_summarizationHook();
            System.out.println();

        } catch (Exception e) {
            System.err.println("执行示例时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main方法：运行所有示例
     *
     * 注意：需要配置ChatModel实例才能运行
     */
    public static void main(String[] args) {
        // TODO: 请配置您的ChatModel实例
        // 例如：ChatModel chatModel = new YourChatModelImplementation();

        ChatModel chatModel = null; // 请替换为实际的ChatModel实例

        if (chatModel == null) {
            System.err.println("错误：请先配置ChatModel实例");
            System.err.println("请修改main方法中的chatModel变量，使用实际的ChatModel实现");
            return;
        }

        // 创建示例实例
        ContextEngineeringExample example = new ContextEngineeringExample(chatModel);

        // 运行所有示例
        example.runAllExamples();
    }
}

