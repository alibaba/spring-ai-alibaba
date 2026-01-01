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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class SupervisorAgentTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testSupervisorAgentWithSimpleAgents() throws Exception {
		// Create simple ReactAgent sub-agents
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("擅长创作各类文章，包括散文、诗歌等文学作品")
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答�?)
				.outputKey("writer_output")
				.build();

		ReactAgent translatorAgent = ReactAgent.builder()
				.name("translator_agent")
				.model(chatModel)
				.description("擅长将文章翻译成各种语言")
				.instruction("你是一个专业的翻译家，能够准确地将文章翻译成目标语言�?)
				.outputKey("translator_output")
				.build();

		// Create SupervisorAgent
		SupervisorAgent supervisorAgent = SupervisorAgent.builder()
				.name("content_supervisor")
				.description("内容管理监督者，负责协调写作、翻译等任务")
				.model(chatModel)
				.subAgents(List.of(writerAgent, translatorAgent))
				.build();

		try {
			// Test 1: Simple writing task
			Optional<OverAllState> result1 = supervisorAgent.invoke("帮我写一篇关于春天的短文");

			assertTrue(result1.isPresent(), "Result should be present");
			OverAllState state1 = result1.get();

			// Verify input is preserved
			assertTrue(state1.value("input").isPresent(), "Input should be present in state");
			assertEquals("帮我写一篇关于春天的短文", state1.value("input").get(), "Input should match the request");

			// Verify writer agent output exists
			assertTrue(state1.value("writer_output").isPresent(), "Writer output should be present");
			AssistantMessage writerContent = (AssistantMessage) state1.value("writer_output").get();
			assertNotNull(writerContent.getText(), "Writer content should not be null");
			assertTrue(writerContent.getText().length() > 0, "Writer content should not be empty");

			// Test 2: Translation task
			Optional<OverAllState> result2 = supervisorAgent.invoke("请将以下内容翻译成英文：春暖花开");

			assertTrue(result2.isPresent(), "Translation result should be present");
			OverAllState state2 = result2.get();

			// Verify translator agent output exists
			assertTrue(state2.value("translator_output").isPresent(), "Translator output should be present");
			AssistantMessage translatorContent = (AssistantMessage) state2.value("translator_output").get();
			assertNotNull(translatorContent.getText(), "Translator content should not be null");
			assertTrue(translatorContent.getText().length() > 0, "Translator content should not be empty");

			System.out.println("Test 1 - Writer output: " + writerContent.getText());
			System.out.println("Test 2 - Translator output: " + translatorContent.getText());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("SupervisorAgent execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testSupervisorAgentWithNestedSequentialAgent() throws Exception {
		// Create simple ReactAgent
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("擅长创作各类文章，包括散文、诗歌等文学作品")
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答�?)
				.outputKey("writer_output")
				.build();

		// Create nested SequentialAgent
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

		// Create nested SequentialAgent
		SequentialAgent writingWorkflowAgent = SequentialAgent.builder()
				.name("writing_workflow_agent")
				.description("完整的写作工作流：先写文章，然后进行评审和修�?)
				.subAgents(List.of(articleWriterAgent, reviewerAgent))
				.build();

		// Define professional supervisor instruction
		final String SUPERVISOR_SYSTEM_PROMPT = """
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
				   - 如果用户只需要简单写作，选择 writer_agent
				   - 如果用户需要高质量文章或明确要求评审，选择 writing_workflow_agent

				2. **任务完成判断**:
				   - 当用户的所有需求都已满足时，返回FINISH
				   - 如果还有未完成的任务，继续路由到相应的Agent

				## 响应格式
				只返回Agent名称（writer_agent、writing_workflow_agent）或FINISH，不要包含其他解释�?
				""";

		// Create SupervisorAgent with nested SequentialAgent
		SupervisorAgent supervisorAgent = SupervisorAgent.builder()
				.name("content_supervisor")
				.description("内容管理监督者，负责协调写作和完整写作工作流等任�?)
				.model(chatModel)
				.systemPrompt(SUPERVISOR_SYSTEM_PROMPT)
				.subAgents(List.of(writerAgent, writingWorkflowAgent))
				.build();

		try {
			// Test: Task requiring quality (should route to writing_workflow_agent)
			Optional<OverAllState> result = supervisorAgent.invoke("帮我写一篇关于西湖的散文，并确保质量");

			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();

			// Verify input is preserved
			assertTrue(state.value("input").isPresent(), "Input should be present in state");
			assertEquals("帮我写一篇关于西湖的散文，并确保质量", state.value("input").get(), "Input should match the request");

			// Verify nested SequentialAgent output exists (reviewed_article from writing_workflow_agent)
			assertTrue(state.value("reviewed_article").isPresent(),
					"Reviewed article should be present after writing workflow agent");
			AssistantMessage reviewedContent = (AssistantMessage) state.value("reviewed_article").get();
			assertNotNull(reviewedContent.getText(), "Reviewed content should not be null");
			assertTrue(reviewedContent.getText().length() > 0, "Reviewed content should not be empty");

			// Verify intermediate output from nested agent also exists
			assertTrue(state.value("article").isPresent(), "Article should be present from nested SequentialAgent");

			System.out.println("Reviewed article: " + reviewedContent.getText());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("SupervisorAgent with nested SequentialAgent execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testSupervisorAgentGraphRepresentation() throws Exception {
		// Create simple sub-agents
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("擅长创作各类文章")
				.instruction("你是一个知名的作家�?)
				.outputKey("writer_output")
				.build();

		ReactAgent translatorAgent = ReactAgent.builder()
				.name("translator_agent")
				.model(chatModel)
				.description("擅长将文章翻译成各种语言")
				.instruction("你是一个专业的翻译家�?)
				.outputKey("translator_output")
				.build();

		// Create SupervisorAgent
		SupervisorAgent supervisorAgent = SupervisorAgent.builder()
				.name("content_supervisor")
				.description("内容管理监督�?)
				.model(chatModel)
				.subAgents(List.of(writerAgent, translatorAgent))
				.build();

		try {
			// Test graph representation
			GraphRepresentation representation = supervisorAgent.getGraph()
					.getGraph(GraphRepresentation.Type.PLANTUML);
			assertNotNull(representation, "Graph representation should not be null");
			assertNotNull(representation.content(), "Graph representation content should not be null");
			assertTrue(representation.content().length() > 0, "Graph representation content should not be empty");

			// Verify graph contains supervisor and sub-agents
			String content = representation.content();
			assertTrue(content.contains("content_supervisor"), "Graph should contain supervisor agent");
			assertTrue(content.contains("writer_agent"), "Graph should contain writer agent");
			assertTrue(content.contains("translator_agent"), "Graph should contain translator agent");

			System.out.println("Graph representation:");
			System.out.println(representation.content());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("SupervisorAgent graph representation failed: " + e.getMessage());
		}
	}

	@Test
	public void testSupervisorAgentMultiStepTask() throws Exception {
		// Create sub-agents
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
				.instruction("你是一个专业的翻译家，能够准确地将文章翻译成目标语言。待翻译文章：\n\n {writer_output}�?)
				.outputKey("translator_output")
				.build();

		// Define supervisor instruction for multi-step tasks
		final String SUPERVISOR_SYSTEM_PROMPT = """
				你是一个智能的内容管理监督者�?
				
				## 可用的子Agent及其职责
				
				### writer_agent
				- **功能**: 擅长创作各类文章，包括散文、诗歌等文学作品
				- **输出**: writer_output
				
				### translator_agent
				- **功能**: 擅长将文章翻译成各种语言
				- **输出**: translator_output
				
				## 决策规则
				
				1. **多步骤任务处�?*:
				   - 如果用户需求包含多个步骤（�?先写文章，然后翻�?），需要分步处�?
				   - 先路由到第一个合适的Agent，等待其完成
				   - 完成后，根据剩余需求继续路由到下一个Agent
				   - 直到所有步骤完成，返回FINISH
				
				2. **任务完成判断**:
				   - 当用户的所有需求都已满足时，返回FINISH
				
				## 响应格式
				只返回Agent名称（writer_agent、translator_agent）或FINISH，不要包含其他解释�?
				""";

		// Create SupervisorAgent
		SupervisorAgent supervisorAgent = SupervisorAgent.builder()
				.name("content_supervisor")
				.description("内容管理监督者，负责协调写作和翻译任�?)
				.model(chatModel)
				.systemPrompt(SUPERVISOR_SYSTEM_PROMPT)
				.subAgents(List.of(writerAgent, translatorAgent))
				.build();

		GraphRepresentation representation = supervisorAgent.getGraph()
				.getGraph(GraphRepresentation.Type.PLANTUML);
		// Verify graph contains supervisor and sub-agents
		String content = representation.content();

		System.out.println("===================");
		System.out.println(content);

		try {
			// Test multi-step task: write first, then translate
			Optional<OverAllState> result = supervisorAgent.invoke("先帮我写一篇关于春天的文章，然后翻译成英文");

			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();

			// Verify input is preserved
			assertTrue(state.value("input").isPresent(), "Input should be present in state");

			// Verify both outputs exist (indicating multi-step execution)
			// Note: Depending on the supervisor's decision, both outputs may or may not be present
			// The supervisor might route to writer first, then translator, or handle it differently
			boolean hasWriterOutput = state.value("writer_output").isPresent();
			boolean hasTranslatorOutput = state.value("translator_output").isPresent();

			// At least one output should be present
			assertTrue(hasWriterOutput || hasTranslatorOutput,
					"At least one agent output should be present after multi-step task");

			if (hasWriterOutput) {
				AssistantMessage writerContent = (AssistantMessage) state.value("writer_output").get();
				assertNotNull(writerContent.getText(), "Writer content should not be null");
				System.out.println("Writer output: " + writerContent.getText());
			}

			if (hasTranslatorOutput) {
				AssistantMessage translatorContent = (AssistantMessage) state.value("translator_output").get();
				assertNotNull(translatorContent.getText(), "Translator content should not be null");
				System.out.println("Translator output: " + translatorContent.getText());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("SupervisorAgent multi-step task execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testSupervisorAgentAsSequentialSubAgentWithPlaceholder() throws Exception {
		// Create first ReactAgent that will output content for SupervisorAgent to process
		ReactAgent articleWriterAgent = ReactAgent.builder()
				.name("article_writer")
				.model(chatModel)
				.description("专业写作Agent，负责创作文�?)
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答：{input}�?)
				.outputKey("article_content")
				.build();

		// Create sub-agents for SupervisorAgent
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

		// Define supervisor instruction that uses placeholder to read previous agent output
		// The instruction contains {article_content} placeholder which will be replaced
		// with the output from the first ReactAgent in SequentialAgent
		final String SUPERVISOR_INSTRUCTION = """
				你是一个智能的内容处理监督者，你可以看到前序Agent的聊天历史与任务处理记录。当前，你收到了以下文章内容�?

				{article_content}

				请根据文章内容的特点，决定是进行翻译还是评审�?
				- 如果文章是中文且需要翻译，选择 translator_agent
				- 如果文章需要评审和改进，选择 reviewer_agent
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

				1. **根据文章内容判断**:
				   - 如果文章是中文且用户要求翻译，选择 translator_agent
				   - 如果文章需要评审、改进或优化，选择 reviewer_agent

				2. **任务完成判断**:
				   - 当所有任务完成时，返�?FINISH

				## 响应格式
				只返回Agent名称（translator_agent、reviewer_agent）或FINISH，不要包含其他解释�?
				""";

		// Create SupervisorAgent with instruction that uses placeholder
		SupervisorAgent supervisorAgent = SupervisorAgent.builder()
				.name("content_supervisor")
				.description("内容处理监督者，根据前序Agent的输出决定翻译或评审")
				.model(chatModel)
				.systemPrompt(SUPERVISOR_SYSTEM_PROMPT)
				.instruction(SUPERVISOR_INSTRUCTION) // This instruction contains {article_content} placeholder
				.subAgents(List.of(translatorAgent, reviewerAgent))
				.build();

		// Create SequentialAgent with articleWriterAgent first, then supervisorAgent
		SequentialAgent sequentialAgent = SequentialAgent.builder()
				.name("content_processing_workflow")
				.description("内容处理工作流：先写文章，然后根据文章内容决定翻译或评审")
				.subAgents(List.of(articleWriterAgent, supervisorAgent))
				.build();

		try {
			// Test: Write an article first, then supervisor decides to translate it
			Optional<OverAllState> result = sequentialAgent.invoke("帮我写一篇关于春天的短文");

			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();

			// Verify input is preserved
			assertTrue(state.value("input").isPresent(), "Input should be present in state");
			assertEquals("帮我写一篇关于春天的短文", state.value("input").get(),
					"Input should match the request");

			// Verify first agent output exists (article_content)
			assertTrue(state.value("article_content").isPresent(),
					"Article content should be present from first agent");
			AssistantMessage articleContent = (AssistantMessage) state.value("article_content").get();
			assertNotNull(articleContent.getText(), "Article content should not be null");
			assertTrue(articleContent.getText().length() > 0, "Article content should not be empty");

			// Verify supervisor agent processed the article content
			// The supervisor should have routed to either translator or reviewer based on the instruction
			boolean hasTranslatorOutput = state.value("translator_output").isPresent();
			boolean hasReviewerOutput = state.value("reviewer_output").isPresent();

			// At least one output from supervisor's sub-agents should be present
			assertTrue(hasTranslatorOutput || hasReviewerOutput,
					"At least one supervisor sub-agent output should be present");

			System.out.println("Article content: " + articleContent.getText());
			if (hasTranslatorOutput) {
				AssistantMessage translatorContent = (AssistantMessage) state.value("translator_output").get();
				assertNotNull(translatorContent.getText(), "Translator content should not be null");
				System.out.println("Translator output: " + translatorContent.getText());
			}
			if (hasReviewerOutput) {
				AssistantMessage reviewerContent = (AssistantMessage) state.value("reviewer_output").get();
				assertNotNull(reviewerContent.getText(), "Reviewer content should not be null");
				System.out.println("Reviewer output: " + reviewerContent.getText());
			}

			// Verify that the supervisor's instruction placeholder was properly replaced
			// by checking that the supervisor actually processed the article content
			// (This is implicit in the fact that one of the sub-agents was invoked)
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("SupervisorAgent as SequentialAgent sub-agent with placeholder failed: " + e.getMessage());
		}
	}

}

