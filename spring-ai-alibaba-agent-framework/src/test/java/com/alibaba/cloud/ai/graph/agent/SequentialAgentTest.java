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
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static com.alibaba.cloud.ai.graph.agent.tools.PoetTool.createPoetToolCallback;
import static com.alibaba.cloud.ai.graph.agent.tools.ReviewerTool.createReviewerToolCallback;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class SequentialAgentTest {

    private static final Logger log = LoggerFactory.getLogger(SequentialAgentTest.class);
    private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testSequentialAgent() throws Exception {
		ReactAgent writerAgent = ReactAgent.builder()
			.name("writer_agent")
			.model(chatModel)
			.description("可以写文章�?)
			.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答�?)
			.outputKey("article")
			.enableLogging(true)
			.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
			.name("reviewer_agent")
			.model(chatModel)
			.description("可以对文章进行评论和修改�?)
			.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。最终只返回修改后的文章，不要包含任何评论信息�?)
			.outputKey("reviewed_article")
			.build();

		SequentialAgent blogAgent = SequentialAgent.builder()
			.name("blog_agent")
			.description("可以根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论�?)
			.subAgents(List.of(writerAgent, reviewerAgent))
			.build();

		try {
			Optional<OverAllState> result = blogAgent.invoke("帮我写一�?00字左右的散文");

			assertTrue(result.isPresent(), "Result should be present");

			OverAllState state = result.get();

			assertTrue(state.value("article").isPresent(), "Article should be present after writer agent");
			assertEquals(5, ((List<?>)state.value("messages").get()).size());
			AssistantMessage article = (AssistantMessage) state.value("article").get();
			assertNotNull(article.getText(), "Article content should not be null");

			assertTrue(state.value("reviewed_article").isPresent(),
					"Reviewed article should be present after reviewer agent");
			AssistantMessage reviewedArticle = (AssistantMessage) state.value("reviewed_article").get();
			assertNotNull(reviewedArticle.getText(), "Reviewed article content should not be null");

			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("SequentialAgent execution failed: " + e.getMessage());
		}
	}


	@Test
	public void testSequentialWithSubAgentReasoningContents() throws Exception {
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("可以写文章�?)
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答�?)
				.returnReasoningContents(true)
				.tools(List.of(createPoetToolCallback()))
				.outputKey("article")
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.description("可以对文章进行评论和修改�?)
				.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。最终只返回修改后的文章，不要包含任何评论信息�?)
				.returnReasoningContents(true)
				.tools(List.of(createReviewerToolCallback()))
				.outputKey("reviewed_article")
				.build();

		SequentialAgent blogAgent = SequentialAgent.builder()
				.name("blog_agent")
				.description("可以根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论�?)
				.subAgents(List.of(writerAgent, reviewerAgent))
				.build();

		try {
			Optional<OverAllState> result = blogAgent.invoke("帮我写一�?00字左右的散文");

			assertTrue(result.isPresent(), "Result should be present");

			OverAllState state = result.get();

			assertTrue(state.value("article").isPresent(), "Article should be present after writer agent");
			assertTrue(state.value("reviewed_article").isPresent(), "Reviewed article should be present after reviewer agent");
			assertEquals(9, ((List<?>)state.value("messages").get()).size());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("SequentialAgent execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testSequentialWithoutSubAgentReasoningContents() throws Exception {
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("可以写文章�?)
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答�?)
				.returnReasoningContents(false) // by default false
				.tools(List.of(createPoetToolCallback()))
				.outputKey("article")
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.description("可以对文章进行评论和修改�?)
				.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。最终只返回修改后的文章，不要包含任何评论信息�?)
				.returnReasoningContents(false)  // by default false
				.tools(List.of(createReviewerToolCallback()))
				.outputKey("reviewed_article")
				.build();

		SequentialAgent blogAgent = SequentialAgent.builder()
				.name("blog_agent")
				.description("可以根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论�?)
				.subAgents(List.of(writerAgent, reviewerAgent))
				.build();

		try {
			Optional<OverAllState> result = blogAgent.invoke("帮我写一�?00字左右的散文");
			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();
			assertTrue(state.value("article").isPresent(), "Article should be present after writer agent");
			assertTrue(state.value("reviewed_article").isPresent(), "Reviewed article should be present after reviewer agent");
			assertEquals(5, ((List<?>)state.value("messages").get()).size());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("SequentialAgent execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testEmbeddedSequentialAgent() throws Exception {

		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("可以写文章�?)
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答�?)
				.outputKey("article")
				.enableLogging(true)
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.description("可以对文章进行评论和修改�?)
				.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。最后输出修改后的文章，不要包含任何评论信息�?)
				.outputKey("reviewed_article")
				.enableLogging(true)
				.build();

		SequentialAgent child_1 = SequentialAgent.builder()
				.name("child_1")
				.description("可以根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论，必要时做出修改�?)
				.subAgents(List.of(writerAgent, reviewerAgent))
				.build();


		ReactAgent signature_agent = ReactAgent.builder()
				.name("signature_agent")
				.model(chatModel)
				.description("为文章增加固定的署名�?)
				.includeContents(true)
				.instruction("你负责为生成的文章署名，请将署名附加在文章最后。署名：Spring AI Alibaba�?)
				.outputKey("signed_article")
				.enableLogging(true)
				.build();


		SequentialAgent blogAgentParent = SequentialAgent.builder()
				.name("blogAgentParent")
				.description("可以根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论，必要时做出修改�?)
				.subAgents(List.of(child_1, signature_agent, getChild3()))
				.build();

		try {
			List<NodeOutput> result = new ArrayList<>();
			 blogAgentParent.stream( "帮我写一�?00字左右的散文").doOnNext(output -> {
				 System.out.println(output);
				 result.add(output);
			}).blockLast();
			assertNotNull(result);
			assertFalse(result.isEmpty());
			var last = result.get(result.size() - 1);
			var finalState = last.state();
			assertTrue(finalState.value("article").isPresent());
			assertTrue(finalState.value("reviewed_article").isPresent());
			assertTrue(finalState.value("signed_article").isPresent());
			assertTrue(finalState.value("revised_article").isPresent());
			assertTrue(finalState.value("censored_article").isPresent());

		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("SequentialAgent execution failed: " + e.getMessage());
		}

		// Verify all hooks were executed
	}

	private ParallelAgent createParallelAgent(String name) throws GraphStateException {
		// Create specialized sub-agents with unique output keys and specific instructions
		ReactAgent proseWriterAgent = ReactAgent.builder()
				.name("prose_writer_agent")
				.model(chatModel)
				.description("专门写散文的AI助手")
				.instruction("你是一个知名的散文作家，擅长写优美的散文。用户会给你一个主题，你只需要创作一�?00字左右的散文，不要写诗或做总结。请专注于散文创作，确保内容优美、意境深远�?)
				.outputKey("prose_result")
				.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
				.name("poem_writer_agent")
				.model(chatModel)
				.description("专门写现代诗的AI助手")
				.instruction("你是一个知名的现代诗人，擅长写现代诗。用户会给你一个主题，你只需要创作一首现代诗，不要写散文或做总结。请专注于诗歌创作，确保语言精炼、意象丰富�?)
				.outputKey("poem_result")
				.build();

		ReactAgent summaryAgent = ReactAgent.builder()
				.name("summary_agent")
				.model(chatModel)
				.description("专门做内容总结的AI助手")
				.instruction("你是一个专业的内容分析师，擅长对主题进行总结和提炼。用户会给你一个主题，你只需要对这个主题进行简要总结，不要写散文或诗歌。请专注于总结分析，确保观点清晰、概括准确�?)
				.outputKey("summary_result")
				.build();

		// Create ParallelAgent that will execute all sub-agents in parallel
		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name(name)
				.description("并行执行多个创作任务，包括写散文、写诗和做总结")
				.subAgents(List.of(proseWriterAgent, poemWriterAgent, summaryAgent))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy()) // �?添加合并策略
				.build();

		return parallelAgent;

	}

	public SequentialAgent getChild3() throws GraphStateException {

		ReactAgent reviserAgent = ReactAgent.builder()
				.name("reviser_agent")
				.model(chatModel)
				.description("对文章进行错别字订正�?)
				.includeContents(false) // 不包含上下文内容，专注于当前文章的审�?
				.instruction("""
					你是一个排版专家，负责检查错别字、语法等问题，最终输出修改后的文档原文，输出不要包含无关信息�?
			
					以下是文档原文：
					{reviewed_article}
				""")
				.outputKey("revised_article")
				.enableLogging(true)
				.build();

		ReactAgent censorAgent = ReactAgent.builder()
				.name("censor_agent")
				.model(chatModel)
				.description("可以对文章内容进行合规性审查�?)
				.includeContents(false) // 不包含上下文内容，专注于当前文章的审�?
				.instruction("""
					你是一个合规审查专员，审查文章中是否有违法或者不合规的内容，如果有的话需要进行改进。最终输出修改后的文档原文，输出不要包含无关信息�?
			
					以下是文档原文：
					{reviewed_article}
				""")
				.outputKey("censored_article")
				.enableLogging(true)
				.build();

		SequentialAgent child_3 = SequentialAgent.builder()
				.name("child_3")
				.description("可以根据对用户给定的文章进行排版、合规等检查和订正�?)
				.subAgents(List.of(reviserAgent, censorAgent))
				.build();

		return child_3;
	}

    @Test
    public void testOutputSchema() throws Exception {
        ReactAgent sqlGenerateAgent = ReactAgent.builder()
                .name("sqlGenerateAgent")
                .model(chatModel)
                .description("可以根据用户的自然语言生成MySQL的SQL代码�?)
                .instruction("你是一个熟悉MySQL数据库的小助手，请你根据用户的自然语言，输出对应的SQL�?)
                .outputSchema("""
                        {
                            "$schema": "https://json-schema.org/draft/2020-12/schema",
                            "type": "object",
                            "properties": {
                                "query": {
                                    "type": "string"
                                },
                                "output": {
                                    "type": "string"
                                }
                            },
                            "additionalProperties": false
                        }
                        """)
                .outputKey("sql")
				.enableLogging(true)
                .build();

        ReactAgent sqlRatingAgent = ReactAgent.builder()
                .name("sqlRatingAgent")
                .model(chatModel)
                .description("可以根据输入的自然语言和SQL语句的匹配度进行评分�?)
                .instruction("你是一个熟悉MySQL数据库的小助手，请你根据用户输入的自然语言和对应的SQL语句，输出一个评分。评分为一个浮点数，在0�?之间。越趋近�?说明SQL越匹配自然语言�?)
                .outputType(Double.class)
                .outputKey("score")
				.enableLogging(true)
                .build();

        // 测试放在一个SequentialAgent�?
        SequentialAgent agent = SequentialAgent.builder()
                .name("sql_agent")
                .description("可以根据用户的输入，生成SQL语句，并对其评分�?)
                .subAgents(List.of(sqlGenerateAgent, sqlRatingAgent))
                .build();

        Optional<OverAllState> state = agent.invoke("现在我有一个user表，我想要查询前10个用户，如何写SQL语句�?);
        assertTrue(state.isPresent());
        OverAllState overAllState = state.get();
        assertTrue(overAllState.value("messages").isPresent());
        assertTrue(overAllState.value("sql").isPresent());
        assertTrue(overAllState.value("score").isPresent());
    }

}
