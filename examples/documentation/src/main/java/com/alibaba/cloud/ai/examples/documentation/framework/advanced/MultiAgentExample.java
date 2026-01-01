/*
 * Copyright 2024-2026 the original author or authors.
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
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;

/**
 * 多智能体（Multi-agent）示�?
 *
 * 演示不同�?Multi-agent 协作模式，包括：
 * 1. 顺序执行（Sequential Agent�?
 * 2. 并行执行（Parallel Agent�?
 * 3. LLM路由（LlmRoutingAgent�?
 * 4. 自定义合并策�?
 * 5. 监督者模式（SupervisorAgent�?
 *
 * 参考文�? advanced_doc/multi-agent.md
 */
public class MultiAgentExample {

	private final ChatModel chatModel;

	public MultiAgentExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * Main方法：运行所有示�?
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
			System.err.println("请设�?AI_DASHSCOPE_API_KEY 环境变量");
			return;
		}

		// 创建示例实例
		MultiAgentExample example = new MultiAgentExample(chatModel);

		// 运行所有示�?
		example.runAllExamples();
	}

	/**
	 * 示例1：顺序执行（Sequential Agent�?
	 *
	 * 多个Agent按预定义的顺序依次执行，每个Agent的输出成为下一个Agent的输�?
	 */
	public void example1_sequentialAgent() throws Exception {
		// 创建专业化的子Agent
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("专业写作Agent")
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答：{input}�?)
				.outputKey("article")
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.description("专业评审Agent")
				.instruction("你是一个知名的评论家，擅长对文章进行评论和修改�? +
						"对于散文类文章，请确保文章中必须包含对于西湖风景的描述。待评论文章：\n\n {article}" +
						"最终只返回修改后的文章，不要包含任何评论信息�?)
				.outputKey("reviewed_article")
				.build();

		// 创建顺序Agent
		SequentialAgent blogAgent = SequentialAgent.builder()
				.name("blog_agent")
				.description("根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论")
				.subAgents(List.of(writerAgent, reviewerAgent))
				.build();

		// 使用
		Optional<OverAllState> result = blogAgent.invoke("帮我写一�?00字左右的散文");

		if (result.isPresent()) {
			OverAllState state = result.get();

			// 访问第一个Agent的输�?
			state.value("article").ifPresent(article -> {
				if (article instanceof AssistantMessage) {
					System.out.println("原始文章: " + ((AssistantMessage) article).getText());
				}
			});

			// 访问第二个Agent的输�?
			state.value("reviewed_article").ifPresent(reviewedArticle -> {
				if (reviewedArticle instanceof AssistantMessage) {
					System.out.println("评审后文�? " + ((AssistantMessage) reviewedArticle).getText());
				}
			});
		}
	}

	/**
	 * 示例2：控制推理内�?
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
				.instruction("请对文章进行评审修正：\n{article}，最终返回评审修正后的文章内�?)
				.includeContents(true) // 包含上一个Agent的推理内�?
				.returnReasoningContents(true)  // 返回推理过程
				.outputKey("reviewed_article")
				.build();


		// 每个子agent的推理内容，下一个执行的子agent会看到上一个子agent的推理内�?
		SequentialAgent blogAgent = SequentialAgent.builder()
				.name("blog_agent")
				.subAgents(List.of(writerAgent, reviewerAgent))
				.build();

		Optional<OverAllState> result = blogAgent.invoke("帮我写一�?00字左右的散文");

		if (result.isPresent()) {
			// 消息历史将包含所有工具调用和推理过程
			List<Message> messages = (List<Message>) result.get().value("messages").orElse(List.of());
			System.out.println("消息数量: " + messages.size()); // 包含所有中间步�?
		}
	}

	/**
	 * 示例3：并行执行（Parallel Agent�?
	 *
	 * 多个Agent同时处理相同的输入，它们的结果被收集并合�?
	 */
	public void example3_parallelAgent() throws Exception {
		// 创建多个专业化Agent
		ReactAgent proseWriterAgent = ReactAgent.builder()
				.name("prose_writer_agent")
				.model(chatModel)
				.description("专门写散文的AI助手")
				.instruction("你是一个知名的散文作家，擅长写优美的散文�? +
						"用户会给你一个主题：{input}，你只需要创作一�?00字左右的散文�?)
				.outputKey("prose_result")
				.enableLogging(true)
				.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
				.name("poem_writer_agent")
				.model(chatModel)
				.description("专门写现代诗的AI助手")
				.instruction("你是一个知名的现代诗人，擅长写现代诗�? +
						"用户会给你的主题是：{input}，你只需要创作一首现代诗�?)
				.outputKey("poem_result")
				.enableLogging(true)
				.build();

		ReactAgent summaryAgent = ReactAgent.builder()
				.name("summary_agent")
				.model(chatModel)
				.description("专门做内容总结的AI助手")
				.instruction("你是一个专业的内容分析师，擅长对主题进行总结和提炼�? +
						"用户会给你一个主题：{input}，你只需要对这个主题进行简要总结�?)
				.outputKey("summary_result")
				.enableLogging(true)
				.build();

		// 创建并行Agent
		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_creative_agent")
				.description("并行执行多个创作任务，包括写散文、写诗和做总结")
				.mergeOutputKey("merged_results")
				.subAgents(List.of(proseWriterAgent, poemWriterAgent, summaryAgent))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
				.build();

		ExecutorService executorService = Executors.newFixedThreadPool(3);
		// 使用
		Flux<NodeOutput> flux = parallelAgent.stream("�?西湖'为主�?, RunnableConfig.builder().addParallelNodeExecutor("parallel_creative_agent", executorService).build());

		AtomicReference<NodeOutput> lastOutput = new AtomicReference<>();
		flux.doOnNext(nodeOutput -> {
			System.out.println("节点输出: " + nodeOutput);
			lastOutput.set(nodeOutput);
		}).doOnError(error -> {
			System.err.println("执行出错: " + error.getMessage());
		}).doOnComplete(() -> {
			System.out.println("并行Agent流式执行完成\n\n");

			NodeOutput output = lastOutput.get();
			if (output == null) {
				System.out.println("未收到任何输出，无法展示结果�?);
				return;
			}

			OverAllState state = output.state();
			// 访问各个Agent的输�?
			state.value("prose_result").ifPresent(r ->
					System.out.println("散文: " + r));
			state.value("poem_result").ifPresent(r ->
					System.out.println("诗歌: " + r));
			state.value("summary_result").ifPresent(r ->
					System.out.println("总结: " + r));

			// 访问合并后的结果
			state.value("merged_results").ifPresent(r ->
					System.out.println("合并结果: " + r));
		}).blockLast();

	}

	/**
	 * 示例4：自定义合并策略
	 *
	 * 实现自定义的合并策略来控制如何组合多个Agent的输�?
	 */
	public void example4_customMergeStrategy() throws Exception {
		// 自定义合并策�?
		class CustomMergeStrategy implements ParallelAgent.MergeStrategy {
			@Override
			public Map<String, Object> merge(Map<String, Object> mergedState, OverAllState state) {
				// 从每个Agent的状态中提取输出
				state.data().forEach((key, value) -> {
					// 检查key不为null且以"_result"结尾
					if (key != null && key.endsWith("_result")) {
						String resultText = "";
						if (value instanceof GraphResponse graphResponse) {
                            if (graphResponse.resultValue().isPresent()) {
                                resultText = graphResponse.resultValue().get().toString();
                            }
						} else if (value != null) {
							resultText = value.toString();
						}
						Object existing = mergedState.get("all_results");
						if (existing == null) {
							mergedState.put("all_results", resultText);
						}
						else {
							mergedState.put("all_results", existing + "\n\n---\n\n" + resultText);
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
				.outputKey("agent1_result")
				.build();

		ReactAgent agent2 = ReactAgent.builder()
				.name("agent2")
				.model(chatModel)
				.outputKey("agent2_result")
				.build();

		ReactAgent agent3 = ReactAgent.builder()
				.name("agent3")
				.model(chatModel)
				.outputKey("agent3_result")
				.build();

		// 使用自定义合并策�?
		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_agent")
				.subAgents(List.of(agent1, agent2, agent3))
				.mergeStrategy(new CustomMergeStrategy())
				.mergeOutputKey("all_results")
				.build();

		Optional<OverAllState> result = parallelAgent.invoke("分析这个主题");

		if (result.isPresent()) {
			OverAllState state = result.get();
			state.value("all_results").ifPresent(mergeResult -> {
				System.out.println("合并结果: " + mergeResult);
			});
			System.out.println("自定义合并策略示例执行成�?);
		}
	}

	/**
	 * 示例5：LLM路由（LlmRoutingAgent�?
	 *
	 * 使用大语言模型动态决定将请求路由到哪个子Agent
	 */
	public void example5_llmRoutingAgent() throws Exception {
		// 创建专业化的子Agent
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("擅长创作各类文章，包括散文、诗歌等文学作品")
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答�?)
				.outputKey("writer_output")
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.description("擅长对文章进行评论、修改和润色")
				.instruction("你是一个知名的评论家，擅长对文章进行评论和修改�? +
						"对于散文类文章，请确保文章中必须包含对于西湖风景的描述�?)
				.outputKey("reviewer_output")
				.build();

		ReactAgent translatorAgent = ReactAgent.builder()
				.name("translator_agent")
				.model(chatModel)
				.description("擅长将文章翻译成各种语言")
				.instruction("你是一个专业的翻译家，能够准确地将文章翻译成目标语言�?)
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
		Optional<OverAllState> result2 = routingAgent.invoke("请帮我修改这篇文章：春天来了，花开了�?);
		// LLM会路由到 reviewerAgent

		System.out.println("路由测试3: 翻译请求");
		Optional<OverAllState> result3 = routingAgent.invoke("请将以下内容翻译成英文：春暖花开");
		// LLM会路由到 translatorAgent

		System.out.println("LLM路由示例执行完成");
	}

	/**
	 * 示例6：优化路由准确�?
	 *
	 * 通过提供清晰明确的Agent描述来提高路由的准确�?
	 */
	public void example6_optimizedRouting() throws Exception {
		// 1. 提供清晰明确的Agent描述
		ReactAgent codeAgent = ReactAgent.builder()
				.name("code_agent")
				.model(chatModel)
				.description("专门处理编程相关问题，包括代码编写、调试、重构和优化�? +
						"擅长Java、Python、JavaScript等主流编程语言�?)
				.instruction("你是一个资深的软件工程�?..")
				.build();

		// 2. 明确Agent的职责边�?
		ReactAgent businessAgent = ReactAgent.builder()
				.name("business_agent")
				.model(chatModel)
				.description("专门处理商业分析、市场研究和战略规划问题�? +
						"不处理技术实现细节�?)
				.instruction("你是一个资深的商业分析�?..")
				.build();

		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("专门处理内容创作，包括文章、报告、文案等写作任务�?)
				.instruction("你是一个专业作�?..")
				.build();

		// 3. 使用不同领域的Agent避免重叠
		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
				.name("multi_domain_router")
				.model(chatModel)
				.subAgents(List.of(codeAgent, businessAgent, writerAgent))
				.build();

		// 测试路由
		routingAgent.invoke("如何用Java实现单例模式�?);
		routingAgent.invoke("分析一下这个市场的竞争态势");
		routingAgent.invoke("写一篇产品介绍文�?);

		System.out.println("优化路由示例执行完成");
	}

	/**
	 * 示例7：混合模�?- 结合顺序、并行和路由
	 *
	 * 组合不同的模式创建复杂的工作�?
	 */
	public void example7_hybridPattern() throws Exception {
		// 创建研究Agent（并行执行）
		ReactAgent webResearchAgent = ReactAgent.builder()
				.name("web_research")
				.model(chatModel)
				.description("从互联网搜索信息")
				.instruction("请搜索并收集关于以下主题的信息：{input}")
				.outputKey("web_data")
				.build();

		ReactAgent dbResearchAgent = ReactAgent.builder()
				.name("db_research")
				.model(chatModel)
				.description("从数据库查询信息")
				.instruction("请从数据库中查询并收集关于以下主题的信息：{input}")
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
				.instruction("请分析以下收集到的数据并提供见解：{research_data}")
				.outputKey("analysis_result")
				.build();

		// 创建报告Agent（路由选择格式�?
		ReactAgent pdfReportAgent = ReactAgent.builder()
				.name("pdf_report")
				.model(chatModel)
				.description("生成PDF格式报告")
				.instruction("""
						请根据研究结果和分析结果生成一份PDF格式的报告�?
						
						研究结果：{research_data}
						分析结果：{analysis_result}
						""")
				.outputKey("pdf_report")
				.build();

		ReactAgent htmlReportAgent = ReactAgent.builder()
				.name("html_report")
				.model(chatModel)
				.description("生成HTML格式报告")
				.instruction("""
						请根据研究结果和分析结果生成一份HTML格式的报告�?
						
						研究结果：{research_data}
						分析结果：{analysis_result}
						""")
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
				.description("完整的研究工作流：并行收�?-> 分析 -> 路由生成报告")
				.subAgents(List.of(researchAgent, analysisAgent, reportAgent))
				.build();


		// 打印工作流图�?
		System.out.println("\n=== 混合模式工作流图�?===");
		printGraphRepresentation(hybridWorkflow);
		System.out.println("=========================\n");

		Optional<OverAllState> result = hybridWorkflow.invoke("研究AI技术趋势并生成HTML报告");

		if (result.isPresent()) {
			System.out.println("混合模式示例执行成功");
		}
	}

	/**
	 * 示例8：监督者模式（SupervisorAgent�?
	 *
	 * SupervisorAgent �?LlmRoutingAgent 类似，但有以下关键区别：
	 * 1. 子Agent处理完成后会返回到Supervisor，而不是直接结�?
	 * 2. Supervisor可以决定继续路由到其他子Agent，或者标记任务完成（FINISH�?
	 * 3. 支持嵌套Agent（如SequentialAgent、ParallelAgent）作为子Agent
	 *
	 * 这个示例展示了如何使用SupervisorAgent管理包含普通ReactAgent和嵌套SequentialAgent的复杂工作流
	 */
	public void example8_supervisorAgent() throws Exception {
		// 定义专业的监督者指令（如果不定义，则使用系统默认的提示词）
		final String SUPERVISOR_INSTRUCTION = """
				你是一个智能的内容管理监督者，负责协调和管理多个专业Agent来完成用户的内容处理需求�?

				## 你的职责
				1. 分析用户需求，将其分解为合适的子任�?
				2. 根据任务特性，选择合适的Agent进行处理
				3. 监控任务执行状态，决定是否需要继续处理或完成任务
				4. 当所有任务完成时，返回FINISH结束流程

				## 可用的子Agent及其职责

				### writer_agent
				- **功能**: 擅长创作各类文章，包括散文、诗歌等文学作品
				- **适用场景**: 
				  * 用户需要创作新文章、散文、诗歌等原创内容
				  * 简单的写作任务，不需要后续评审或修改
				- **输出**: writer_output

				### translator_agent
				- **功能**: 擅长将文章翻译成各种语言
				- **适用场景**:
				  * 用户需要将内容翻译成其他语言
				  * 翻译任务通常是单一操作，不需要多步骤处理
				- **输出**: translator_output

				### writing_workflow_agent
				- **功能**: 完整的写作工作流，包含两个步骤：先写文章，然后进行评审和修改
				- **适用场景**:
				  * 用户需要高质量的文章，要求经过评审和修�?
				  * 任务明确要求"确保质量"�?需要评�?�?需要修�?�?
				  * 需要多步骤处理的复杂写作任�?
				- **工作流程**: 
				  1. article_writer: 根据用户需求创作文�?
				  2. reviewer: 对文章进行评审和修改，确保质�?
				- **输出**: reviewed_article

				## 决策规则

				1. **单一任务判断**:
				   - 如果用户只需要翻译，选择 translator_agent
				   - 如果用户只需要简单写作，选择 writer_agent
				   - 如果用户需要高质量文章或明确要求评审，选择 writing_workflow_agent

				2. **多步骤任务处�?*:
				   - 如果用户需求包含多个步骤（�?先写文章，然后翻�?），需要分步处�?
				   - 先路由到第一个合适的Agent，等待其完成
				   - 完成后，根据剩余需求继续路由到下一个Agent
				   - 直到所有步骤完成，返回FINISH

				3. **任务完成判断**:
				   - 当用户的所有需求都已满足时，返回FINISH
				   - 如果还有未完成的任务，继续路由到相应的Agent

				## 响应格式
				只返回Agent名称（writer_agent、translator_agent、writing_workflow_agent）或FINISH，不要包含其他解释�?
				""";
		// 1. 创建普通的ReactAgent子Agent
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("擅长创作各类文章，包括散文、诗歌等文学作品")
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答：\n\n {input}�?)
				.outputKey("writer_output")
				.build();

		ReactAgent translatorAgent = ReactAgent.builder()
				.name("translator_agent")
				.model(chatModel)
				.description("擅长将文章翻译成各种语言")
				.instruction("你是一个专业的翻译家，能够准确地将文章翻译成目标语言�? +
						"如果待翻译的内容已存在于状态中，请使用：\n\n {writer_output}�?)
				.outputKey("translator_output")
				.build();

		// 2. 创建嵌套的SequentialAgent作为子Agent
		// 这个SequentialAgent包含多个步骤：先写文章，再评�?
		ReactAgent articleWriterAgent = ReactAgent.builder()
				.name("article_writer")
				.model(chatModel)
				.description("专业写作Agent")
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答：{input}�?)
				.outputKey("article")
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer")
				.model(chatModel)
				.description("专业评审Agent")
				.instruction("你是一个知名的评论家，擅长对文章进行评论和修改�? +
						"对于散文类文章，请确保文章中必须包含对于西湖风景的描述。待评论文章：\n\n {article}" +
						"最终只返回修改后的文章，不要包含任何评论信息�?)
				.outputKey("reviewed_article")
				.build();

		// 创建嵌套的SequentialAgent
		SequentialAgent writingWorkflowAgent = SequentialAgent.builder()
				.name("writing_workflow_agent")
				.description("完整的写作工作流：先写文章，然后进行评审和修�?)
				.subAgents(List.of(articleWriterAgent, reviewerAgent))
				.build();

		// 3. 创建SupervisorAgent，包含普通Agent和嵌套Agent
		SupervisorAgent supervisorAgent = SupervisorAgent.builder()
				.name("content_supervisor")
				.description("内容管理监督者，负责协调写作、翻译和完整写作工作流等任务")
				.model(chatModel)
				.systemPrompt(SUPERVISOR_INSTRUCTION)
				.subAgents(List.of(writerAgent, translatorAgent, writingWorkflowAgent))
				.build();

		// 使用示例
		System.out.println("监督者测�?: 简单写作任�?);
		Optional<OverAllState> result1 = supervisorAgent.invoke("帮我写一篇关于春天的短文");
		// Supervisor会路由到writer_agent，处理完成后返回Supervisor，Supervisor判断完成返回FINISH
		if (result1.isPresent()) {
			result1.get().value("writer_output").ifPresent(output ->
					System.out.println("写作结果: " + output));
		}

		System.out.println("\n监督者测�?: 需要完整工作流的任�?);
		Optional<OverAllState> result2 = supervisorAgent.invoke("帮我写一篇关于西湖的散文，并确保质量");
		// Supervisor会路由到writing_workflow_agent（嵌套SequentialAgent），
		// 该Agent会先写文章，然后评审，完成后返回Supervisor，Supervisor判断完成返回FINISH
		if (result2.isPresent()) {
			result2.get().value("reviewed_article").ifPresent(output ->
					System.out.println("评审后文�? " + output));
		}

		System.out.println("\n监督者测�?: 翻译任务");
		Optional<OverAllState> result3 = supervisorAgent.invoke("请将以下内容翻译成英文：春暖花开");
		// Supervisor会路由到translator_agent，处理完成后返回Supervisor，Supervisor判断完成返回FINISH
		if (result3.isPresent()) {
			result3.get().value("translator_output").ifPresent(output ->
					System.out.println("翻译结果: " + output));
		}

		System.out.println("\n监督者测�?: 多步骤任务（可能需要多次路由）");
		Optional<OverAllState> result4 = supervisorAgent.invoke("先帮我写一篇关于春天的文章，然后翻译成英文");
		// Supervisor可能会：
		// 1. 先路由到writer_agent写文章，完成后返回Supervisor
		// 2. Supervisor判断还需要翻译，路由到translator_agent
		// 3. 翻译完成后返回Supervisor，Supervisor判断所有任务完成，返回FINISH
		if (result4.isPresent()) {
			result4.get().value("writer_output").ifPresent(output ->
					System.out.println("写作结果: " + output));
			result4.get().value("translator_output").ifPresent(output ->
					System.out.println("翻译结果: " + output));
		}

		// 打印工作流图�?
		System.out.println("\n=== SupervisorAgent 工作流图�?===");
		printGraphRepresentation(supervisorAgent);
		System.out.println("==================================\n");

		// 示例5：SupervisorAgent作为SequentialAgent的子Agent，使用占位符
		System.out.println("\n监督者测�?: SupervisorAgent作为SequentialAgent的子Agent（使用占位符�?);
		example8_supervisorAgentAsSequentialSubAgent();
		System.out.println();

		System.out.println("SupervisorAgent示例执行完成");
	}

	/**
	 * 示例8.1：SupervisorAgent作为SequentialAgent的子Agent，使用占位符
	 *
	 * 这个示例展示了：
	 * 1. SupervisorAgent可以作为SequentialAgent的子Agent
	 * 2. SupervisorAgent的instruction可以使用占位符引用前序Agent的输�?
	 * 3. SupervisorAgent的子Agent的instruction也可以使用占位符引用前序Agent的输�?
	 */
	private void example8_supervisorAgentAsSequentialSubAgent() throws Exception {
		// 1. 创建第一个Agent，用于生成文章内�?
		ReactAgent articleWriterAgent = ReactAgent.builder()
				.name("article_writer")
				.model(chatModel)
				.description("专业写作Agent，负责创作文�?)
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答：{input}�?)
				.outputKey("article_content")
				.build();

		// 2. 创建SupervisorAgent的子Agent
		ReactAgent translatorAgent = ReactAgent.builder()
				.name("translator_agent")
				.model(chatModel)
				.description("擅长将文章翻译成各种语言")
				.instruction("你是一个专业的翻译家，能够准确地将文章翻译成目标语言。待翻译文章：\n\n {article_content}�?)
				.outputKey("translator_output")
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.description("擅长对文章进行评审和修改")
				.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。待评审文章：\n\n {article_content}�?
						+ "请对文章进行评审，指出优点和需要改进的地方，并返回评审后的改进版本�?)
				.outputKey("reviewer_output")
				.build();

		// 3. 定义SupervisorAgent的instruction，使用占位符引用前序Agent的输�?
		// 这个instruction包含 {article_content} 占位符，会被替换为第一个Agent的输�?
		final String SUPERVISOR_INSTRUCTION = """
				你是一个智能的内容处理监督者，你可以看到前序Agent的聊天历史与任务处理记录。当前，你收到了以下文章内容�?

				{article_content}

				请根据文章内容的特点和用户需求，决定是进行翻译还是评审：
				- 如果用户要求翻译或文章需要翻译成其他语言，选择 translator_agent
				- 如果用户要求评审、改进或优化文章，选择 reviewer_agent
				- 如果任务完成，返�?FINISH
				""";

		final String SUPERVISOR_SYSTEM_PROMPT = """
				你是一个智能的内容处理监督者，负责协调翻译和评审任务�?

				## 可用的子Agent及其职责

				### translator_agent
				- **功能**: 擅长将文章翻译成各种语言
				- **适用场景**: 当文章需要翻译成其他语言�?
				- **输出**: translator_output

				### reviewer_agent
				- **功能**: 擅长对文章进行评审和修改
				- **适用场景**: 当文章需要评审、改进或优化�?
				- **输出**: reviewer_output

				## 决策规则

				1. **根据文章内容和用户需求判�?*:
				   - 如果用户要求翻译或文章需要翻译成其他语言，选择 translator_agent
				   - 如果用户要求评审、改进或优化文章，选择 reviewer_agent

				2. **任务完成判断**:
				   - 当所有任务完成时，返�?FINISH

				## 响应格式
				只返回Agent名称（translator_agent、reviewer_agent）或FINISH，不要包含其他解释�?
				""";

		// 4. 创建SupervisorAgent，其instruction使用占位�?
		SupervisorAgent supervisorAgent = SupervisorAgent.builder()
				.name("content_supervisor")
				.description("内容处理监督者，根据前序Agent的输出决定翻译或评审")
				.model(chatModel)
				.systemPrompt(SUPERVISOR_SYSTEM_PROMPT)
				.instruction(SUPERVISOR_INSTRUCTION) // 这个instruction包含 {article_content} 占位�?
				.subAgents(List.of(translatorAgent, reviewerAgent))
				.build();

		// 5. 创建SequentialAgent，先执行articleWriterAgent，然后执行supervisorAgent
		SequentialAgent sequentialAgent = SequentialAgent.builder()
				.name("content_processing_workflow")
				.description("内容处理工作流：先写文章，然后根据文章内容决定翻译或评审")
				.subAgents(List.of(articleWriterAgent, supervisorAgent))
				.build();

		// 测试场景1：写文章后翻�?
		System.out.println("场景1: 写文章后翻译");
		Optional<OverAllState> result1 = sequentialAgent.invoke("帮我写一篇关于春天的短文，然后翻译成英文");
		if (result1.isPresent()) {
			OverAllState state = result1.get();
			state.value("article_content").ifPresent(output -> {
				if (output instanceof AssistantMessage) {
					System.out.println("文章内容: " + ((AssistantMessage) output).getText());
				}
			});
			state.value("translator_output").ifPresent(output -> {
				if (output instanceof AssistantMessage) {
					System.out.println("翻译结果: " + ((AssistantMessage) output).getText());
				}
			});
		}

		// 测试场景2：写文章后评�?
		System.out.println("\n场景2: 写文章后评审");
		Optional<OverAllState> result2 = sequentialAgent.invoke("帮我写一篇关于春天的短文，然后进行评审和改进");
		if (result2.isPresent()) {
			OverAllState state = result2.get();
			state.value("article_content").ifPresent(output -> {
				if (output instanceof AssistantMessage) {
					System.out.println("文章内容: " + ((AssistantMessage) output).getText());
				}
			});
			state.value("reviewer_output").ifPresent(output -> {
				if (output instanceof AssistantMessage) {
					System.out.println("评审结果: " + ((AssistantMessage) output).getText());
				}
			});
		}
	}

	/**
	 * 打印工作流图表（支持SupervisorAgent�?
	 */
	private void printGraphRepresentation(SupervisorAgent agent) {
		GraphRepresentation representation = agent.getAndCompileGraph().getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println(representation.content());
	}

	private void testRoutingSequentialEmbedding() throws GraphRunnerException {
		ReactAgent reactAgent = ReactAgent.builder()
				.name("weather_agent")
				.description("根据用户的问题和提炼的位置信息查询天气。\n\n 用户问题：{input} \n\n 位置信息：{location}")
				.model(chatModel)
				.outputKey("weather")
				.systemPrompt("你是一个天气查询专�?).build();

		ReactAgent locationAgent = ReactAgent.builder()
				.name("location_agent")
				.description("根据用户的问题，进行位置查询。\n 用户问题：{input}")
				.model(chatModel)
				.outputKey("location")
				.systemPrompt("你是一个位置查询专�?).build();

		SequentialAgent sequentialAgent = SequentialAgent.builder()
				.name("天气小助�?)
				.description("天气小助�?)
				.subAgents(List.of(locationAgent, reactAgent))
				.build();

		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
				.name("用户小助�?)
				.description("帮助用户完成各种需�?)
//				.routingInstruction(""); // 可以提供详尽的说明，告知routing路由职责，如何选择子Agent等，用于替代系统默认的prompt�?
				.model(chatModel)
				.subAgents(List.of(sequentialAgent)).build();

		Optional<OverAllState> invoke = routingAgent.invoke("天气怎么�?);
		System.out.println(invoke);
	}

	/**
	 * 打印工作流图�?
	 *
	 * 使用PlantUML格式展示Agent工作流的结构
	 */
	private void printGraphRepresentation(SequentialAgent agent) {
		GraphRepresentation representation = agent.getAndCompileGraph().getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println(representation.content());
	}

	/**
	 * 运行所有示�?
	 */
	public void runAllExamples() {
		System.out.println("=== 多智能体（Multi-agent）示�?===\n");

		try {
			System.out.println("示例1: 顺序执行（Sequential Agent�?);
			example1_sequentialAgent();
			System.out.println();

			System.out.println("示例2: 控制推理内容");
			example2_controlReasoningContents();
			System.out.println();

			System.out.println("示例3: 并行执行（Parallel Agent�?);
			example3_parallelAgent();
			System.out.println();

			System.out.println("示例4: 自定义合并策�?);
			example4_customMergeStrategy();
			System.out.println();
//
			System.out.println("示例5: LLM路由（LlmRoutingAgent�?);
			example5_llmRoutingAgent();
			System.out.println();

			System.out.println("示例6: 优化路由准确�?);
			example6_optimizedRouting();
			System.out.println();

			System.out.println("示例7: 混合模式");
			example7_hybridPattern();
			System.out.println();

			System.out.println("示例8: 监督者模式（SupervisorAgent�?);
			example8_supervisorAgent();
			System.out.println();

			testRoutingSequentialEmbedding();

		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}
}

