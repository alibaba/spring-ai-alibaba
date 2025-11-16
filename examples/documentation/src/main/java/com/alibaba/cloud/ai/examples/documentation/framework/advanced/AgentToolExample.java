/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.documentation.framework.advanced;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Optional;

/**
 * 智能体作为工具（Agent Tool）示例
 *
 * 演示 Multi-agent 工具调用模式，包括：
 * 1. 将子Agent作为工具使用
 * 2. 自定义输入和输出Schema
 * 3. 类型化的Agent工具调用
 * 4. 完整的工具调用示例
 *
 * 参考文档: advanced_doc/agent-tool.md
 */
public class AgentToolExample {

	private final ChatModel chatModel;

	public AgentToolExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * Main方法：运行所有示例
	 *
	 * 注意：需要配置ChatModel实例才能运行
	 */
	public static void main(String[] args) {
		// 创建 DashScope API 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// 创建 ChatModel
		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		if (chatModel == null) {
			System.err.println("错误：请先配置ChatModel实例");
			System.err.println("请设置 AI_DASHSCOPE_API_KEY 环境变量");
			return;
		}

		// 创建示例实例
		AgentToolExample example = new AgentToolExample(chatModel);

		// 运行所有示例
		example.runAllExamples();
	}

	/**
	 * 示例1：基础 Agent Tool 调用
	 *
	 * 主Agent将子Agent作为工具调用，子Agent执行特定任务并返回结果
	 */
	public void example1_basicAgentTool() throws GraphRunnerException {
		// 创建子Agent - 作为工具使用
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

		if (result.isPresent()) {
			System.out.println("文章生成成功");
			// 处理结果
		}
	}

	/**
	 * 示例2：使用 inputSchema 控制子Agent的输入
	 *
	 * 通过定义输入Schema，使子Agent能够接收结构化的输入信息
	 */
	public void example2_agentToolWithInputSchema() throws GraphRunnerException {
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

		if (result.isPresent()) {
			System.out.println("结构化输入示例执行成功");
		}
	}

	/**
	 * 示例3：使用 inputType 定义类型化输入
	 *
	 * 使用 Java 类型定义输入，框架会自动生成 JSON Schema
	 */
	public void example3_agentToolWithInputType() throws GraphRunnerException {
		// 定义输入类型
		record ArticleRequest(String topic, int wordCount, String style) { }

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

		if (result.isPresent()) {
			System.out.println("类型化输入示例执行成功");
		}
	}

	/**
	 * 示例4：使用 outputSchema 控制子Agent的输出
	 *
	 * 定义输出Schema，使子Agent返回结构化的输出格式
	 */
	public void example4_agentToolWithOutputSchema() throws GraphRunnerException {
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

		if (result.isPresent()) {
			System.out.println("结构化输出示例执行成功");
		}
	}

	/**
	 * 示例5：使用 outputType 定义类型化输出
	 *
	 * 使用 Java 类型定义输出，框架会自动生成输出 schema
	 */
	public void example5_agentToolWithOutputType() throws GraphRunnerException {
		// 定义输出类型
		class ArticleOutput {
			private String title;
			private String content;
			private int characterCount;

			// getters and setters
			public String getTitle() {
				return title;
			}

			public String getContent() {
				return content;
			}			public void setTitle(String title) {
				this.title = title;
			}

			public int getCharacterCount() {
				return characterCount;
			}

			public void setContent(String content) {
				this.content = content;
			}



			public void setCharacterCount(int characterCount) {
				this.characterCount = characterCount;
			}
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

		if (result.isPresent()) {
			System.out.println("类型化输出示例执行成功");
		}
	}

	/**
	 * 示例6：完整类型化示例
	 *
	 * 同时使用 inputType 和 outputType 进行完整的类型化Agent工具调用
	 */
	public void example6_fullTypedAgentTool() throws GraphRunnerException {
		// 定义输入和输出类型
		record ArticleRequest(String topic, int wordCount, String style) { }

		class ArticleOutput {
			private String title;
			private String content;
			private int characterCount;

			public String getTitle() {
				return title;
			}

			public String getContent() {
				return content;
			}			public void setTitle(String title) {
				this.title = title;
			}

			public int getCharacterCount() {
				return characterCount;
			}

			public void setContent(String content) {
				this.content = content;
			}



			public void setCharacterCount(int characterCount) {
				this.characterCount = characterCount;
			}
		}

		class ReviewOutput {
			private String comment;
			private boolean approved;
			private List<String> suggestions;

			public String getComment() {
				return comment;
			}

			public void setComment(String comment) {
				this.comment = comment;
			}

			public boolean isApproved() {
				return approved;
			}

			public void setApproved(boolean approved) {
				this.approved = approved;
			}

			public List<String> getSuggestions() {
				return suggestions;
			}

			public void setSuggestions(List<String> suggestions) {
				this.suggestions = suggestions;
			}
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
				.instruction("对文章进行评审，返回评审意见（comment、approved、suggestions）。")
				.outputType(ReviewOutput.class)
				.build();

		ReactAgent orchestratorAgent = ReactAgent.builder()
				.name("orchestrator")
				.model(chatModel)
				.instruction("协调写作和评审流程。先调用写作工具创作文章，然后调用评审工具进行评审。")
				.tools(
						AgentTool.getFunctionToolCallback(writerAgent),
						AgentTool.getFunctionToolCallback(reviewerAgent)
				)
				.build();

		Optional<OverAllState> result = orchestratorAgent.invoke("请写一篇关于友谊的散文，约200字，需要评审");

		if (result.isPresent()) {
			System.out.println("完整类型化示例执行成功");
		}
	}

	/**
	 * 示例7：多个子Agent作为工具
	 *
	 * 主Agent可以访问多个不同的子Agent工具，根据需要调用
	 */
	public void example7_multipleAgentTools() throws GraphRunnerException {
		// 创建写作Agent
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("专门负责创作文章和内容生成")
				.instruction("你是一个专业作家，擅长各类文章创作。")
				.build();

		// 创建翻译Agent
		ReactAgent translatorAgent = ReactAgent.builder()
				.name("translator_agent")
				.model(chatModel)
				.description("专门负责文本翻译工作")
				.instruction("你是一个专业翻译，能够准确翻译多种语言。")
				.build();

		// 创建总结Agent
		ReactAgent summarizerAgent = ReactAgent.builder()
				.name("summarizer_agent")
				.model(chatModel)
				.description("专门负责内容总结和提炼")
				.instruction("你是一个内容总结专家，擅长提炼关键信息。")
				.build();

		// 创建主Agent，集成多个工具
		ReactAgent multiToolAgent = ReactAgent.builder()
				.name("multi_tool_coordinator")
				.model(chatModel)
				.instruction("你可以访问多个专业工具：写作、翻译和总结。" +
						"根据用户需求选择合适的工具来完成任务。")
				.tools(
						AgentTool.getFunctionToolCallback(writerAgent),
						AgentTool.getFunctionToolCallback(translatorAgent),
						AgentTool.getFunctionToolCallback(summarizerAgent)
				)
				.build();

		// 测试不同的请求
		multiToolAgent.invoke("请写一篇关于AI的文章，然后翻译成英文，最后给出摘要");

		System.out.println("多工具Agent示例执行成功");
	}

	/**
	 * 运行所有示例
	 */
	public void runAllExamples() {
		System.out.println("=== 智能体作为工具（Agent Tool）示例 ===\n");

		try {
			System.out.println("示例1: 基础 Agent Tool 调用");
			example1_basicAgentTool();
			System.out.println();

			System.out.println("示例2: 使用 inputSchema 控制输入");
			example2_agentToolWithInputSchema();
			System.out.println();

			System.out.println("示例3: 使用 inputType 定义类型化输入");
			example3_agentToolWithInputType();
			System.out.println();

			System.out.println("示例4: 使用 outputSchema 控制输出");
			example4_agentToolWithOutputSchema();
			System.out.println();

			System.out.println("示例5: 使用 outputType 定义类型化输出");
			example5_agentToolWithOutputType();
			System.out.println();

			System.out.println("示例6: 完整类型化示例");
			example6_fullTypedAgentTool();
			System.out.println();

			System.out.println("示例7: 多个子Agent作为工具");
			example7_multipleAgentTools();
			System.out.println();

		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

