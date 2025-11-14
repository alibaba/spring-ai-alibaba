package com.alibaba.cloud.ai.graph.agent.documentation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.tool.AgentTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Optional;

/**
 * Agent Tool Advanced Example - 完整代码示例
 * 展示如何将智能体作为工具使用，实现Multi-agent协作
 *
 * 来源：advanced/agent-tool.md
 */
public class AgentToolExample {

	// ==================== 基础示例 ====================

	/**
	 * 示例1：最小Agent作为工具示例
	 */
	public static void basicAgentTool() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 创建子Agent
		ReactAgent writerAgent = ReactAgent.builder()
			.name("writer_agent")
			.model(chatModel)
			.description("可以写文章")
			.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
			.build();

		// 创建主Agent，将子Agent作为工具
		ReactAgent blogAgent = ReactAgent.builder()
			.name("blog_agent")
			.model(chatModel)
			.instruction("根据用户给定的主题写一篇文章。使用写作工具来完成任务。")
			.tools(AgentTool.getFunctionToolCallback(writerAgent))
			.build();

		// 使用
		Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的散文");
	}

	// ==================== 输入控制 ====================

	/**
	 * 示例2：使用 inputSchema 控制子Agent输入
	 */
	public static void inputSchemaControl() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 定义子Agent的输入Schema
		String writerInputSchema = """
			{
				"topic": "文章主题",
				"wordCount": "字数要求（整数）",
				"style": "文章风格（如：散文、诗歌等）"
			}
			""";

		ReactAgent writerAgent = ReactAgent.builder()
			.name("structured_writer_agent")
			.model(chatModel)
			.description("根据结构化输入写文章")
			.instruction("你是一个专业作家。请严格按照输入的主题、字数和风格要求创作文章。")
			.inputSchema(writerInputSchema)
			.build();

		ReactAgent coordinatorAgent = ReactAgent.builder()
			.name("coordinator_agent")
			.model(chatModel)
			.instruction("你需要调用写作工具来完成用户的写作请求。请根据用户需求，使用结构化的参数调用写作工具。")
			.tools(AgentTool.getFunctionToolCallback(writerAgent))
			.build();

		Optional<OverAllState> result = coordinatorAgent.invoke("请写一篇关于春天的散文，大约150字");
	}

	/**
	 * 示例3：使用 inputType 进行类型化输入
	 */
	public static void inputTypeControl() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 定义输入类型
		record ArticleRequest(
			String topic,      // 文章主题
			int wordCount,     // 字数要求
			String style       // 文章风格
		) {}

		ReactAgent writerAgent = ReactAgent.builder()
			.name("typed_writer_agent")
			.model(chatModel)
			.description("根据类型化输入写文章")
			.instruction("你是一个专业作家。请严格按照输入的 topic（主题）、wordCount（字数）和 style（风格）要求创作文章。")
			.inputType(ArticleRequest.class)
			.build();

		ReactAgent coordinatorAgent = ReactAgent.builder()
			.name("coordinator_with_type_agent")
			.model(chatModel)
			.instruction("你需要调用写作工具来完成用户的写作请求。工具接收 JSON 格式的参数。")
			.tools(AgentTool.getFunctionToolCallback(writerAgent))
			.build();

		Optional<OverAllState> result = coordinatorAgent.invoke("请写一篇关于秋天的现代诗，大约100字");
	}

	// ==================== 输出控制 ====================

	/**
	 * 示例4：使用 outputSchema 控制子Agent输出
	 */
	public static void outputSchemaControl() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 定义输出Schema
		String writerOutputSchema = """
			请按照以下JSON格式返回：
			{
				"title": "文章标题",
				"content": "文章正文内容",
				"characterCount": "文章字符数（整数）"
			}
			""";

		ReactAgent writerAgent = ReactAgent.builder()
			.name("writer_with_output_schema")
			.model(chatModel)
			.description("写文章并返回结构化输出")
			.instruction("你是一个专业作家。请创作文章并严格按照指定的JSON格式返回结果。")
			.outputSchema(writerOutputSchema)
			.build();

		ReactAgent coordinatorAgent = ReactAgent.builder()
			.name("coordinator_output_schema")
			.model(chatModel)
			.instruction("调用写作工具完成用户请求，工具会返回结构化的文章数据。")
			.tools(AgentTool.getFunctionToolCallback(writerAgent))
			.build();

		Optional<OverAllState> result = coordinatorAgent.invoke("写一篇关于冬天的短文");
	}

	/**
	 * 示例5：使用 outputType 进行类型化输出
	 */
	public static void outputTypeControl() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 定义输出类型
		class ArticleOutput {
			private String title;
			private String content;
			private int characterCount;

			// getters and setters
			public String getTitle() { return title; }
			public void setTitle(String title) { this.title = title; }
			public String getContent() { return content; }
			public void setContent(String content) { this.content = content; }
			public int getCharacterCount() { return characterCount; }
			public void setCharacterCount(int characterCount) { this.characterCount = characterCount; }
		}

		ReactAgent writerAgent = ReactAgent.builder()
			.name("writer_with_output_type")
			.model(chatModel)
			.description("写文章并返回类型化输出")
			.instruction("你是一个专业作家。请创作文章并返回包含 title、content 和 characterCount 的结构化结果。")
			.outputType(ArticleOutput.class)
			.build();

		ReactAgent coordinatorAgent = ReactAgent.builder()
			.name("coordinator_output_type")
			.model(chatModel)
			.instruction("调用写作工具完成用户请求。")
			.tools(AgentTool.getFunctionToolCallback(writerAgent))
			.build();

		Optional<OverAllState> result = coordinatorAgent.invoke("写一篇关于夏天的小诗");
	}

	// ==================== 完整类型化示例 ====================

	/**
	 * 示例6：同时使用 inputType 和 outputType
	 */
	public static void fullTypedExample() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 定义输入和输出类型
		record ArticleRequest(String topic, int wordCount, String style) {}

		class ArticleOutput {
			private String title;
			private String content;
			private int characterCount;
			// getters and setters...
		}

		class ReviewOutput {
			private String comment;
			private boolean approved;
			// getters and setters...
		}

		// 创建完整类型化的Agent
		ReactAgent writerAgent = ReactAgent.builder()
			.name("full_typed_writer")
			.model(chatModel)
			.description("完整类型化的写作工具")
			.instruction("根据结构化输入（topic、wordCount、style）创作文章，并返回结构化输出（title、content、characterCount）。")
			.inputType(ArticleRequest.class)
			.outputType(ArticleOutput.class)
			.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
			.name("typed_reviewer")
			.model(chatModel)
			.description("完整类型化的评审工具")
			.instruction("对文章进行评审，返回评审意见（comment、approved）。")
			.outputType(ReviewOutput.class)
			.build();

		ReactAgent orchestratorAgent = ReactAgent.builder()
			.name("orchestrator")
			.model(chatModel)
			.instruction("协调写作和评审流程，确保文章质量。")
			.tools(
				AgentTool.getFunctionToolCallback(writerAgent),
				AgentTool.getFunctionToolCallback(reviewerAgent)
			)
			.saver(new MemorySaver())
			.build();

		Optional<OverAllState> result = orchestratorAgent.invoke("创作并评审一篇关于春天的文章");
	}

	// ==================== Main 方法 ====================

	public static void main(String[] args) {
		System.out.println("=== Agent Tool Advanced Examples ===");

		// 运行示例（需要设置 AI_DASHSCOPE_API_KEY 环境变量）
		// basicAgentTool();
		// inputSchemaControl();
		// inputTypeControl();
		// outputSchemaControl();
		// outputTypeControl();
		// fullTypedExample();
	}
}

