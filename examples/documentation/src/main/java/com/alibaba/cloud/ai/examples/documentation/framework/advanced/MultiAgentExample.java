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
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 多智能体（Multi-agent）示例
 *
 * 演示不同的 Multi-agent 协作模式，包括：
 * 1. 顺序执行（Sequential Agent）
 * 2. 并行执行（Parallel Agent）
 * 3. LLM路由（LlmRoutingAgent）
 * 4. 自定义合并策略
 *
 * 参考文档: advanced_doc/multi-agent.md
 */
public class MultiAgentExample {

	private final ChatModel chatModel;

	public MultiAgentExample(ChatModel chatModel) {
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
		MultiAgentExample example = new MultiAgentExample(chatModel);

		// 运行所有示例
		example.runAllExamples();
	}

	/**
	 * 示例1：顺序执行（Sequential Agent）
	 *
	 * 多个Agent按预定义的顺序依次执行，每个Agent的输出成为下一个Agent的输入
	 */
	public void example1_sequentialAgent() throws Exception {
		// 创建专业化的子Agent
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("专业写作Agent")
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
				.outputKey("article")
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.description("专业评审Agent")
				.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。" +
						"对于散文类文章，请确保文章中必须包含对于西湖风景的描述。" +
						"最终只返回修改后的文章，不要包含任何评论信息。")
				.outputKey("reviewed_article")
				.build();

		// 创建顺序Agent
		SequentialAgent blogAgent = SequentialAgent.builder()
				.name("blog_agent")
				.description("根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论")
				.subAgents(List.of(writerAgent, reviewerAgent))
				.build();

		// 使用
		Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的散文");

		if (result.isPresent()) {
			OverAllState state = result.get();

			// 访问第一个Agent的输出
			state.value("article").ifPresent(article -> {
				if (article instanceof AssistantMessage) {
					System.out.println("原始文章: " + ((AssistantMessage) article).getText());
				}
			});

			// 访问第二个Agent的输出
			state.value("reviewed_article").ifPresent(reviewedArticle -> {
				if (reviewedArticle instanceof AssistantMessage) {
					System.out.println("评审后文章: " + ((AssistantMessage) reviewedArticle).getText());
				}
			});
		}
	}

	/**
	 * 示例2：控制推理内容
	 *
	 * 使用 returnReasoningContents 控制是否在消息历史中包含中间推理
	 */
	public void example2_controlReasoningContents() throws Exception {
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.returnReasoningContents(true)  // 返回推理过程
				.outputKey("article")
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.includeContents(true) // 包含上一个Agent的推理内容
				.returnReasoningContents(true)  // 返回推理过程
				.outputKey("reviewed_article")
				.build();


		// 每个子agent的推理内容，下一个执行的子agent会看到上一个子agent的推理内容
		SequentialAgent blogAgent = SequentialAgent.builder()
				.name("blog_agent")
				.subAgents(List.of(writerAgent, reviewerAgent))
				.build();

		Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的散文");

		if (result.isPresent()) {
			// 消息历史将包含所有工具调用和推理过程
			List<Message> messages = (List<Message>) result.get().value("messages").orElse(List.of());
			System.out.println("消息数量: " + messages.size()); // 包含所有中间步骤
		}
	}

	/**
	 * 示例3：并行执行（Parallel Agent）
	 *
	 * 多个Agent同时处理相同的输入，它们的结果被收集并合并
	 */
	public void example3_parallelAgent() throws Exception {
		// 创建多个专业化Agent
		ReactAgent proseWriterAgent = ReactAgent.builder()
				.name("prose_writer_agent")
				.model(chatModel)
				.description("专门写散文的AI助手")
				.instruction("你是一个知名的散文作家，擅长写优美的散文。" +
						"用户会给你一个主题，你只需要创作一篇100字左右的散文。")
				.outputKey("prose_result")
				.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
				.name("poem_writer_agent")
				.model(chatModel)
				.description("专门写现代诗的AI助手")
				.instruction("你是一个知名的现代诗人，擅长写现代诗。" +
						"用户会给你一个主题，你只需要创作一首现代诗。")
				.outputKey("poem_result")
				.build();

		ReactAgent summaryAgent = ReactAgent.builder()
				.name("summary_agent")
				.model(chatModel)
				.description("专门做内容总结的AI助手")
				.instruction("你是一个专业的内容分析师，擅长对主题进行总结和提炼。" +
						"用户会给你一个主题，你只需要对这个主题进行简要总结。")
				.outputKey("summary_result")
				.build();

		// 创建并行Agent
		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_creative_agent")
				.description("并行执行多个创作任务，包括写散文、写诗和做总结")
				.mergeOutputKey("merged_results")
				.subAgents(List.of(proseWriterAgent, poemWriterAgent, summaryAgent))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
				.build();

		// 使用
		Optional<OverAllState> result = parallelAgent.invoke("以'西湖'为主题");

		if (result.isPresent()) {
			OverAllState state = result.get();

			// 访问各个Agent的输出
			state.value("prose_result").ifPresent(r ->
					System.out.println("散文: " + r));
			state.value("poem_result").ifPresent(r ->
					System.out.println("诗歌: " + r));
			state.value("summary_result").ifPresent(r ->
					System.out.println("总结: " + r));

			// 访问合并后的结果
			state.value("merged_results").ifPresent(r ->
					System.out.println("合并结果: " + r));
		}
	}

	/**
	 * 示例4：自定义合并策略
	 *
	 * 实现自定义的合并策略来控制如何组合多个Agent的输出
	 */
	public void example4_customMergeStrategy() throws Exception {
		// 自定义合并策略
		class CustomMergeStrategy implements ParallelAgent.MergeStrategy {
			@Override
			public Map<String, Object> merge(Map<String, Object> mergedState, OverAllState state) {
				// 从每个Agent的状态中提取输出
				state.data().forEach((key, value) -> {
					if (key.endsWith("_result")) {
						Object existing = mergedState.get("all_results");
						if (existing == null) {
							mergedState.put("all_results", value.toString());
						}
						else {
							mergedState.put("all_results", existing + "\n\n---\n\n" + value.toString());
						}
					}
				});
				return mergedState;
			}
		}

		// 创建Agent
		ReactAgent agent1 = ReactAgent.builder()
				.name("agent1")
				.model(chatModel)
				.outputKey("result1")
				.build();

		ReactAgent agent2 = ReactAgent.builder()
				.name("agent2")
				.model(chatModel)
				.outputKey("result2")
				.build();

		ReactAgent agent3 = ReactAgent.builder()
				.name("agent3")
				.model(chatModel)
				.outputKey("result3")
				.build();

		// 使用自定义合并策略
		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_agent")
				.subAgents(List.of(agent1, agent2, agent3))
				.mergeStrategy(new CustomMergeStrategy())
				.build();

		Optional<OverAllState> result = parallelAgent.invoke("分析这个主题");

		if (result.isPresent()) {
			System.out.println("自定义合并策略示例执行成功");
		}
	}

	/**
	 * 示例5：LLM路由（LlmRoutingAgent）
	 *
	 * 使用大语言模型动态决定将请求路由到哪个子Agent
	 */
	public void example5_llmRoutingAgent() throws Exception {
		// 创建专业化的子Agent
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("擅长创作各类文章，包括散文、诗歌等文学作品")
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
				.outputKey("writer_output")
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.description("擅长对文章进行评论、修改和润色")
				.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。" +
						"对于散文类文章，请确保文章中必须包含对于西湖风景的描述。")
				.outputKey("reviewer_output")
				.build();

		ReactAgent translatorAgent = ReactAgent.builder()
				.name("translator_agent")
				.model(chatModel)
				.description("擅长将文章翻译成各种语言")
				.instruction("你是一个专业的翻译家，能够准确地将文章翻译成目标语言。")
				.outputKey("translator_output")
				.build();

		// 创建路由Agent
		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
				.name("content_routing_agent")
				.description("根据用户需求智能路由到合适的专家Agent")
				.model(chatModel)
				.subAgents(List.of(writerAgent, reviewerAgent, translatorAgent))
				.build();

		// 使用 - LLM会自动选择最合适的Agent
		System.out.println("路由测试1: 写作请求");
		Optional<OverAllState> result1 = routingAgent.invoke("帮我写一篇关于春天的散文");
		// LLM会路由到 writerAgent

		System.out.println("路由测试2: 修改请求");
		Optional<OverAllState> result2 = routingAgent.invoke("请帮我修改这篇文章：春天来了，花开了。");
		// LLM会路由到 reviewerAgent

		System.out.println("路由测试3: 翻译请求");
		Optional<OverAllState> result3 = routingAgent.invoke("请将以下内容翻译成英文：春暖花开");
		// LLM会路由到 translatorAgent

		System.out.println("LLM路由示例执行完成");
	}

	/**
	 * 示例6：优化路由准确性
	 *
	 * 通过提供清晰明确的Agent描述来提高路由的准确性
	 */
	public void example6_optimizedRouting() throws Exception {
		// 1. 提供清晰明确的Agent描述
		ReactAgent codeAgent = ReactAgent.builder()
				.name("code_agent")
				.model(chatModel)
				.description("专门处理编程相关问题，包括代码编写、调试、重构和优化。" +
						"擅长Java、Python、JavaScript等主流编程语言。")
				.instruction("你是一个资深的软件工程师...")
				.build();

		// 2. 明确Agent的职责边界
		ReactAgent businessAgent = ReactAgent.builder()
				.name("business_agent")
				.model(chatModel)
				.description("专门处理商业分析、市场研究和战略规划问题。" +
						"不处理技术实现细节。")
				.instruction("你是一个资深的商业分析师...")
				.build();

		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("专门处理内容创作，包括文章、报告、文案等写作任务。")
				.instruction("你是一个专业作家...")
				.build();

		// 3. 使用不同领域的Agent避免重叠
		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
				.name("multi_domain_router")
				.model(chatModel)
				.subAgents(List.of(codeAgent, businessAgent, writerAgent))
				.build();

		// 测试路由
		routingAgent.invoke("如何用Java实现单例模式？");
		routingAgent.invoke("分析一下这个市场的竞争态势");
		routingAgent.invoke("写一篇产品介绍文案");

		System.out.println("优化路由示例执行完成");
	}

	/**
	 * 示例7：混合模式 - 结合顺序、并行和路由
	 *
	 * 组合不同的模式创建复杂的工作流
	 */
	public void example7_hybridPattern() throws Exception {
		// 创建研究Agent（并行执行）
		ReactAgent webResearchAgent = ReactAgent.builder()
				.name("web_research")
				.model(chatModel)
				.description("从互联网搜索信息")
				.outputKey("web_data")
				.build();

		ReactAgent dbResearchAgent = ReactAgent.builder()
				.name("db_research")
				.model(chatModel)
				.description("从数据库查询信息")
				.outputKey("db_data")
				.build();

		ParallelAgent researchAgent = ParallelAgent.builder()
				.name("parallel_research")
				.description("并行收集多个数据源的信息")
				.subAgents(List.of(webResearchAgent, dbResearchAgent))
				.mergeOutputKey("research_data")
				.build();

		// 创建分析Agent
		ReactAgent analysisAgent = ReactAgent.builder()
				.name("analysis_agent")
				.model(chatModel)
				.description("分析研究数据")
				.outputKey("analysis_result")
				.build();

		// 创建报告Agent（路由选择格式）
		ReactAgent pdfReportAgent = ReactAgent.builder()
				.name("pdf_report")
				.model(chatModel)
				.description("生成PDF格式报告")
				.outputKey("pdf_report")
				.build();

		ReactAgent htmlReportAgent = ReactAgent.builder()
				.name("html_report")
				.model(chatModel)
				.description("生成HTML格式报告")
				.outputKey("html_report")
				.build();

		LlmRoutingAgent reportAgent = LlmRoutingAgent.builder()
				.name("report_router")
				.description("根据需求选择报告格式")
				.model(chatModel)
				.subAgents(List.of(pdfReportAgent, htmlReportAgent))
				.build();

		// 组合成顺序工作流
		SequentialAgent hybridWorkflow = SequentialAgent.builder()
				.name("research_workflow")
				.description("完整的研究工作流：并行收集 -> 分析 -> 路由生成报告")
				.subAgents(List.of(researchAgent, analysisAgent, reportAgent))
				.build();

		Optional<OverAllState> result = hybridWorkflow.invoke("研究AI技术趋势并生成HTML报告");

		if (result.isPresent()) {
			System.out.println("混合模式示例执行成功");
		}
	}

	/**
	 * 运行所有示例
	 */
	public void runAllExamples() {
		System.out.println("=== 多智能体（Multi-agent）示例 ===\n");

		try {
			System.out.println("示例1: 顺序执行（Sequential Agent）");
			example1_sequentialAgent();
			System.out.println();

			System.out.println("示例2: 控制推理内容");
			example2_controlReasoningContents();
			System.out.println();

			System.out.println("示例3: 并行执行（Parallel Agent）");
			example3_parallelAgent();
			System.out.println();

			System.out.println("示例4: 自定义合并策略");
			example4_customMergeStrategy();
			System.out.println();

			System.out.println("示例5: LLM路由（LlmRoutingAgent）");
			example5_llmRoutingAgent();
			System.out.println();

			System.out.println("示例6: 优化路由准确性");
			example6_optimizedRouting();
			System.out.println();

			System.out.println("示例7: 混合模式");
			example7_hybridPattern();
			System.out.println();

		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

