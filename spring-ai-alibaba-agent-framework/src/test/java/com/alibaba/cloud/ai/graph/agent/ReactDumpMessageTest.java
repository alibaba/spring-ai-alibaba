package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.advisors.SkillPromptAugmentAdvisor;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.ReadSkillTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 验证 SequentialAgent 多轮对话时 messages 的累积行为。
 * <p>
 * 流程：用户输入 → 市场分析Agent → 商品搜索Agent → 输出
 * 通过 MessagesModelHook 在每次调用模型前后打印 messages 快照，
 * 观察第二轮对话时 messages 是否出现重复。
 *
 * @author haojun.phj（ Jackie ）
 * @since 2026/3/9
 */
public class ReactDumpMessageTest {
    private ChatModel chatModel;

    @BeforeEach
    public void setUp() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // Create DashScope ChatModel instance
        this.chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
    }

    @Test
    void testSequentialAgentMultiRoundMessages() throws Exception {
        MessageMonitorHook monitorA = new MessageMonitorHook("A-市场分析");
        MessageMonitorHook monitorB = new MessageMonitorHook("B-商品搜索");

        MemorySaver saver = new MemorySaver();

        // 方案一验证：子 Agent 不配 saver，只让 SequentialAgent 管理状态
        ReactAgent agentA = ReactAgent.builder()
                .name("market-analysis")
                .model(chatModel)
                .systemPrompt("无论用户说什么，只回复「市场OK」三个字，不要多说。")
                .hooks(List.of(monitorA))
                .build();

        ReactAgent agentB = ReactAgent.builder()
                .name("product-search")
                .model(chatModel)
                .systemPrompt("无论用户说什么，只回复「商品OK」三个字，不要多说。")
                .hooks(List.of(monitorB))
                .build();

        SequentialAgent pipeline = SequentialAgent.builder()
                .name("pipeline")
                .subAgents(List.of(agentA, agentB))
                .saver(saver)
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("test-1")
                .build();

        // ==================== 第一轮 ====================
        log("第一轮", "AAA");
        Optional<OverAllState> result1 = pipeline.invoke("AAA", config);
        logResult("第一轮", result1);

        // ==================== 第二轮 ====================
        log("第二轮", "BBB");
        Optional<OverAllState> result2 = pipeline.invoke("BBB", config);
        logResult("第二轮", result2);
    }

    // ======================== 日志工具 ========================

    private static void log(String round, String input) {
        System.out.println("\n============================");
        System.out.println(round + " | 用户输入: \"" + input + "\"");
        System.out.println("============================");
    }

    private static void logResult(String round, Optional<OverAllState> result) {
        result.ifPresent(state -> {
            @SuppressWarnings("unchecked")
            List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
            System.out.println("\n--- " + round + " 结束 | messages 共 " + messages.size() + " 条 ---");
            dumpMessages(messages);
            System.out.println("-------------------------------------------");
        });
    }

    private static void dumpMessages(List<Message> messages) {
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            System.out.println("  " + i + ". [" + label(msg) + "] " + brief(msg));
        }
    }

    private static String label(Message msg) {
        if (msg instanceof UserMessage) return "用户";
        if (msg instanceof AssistantMessage) return "助手";
        if (msg instanceof ToolResponseMessage) return "工具";
        return msg.getMessageType().toString();
    }

    private static String brief(Message msg) {
        String text = "";
        if (msg instanceof UserMessage userMessage) {
            text = userMessage.getText();
        } else if (msg instanceof AssistantMessage assistantMessage) {
            text = assistantMessage.getText();
        }
        return text.length() > 60 ? text.substring(0, 60) + "..." : text;
    }

    // ======================== ChatClient + read_skill 流式调用 ========================

    @Test
    void testChatClientStreamWithReadSkill() throws Exception {
        // 1. 构建 SkillComponents（advisor + readSkillToolCallback）
        String skillsDir = getClass().getClassLoader().getResource("skills").getFile();
        SkillComponents skillComponents = buildSkillComponents(skillsDir);

        System.out.println("=== 已加载 " + skillComponents.advisor.getSkillCount() + " 个 Skills ===");
        skillComponents.advisor.listSkills().forEach(skill ->
                System.out.println("  - " + skill.getName() + ": " + skill.getDescription()));

        // 2. 构建 ChatClient，将 SkillPromptAugmentAdvisor 作为 defaultAdvisor
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(skillComponents.advisor)
                .build();

        // 3. 准备消息
        List<Message> messages = List.of(
                new UserMessage("帮我做一个夏季防晒品类的选品分析，并写一句营销文案"),
                new SystemMessage(
                        """
                                # Role
                                你是一个电商场景的资深卖家，擅长分析电商数据，你应当以报告的形式解决用户问题。
                                报告结论要简洁、犀利（不要模棱两可），报告正文要忠于材料，通过精准识别筛选出核心信息，并果断去除所有无关的"噪声"，分析时要综合材料来判断，要避免重复、冗长、不必要的信息。
                                                      
                                # Task
                                1. 根据用户需求判断写作情形，调用 read_skill 工具读取对应写作模板
                                2. 严格遵循写作原则和模板撰写报告，从用户 query 出发灵活调整
                                """
                )
        );

        // 4. 构建流式调用请求
        System.out.println("\n=== 开始流式调用 ===");
        StringBuilder resultBuffer = new StringBuilder();

        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                .messages(messages);

        // 注册 ReadSkillTool，让 LLM 能按需调用 read_skill 读取完整 Skill 内容
        if (skillComponents.readSkillToolCallback != null) {
            requestSpec.toolCallbacks(skillComponents.readSkillToolCallback);
        }

        // 使用 stream().content() 模式，Spring AI 自动处理 tool call
        requestSpec
                .stream()
                .content()
                .doOnNext(chunk -> {
                    System.out.print(chunk);
                    resultBuffer.append(chunk);
                })
                .doOnComplete(() -> {
                    System.out.println("\n\n=== 流式调用完成 ===");
                    System.out.println("最终结果: " + resultBuffer);
                })
                .blockLast();
    }

    /**
     * 构建 SkillComponents，包含 SkillPromptAugmentAdvisor 和 read_skill ToolCallback。
     * 先显式创建 SkillRegistry，再共享给 advisor 和 ReadSkillTool，确保两者使用同一实例。
     */
    private SkillComponents buildSkillComponents(String skillsDirectory) {
        // 1. 显式创建 SkillRegistry，加载指定目录下的 skills
        SkillRegistry skillRegistry = FileSystemSkillRegistry.builder()
                .projectSkillsDirectory(skillsDirectory)
                .build();

        // 2. 将同一个 SkillRegistry 注入 advisor，用于注入 Skill 摘要到 System Prompt
        SkillPromptAugmentAdvisor advisor = SkillPromptAugmentAdvisor.builder()
                .skillRegistry(skillRegistry)
                .build();

        // 3. 用同一个 SkillRegistry 创建 read_skill 工具，确保 LLM 能读取到相同的 skills
        ToolCallback rawReadSkillTool = ReadSkillTool.createReadSkillToolCallback(skillRegistry, null);
        ToolCallback readSkillTool = new LoggingToolCallbackWrapper(rawReadSkillTool);

        return new SkillComponents(advisor, readSkillTool);
    }

    /**
     * 封装 Skill 相关组件：advisor 用于注入 Skill 摘要到 System Prompt，
     * readSkillToolCallback 用于 LLM 按需读取完整 Skill 内容。
     */
    static class SkillComponents {

        final SkillPromptAugmentAdvisor advisor;

        final ToolCallback readSkillToolCallback;

        SkillComponents(SkillPromptAugmentAdvisor advisor, ToolCallback readSkillToolCallback) {
            this.advisor = advisor;
            this.readSkillToolCallback = readSkillToolCallback;
        }
    }

    /**
     * 包装 ToolCallback，在调用前后打印清晰的日志。
     * 运行测试时只需在控制台搜索 "🔧" 即可快速定位 read_skill 的调用。
     */
    static class LoggingToolCallbackWrapper implements ToolCallback {

        private final ToolCallback delegate;

        LoggingToolCallbackWrapper(ToolCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public String call(String toolInput) {
            System.out.println("\n🔧🔧🔧 read_skill 被调用！输入: " + toolInput);
            String result = delegate.call(toolInput);
            String preview = result.length() > 200 ? result.substring(0, 200) + "..." : result;
            System.out.println("🔧🔧🔧 read_skill 返回内容（前200字）: " + preview);
            System.out.println("🔧🔧🔧 返回内容总长度: " + result.length() + " 字符\n");
            return result;
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            System.out.println("\n🔧🔧🔧 read_skill 被调用！输入: " + toolInput);
            String result = delegate.call(toolInput, toolContext);
            String preview = result.length() > 200 ? result.substring(0, 200) + "..." : result;
            System.out.println("🔧🔧🔧 read_skill 返回内容（前200字）: " + preview);
            System.out.println("🔧🔧🔧 返回内容总长度: " + result.length() + " 字符\n");
            return result;
        }
    }

    // ======================== Hook ========================

    @HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
    static class MessageMonitorHook extends MessagesModelHook {

        private final String tag;

        MessageMonitorHook(String tag) {
            this.tag = tag;
        }

        @Override
        public AgentCommand beforeModel(List<Message> messages, RunnableConfig config) {
            System.out.println("\n  >> " + tag + " 调用模型前 | " + messages.size() + " 条消息");
            dumpMessages(messages);
            return new AgentCommand(messages);
        }

        @Override
        public AgentCommand afterModel(List<Message> messages, RunnableConfig config) {
            System.out.println("\n  << " + tag + " 模型返回后 | " + messages.size() + " 条消息");
            dumpMessages(messages);
            return new AgentCommand(messages);
        }

        @Override
        public String getName() {
            return tag;
        }
    }
}
