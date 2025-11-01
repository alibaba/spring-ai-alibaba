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

package com.alibaba.cloud.ai.graph.agent.flow;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopMode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class LoopAgentTest {

	private static final Logger logger = LoggerFactory.getLogger(LoopAgentTest.class);

	private ChatModel chatModel;

	private SequentialAgent blogAgent;

	@BeforeEach
	void setUp() throws GraphStateException {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

        ReactAgent writerAgent = ReactAgent.builder()
                .name("writer_agent")
                .model(chatModel)
                .description("可以写文章。")
                .instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
                .outputKey("article")
                .build();

        ReactAgent reviewerAgent = ReactAgent.builder()
                .name("reviewer_agent")
                .model(chatModel)
                .description("可以对文章进行评论和修改。")
                .instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。最终只返回修改后的文章，不要包含任何评论信息。")
                .outputKey("reviewed_article")
                .build();

        this.blogAgent = SequentialAgent.builder()
                .name("blog_agent")
                .description("可以根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论。")
                .subAgents(List.of(writerAgent, reviewerAgent))
                .build();
	}

    @Test
    void testLoopAgent() throws Exception {
        LoopAgent loopAgent = LoopAgent.builder()
                .name("loop_agent")
                .description("循环执行一个任务，直到满足条件。")
                .subAgent(this.blogAgent)
                .loopStrategy(LoopMode.count(3))
                .build();
        OverAllState state = loopAgent.invoke("写一篇关于杭州西湖的散文文章。").orElseThrow();
        logger.info("Result: {}", state.data());
    }

}
