package com.alibaba.cloud.ai.graph.agent.documentation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Messages Tutorial - 完整代码示例
 * 展示Messages作为模型交互的基本单元的使用方法
 *
 * 来源：messages.md
 */
public class MessagesExample {

	// ==================== 基础使用 ====================

	/**
	 * 示例1：基础消息使用
	 */
	public static void basicMessageUsage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		// 使用 DashScope ChatModel
		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		SystemMessage systemMsg = new SystemMessage("你是一个有帮助的助手。");
		UserMessage userMsg = new UserMessage("你好，你好吗？");

		// 与聊天模型一起使用
		List<Message> messages = List.of(systemMsg, userMsg);
		Prompt prompt = new Prompt(messages);
		ChatResponse response = chatModel.call(prompt);  // 返回 ChatResponse，包含 AssistantMessage
	}

	// ==================== 文本提示 vs 消息提示 ====================

	/**
	 * 示例2：文本提示
	 */
	public static void textPromptUsage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 使用字符串直接调用
		String response = chatModel.call("写一首关于春天的俳句");
	}

	/**
	 * 示例3：消息提示
	 */
	public static void messagePromptUsage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		List<Message> messages = List.of(
			new SystemMessage("你是一个诗歌专家"),
			new UserMessage("写一首关于春天的俳句"),
			new AssistantMessage("樱花盛开时...")
		);
		Prompt prompt = new Prompt(messages);
		ChatResponse response = chatModel.call(prompt);
	}

	// ==================== System Message ====================

	/**
	 * 示例4：基础指令
	 */
	public static void basicSystemMessage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 基础指令
		SystemMessage systemMsg = new SystemMessage("你是一个有帮助的编程助手。");

		List<Message> messages = List.of(
			systemMsg,
			new UserMessage("如何创建 REST API？")
		);
		ChatResponse response = chatModel.call(new Prompt(messages));
	}

	/**
	 * 示例5：详细的角色设定
	 */
	public static void detailedSystemMessage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 详细的角色设定
		SystemMessage systemMsg = new SystemMessage("""
			你是一位资深的 Java 开发者，擅长 Web 框架。
			始终提供代码示例并解释你的推理。
			在解释中要简洁但透彻。
			""");

		List<Message> messages = List.of(
			systemMsg,
			new UserMessage("如何创建 REST API？")
		);
		ChatResponse response = chatModel.call(new Prompt(messages));
	}

	// ==================== User Message ====================

	/**
	 * 示例6：文本内容
	 */
	public static void textUserMessage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 使用消息对象
		ChatResponse response = chatModel.call(
			new Prompt(List.of(new UserMessage("什么是机器学习？")))
		);

		// 使用字符串快捷方式
		// 使用字符串是单个 UserMessage 的快捷方式
		String response2 = chatModel.call("什么是机器学习？");
	}

	/**
	 * 示例7：消息元数据
	 */
	public static void userMessageMetadata() {
		UserMessage userMsg = UserMessage.builder()
			.text("你好！")
			.metadata(Map.of(
				"user_id", "alice",  // 可选：识别不同用户
				"session_id", "sess_123"  // 可选：会话标识符
			))
			.build();
	}

	/**
	 * 示例8：多模态内容 - 图像
	 */
	public static void multimodalImageMessage() throws Exception {
		// 从 URL 创建图像
		UserMessage userMsg = UserMessage.builder()
			.text("描述这张图片的内容。")
			.media(new Media(
				MimeTypeUtils.IMAGE_JPEG,
				new URL("https://example.com/image.jpg")
			))
			.build();
	}

	// ==================== Assistant Message ====================

	/**
	 * 示例9：Assistant Message 基础使用
	 */
	public static void basicAssistantMessage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		ChatResponse response = chatModel.call(new Prompt("解释 AI"));
		AssistantMessage aiMessage = response.getResult().getOutput();
		System.out.println(aiMessage.getText());
	}

	/**
	 * 示例10：手动创建 AI 消息
	 */
	public static void manualAssistantMessage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 手动创建 AI 消息（例如，用于对话历史）
		AssistantMessage aiMsg = new AssistantMessage("我很乐意帮助你回答这个问题！");

		// 添加到对话历史
		List<Message> messages = List.of(
			new SystemMessage("你是一个有帮助的助手"),
			new UserMessage("你能帮我吗？"),
			aiMsg,  // 插入，就像它来自模型一样
			new UserMessage("太好了！2+2 等于多少？")
		);

		ChatResponse response = chatModel.call(new Prompt(messages));
	}

	/**
	 * 示例11：工具调用
	 */
	public static void toolCallsInAssistantMessage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		Prompt prompt = new Prompt("北京的天气怎么样？");
		ChatResponse response = chatModel.call(prompt);
		AssistantMessage aiMessage = response.getResult().getOutput();

		if (aiMessage.hasToolCalls()) {
			for (AssistantMessage.ToolCall toolCall : aiMessage.getToolCalls()) {
				System.out.println("Tool: " + toolCall.name());
				System.out.println("Args: " + toolCall.arguments());
				System.out.println("ID: " + toolCall.id());
			}
		}
	}

	/**
	 * 示例12：Token 使用
	 */
	public static void tokenUsage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		ChatResponse response = chatModel.call(new Prompt("你好！"));
		ChatResponseMetadata metadata = response.getMetadata();

		// 访问使用信息
		if (metadata != null && metadata.getUsage() != null) {
			System.out.println("Input tokens: " + metadata.getUsage().getPromptTokens());
			System.out.println("Output tokens: " + metadata.getUsage().getGenerationTokens());
			System.out.println("Total tokens: " + metadata.getUsage().getTotalTokens());
		}
	}

	/**
	 * 示例13：流式和块
	 */
	public static void streamingMessages() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		Flux<ChatResponse> responseStream = chatModel.stream(new Prompt("你好"));

		StringBuilder fullResponse = new StringBuilder();
		responseStream.subscribe(
			chunk -> {
				String content = chunk.getResult().getOutput().getText();
				fullResponse.append(content);
				System.out.print(content);
			}
		);
	}

	// ==================== Tool Response Message ====================

	/**
	 * 示例14：Tool Response Message
	 */
	public static void toolResponseMessage() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 在模型进行工具调用后
		AssistantMessage aiMessage = AssistantMessage.builder()
			.text("")
			.toolCalls(List.of(
				new AssistantMessage.ToolCall(
					"call_123",
					"tool",
					"get_weather",
					"{\"location\": \"San Francisco\"}"
				)
			))
			.build();

		// 执行工具并创建结果消息
		String weatherResult = "晴朗，22°C";
		ToolResponseMessage toolMessage = ToolResponseMessage.builder()
			.responses(List.of(
				new ToolResponseMessage.ToolResponse("call_123", "get_weather", weatherResult)
			))
			.build();

		// 继续对话
		List<Message> messages = List.of(
			new UserMessage("旧金山的天气怎么样？"),
			aiMessage,      // 模型的工具调用
			toolMessage     // 工具执行结果
		);
		ChatResponse response = chatModel.call(new Prompt(messages));
	}

	// ==================== 多模态内容 ====================

	/**
	 * 示例15：图像输入 - 从 URL
	 */
	public static void imageInputFromURL() throws Exception {
		// 从 URL
		UserMessage message = UserMessage.builder()
			.text("描述这张图片的内容。")
			.media(new Media(
				MimeTypeUtils.IMAGE_JPEG,
				new URL("https://example.com/path/to/image.jpg")
			))
			.build();
	}

	/**
	 * 示例16：图像输入 - 从本地文件
	 */
	public static void imageInputFromFile() {
		// 从本地文件
		UserMessage message = UserMessage.builder()
			.text("描述这张图片的内容。")
			.media(new Media(
				MimeTypeUtils.IMAGE_JPEG,
				new ClassPathResource("images/photo.jpg")
			))
			.build();
	}

	/**
	 * 示例17：音频输入
	 */
	public static void audioInput() {
		UserMessage message = UserMessage.builder()
			.text("描述这段音频的内容。")
			.media(new Media(
				MimeTypeUtils.parseMimeType("audio/wav"),
				new ClassPathResource("audio/recording.wav")
			))
			.build();
	}

	/**
	 * 示例18：视频输入
	 */
	public static void videoInput() throws Exception {
		UserMessage message = UserMessage.builder()
			.text("描述这段视频的内容。")
			.media(new Media(
				MimeTypeUtils.parseMimeType("video/mp4"),
				new URL("https://example.com/path/to/video.mp4")
			))
			.build();
	}

	// ==================== 与 Chat Models 一起使用 ====================

	/**
	 * 示例19：基础对话示例
	 */
	public static void basicConversationExample() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		List<Message> conversationHistory = new ArrayList<>();

		// 第一轮对话
		conversationHistory.add(new UserMessage("你好！"));
		ChatResponse response1 = chatModel.call(new Prompt(conversationHistory));
		conversationHistory.add(response1.getResult().getOutput());

		// 第二轮对话
		conversationHistory.add(new UserMessage("你能帮我学习 Java 吗？"));
		ChatResponse response2 = chatModel.call(new Prompt(conversationHistory));
		conversationHistory.add(response2.getResult().getOutput());

		// 第三轮对话
		conversationHistory.add(new UserMessage("从哪里开始？"));
		ChatResponse response3 = chatModel.call(new Prompt(conversationHistory));
	}

	/**
	 * 示例20：使用 Builder 模式
	 */
	public static void builderPattern() {
		// UserMessage with builder
		UserMessage userMsg = UserMessage.builder()
			.text("你好，我想学习 Spring AI Alibaba")
			.metadata(Map.of("user_id", "user_123"))
			.build();

		// SystemMessage with builder
		SystemMessage systemMsg = SystemMessage.builder()
			.text("你是一个 Spring 框架专家")
			.metadata(Map.of("version", "1.0"))
			.build();

		// AssistantMessage with builder
		AssistantMessage assistantMsg = AssistantMessage.builder()
			.text("我很乐意帮助你学习 Spring AI Alibaba！")
			.build();
	}

	/**
	 * 示例21：消息复制和修改
	 */
	public static void messageCopyAndModify() {
		// 复制消息
		UserMessage original = new UserMessage("原始消息");
		UserMessage copy = original.copy();

		// 使用 mutate 创建修改的副本
		UserMessage modified = original.mutate()
			.text("修改后的消息")
			.metadata(Map.of("modified", true))
			.build();
	}

	// ==================== 在 ReactAgent 中使用 ====================

	/**
	 * 示例22：在 ReactAgent 中使用消息
	 */
	public static void messagesInReactAgent() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("my_agent")
			.model(chatModel)
			.systemPrompt("你是一个有帮助的助手")
			.build();

		// 使用字符串
		AssistantMessage response1 = agent.call("你好");

		// 使用 UserMessage
		UserMessage userMsg = new UserMessage("帮我写一首诗");
		AssistantMessage response2 = agent.call(userMsg);

		// 使用消息列表
		List<Message> messages = List.of(
			new UserMessage("我喜欢春天"),
			new UserMessage("写一首关于春天的诗")
		);
		AssistantMessage response3 = agent.call(messages);
	}

	// ==================== Main 方法 ====================

	public static void main(String[] args) {
		System.out.println("=== Messages Tutorial Examples ===");

		// 运行示例（需要设置 AI_DASHSCOPE_API_KEY 环境变量）
		// basicMessageUsage();
		// textPromptUsage();
		// messagePromptUsage();
		// basicSystemMessage();
		// textUserMessage();
		// basicAssistantMessage();
		// tokenUsage();
	}
}

