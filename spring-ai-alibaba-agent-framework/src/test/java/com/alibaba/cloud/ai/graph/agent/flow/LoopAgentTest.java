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

package com.alibaba.cloud.ai.graph.agent.flow;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopMode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.model.ChatModel;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class LoopAgentTest {

	private static final Logger logger = LoggerFactory.getLogger(LoopAgentTest.class);

	private ChatModel chatModel;

	private SequentialAgent blogAgent;

    private SequentialAgent sqlAgent;

	@BeforeEach
	void setUp() throws GraphStateException {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

        ReactAgent writerAgent = ReactAgent.builder()
                .name("writer_agent")
                .model(chatModel)
                .description("可以写文章�?)
                .instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答�?)
                .outputKey("article")
                .build();

        ReactAgent reviewerAgent = ReactAgent.builder()
                .name("reviewer_agent")
                .model(chatModel)
                .description("可以对文章进行评论和修改�?)
                .instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。最终只返回修改后的文章，不要包含任何评论信息�?)
                .outputKey("reviewed_article")
                .build();

        this.blogAgent = SequentialAgent.builder()
                .name("blog_agent")
                .description("可以根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论�?)
                .subAgents(List.of(writerAgent, reviewerAgent))
                .build();

        ReactAgent sqlGenerateAgent = ReactAgent.builder()
                .name("sqlGenerateAgent")
                .model(chatModel)
                .description("可以根据用户的自然语言生成MySQL的SQL代码�?)
                .instruction("你是一个熟悉MySQL数据库的小助手，请你根据用户的自然语言，输出对应的SQL�?)
                .outputSchema("""
                        {
                           "query": 用户的请�?
                           "output": 生成SQL结果
                        }
                        """)
                .outputKey("sql")
                .build();

        ReactAgent sqlRatingAgent = ReactAgent.builder()
                .name("sqlRatingAgent")
                .model(chatModel)
                .description("可以根据输入的自然语言和SQL语句的匹配度进行评分�?)
                .instruction("你是一个熟悉MySQL数据库的小助手，请你根据用户输入的自然语言和对应的SQL语句，输出一个评分。评分为一个浮点数，在0�?之间。越趋近�?说明SQL越匹配自然语言�?)
                .outputSchema("你的输出有且仅有一个浮点数，且�?�?之间�?*不要输出任何额外的字�?*")
                .outputKey("score")
                .build();

        this.sqlAgent = SequentialAgent.builder()
                .name("sql_agent")
                .description("可以根据用户的输入，生成SQL语句，并对其评分�?)
                .subAgents(List.of(sqlGenerateAgent, sqlRatingAgent))
                .build();
	}

    @Test
    void testCountMode() throws Exception {
        LoopAgent loopAgent = LoopAgent.builder()
                .name("loop_agent")
                .description("循环执行一个任务，直到满足条件�?)
                .subAgent(this.blogAgent)
                .loopStrategy(LoopMode.count(2))
                .build();
        OverAllState state = loopAgent.invoke("帮我写一个Python Socket编程的demo，并优化代码").orElseThrow();
        logger.info("Result: {}", state.data());
        Optional<Object> optional = state.value("messages");
        assert optional.isPresent();
        Object object = optional.get();
        assert object instanceof List;
        List<?> messages = (List<?>) object;
        assert !messages.isEmpty();
    }

    @Test
    void testConditionMode() throws Exception {
        LoopAgent loopAgent = LoopAgent.builder()
                .name("loop_agent")
                .description("循环执行一个任务，直到满足条件�?)
                .subAgent(this.sqlAgent)
                .loopStrategy(LoopMode.condition(messages -> {
                    logger.info("Messages: {}", messages);
                    if(messages.isEmpty()) {
                        return false;
                    }
                    String text = messages.get(messages.size() - 1).getText();
                    try {
                        double score = Double.parseDouble(text);
                        return score > 0.5;
                    } catch (Exception e) {
                        return false;
                    }
                }))
                .build();
        OverAllState state = loopAgent.invoke("现在有一个用户表，名为user，有列（id, name, password），现在我想要找所有名字以s开头的用户，如何写对应SQL�?).orElseThrow();
        logger.info("Result: {}", state.data());
        Optional<Object> optional = state.value("messages");
        assert optional.isPresent();
        Object object = optional.get();
        assert object instanceof List;
        List<?> messages = (List<?>) object;
        assert !messages.isEmpty();
    }

    @Test
    void testArrayMode() throws Exception {
        LoopAgent loopAgent = LoopAgent.builder()
                .name("loop_agent")
                .description("循环执行任务�?)
                .subAgent(this.sqlAgent)
                .loopStrategy(LoopMode.array())
                .build();
        OverAllState state = loopAgent.invoke("""
                ["现在有一个用户表，名为user，有列（id, name, password），现在我想要找所有名字以s开头的用户，如何写对应SQL�?,
                "现在有一个用户表，名为user，有列（id, name, password），现在我想要找所有名字以t开头的用户，如何写对应SQL�?,
                "现在有一个用户表，名为user，现在我想要找所有用户，如何写对应SQL�?]
                """).orElseThrow();
        logger.info("Result: {}", state.data());
        Optional<Object> optional = state.value("messages");
        assert optional.isPresent();
        Object object = optional.get();
        assert object instanceof List;
        List<?> messages = (List<?>) object;
        assert !messages.isEmpty();
    }

    @Test
    void testLoopAgentWithExecutor() throws Exception {
        ExecutorService customExecutor = Executors.newFixedThreadPool(4);
        try {
            LoopAgent loopAgent = LoopAgent.builder()
                    .name("loop_agent_with_executor")
                    .description("Loop agent with executor")
                    .subAgent(this.blogAgent)
                    .loopStrategy(LoopMode.count(2))
                    .executor(customExecutor)
                    .build();

            assertNotNull(loopAgent, "LoopAgent should not be null");

            // Verify executor is set and passed to RunnableConfig
            RunnableConfig config = buildNonStreamConfig(loopAgent, null);
            assertNotNull(config, "RunnableConfig should not be null");
            
            assertTrue(config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent(),
                "Default parallel executor should be present in metadata");
            assertEquals(customExecutor, 
                config.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get(),
                "Executor in metadata should match configured executor");
        } finally {
            customExecutor.shutdown();
        }
    }

    @Test
    void testLoopAgentExecutorWithExistingConfig() throws Exception {
        Executor customExecutor = Executors.newFixedThreadPool(4);

        LoopAgent loopAgent = LoopAgent.builder()
                .name("loop_agent_executor_config")
                .description("Loop agent with executor and existing config")
                .subAgent(this.sqlAgent)
                .loopStrategy(LoopMode.count(1))
                .executor(customExecutor)
                .build();

        // Create an existing RunnableConfig
        RunnableConfig existingConfig = RunnableConfig.builder()
                .threadId("test-thread")
                .build();

        // Build config with existing config
        RunnableConfig newConfig = buildNonStreamConfig(loopAgent, existingConfig);
        
        // Verify existing config properties are preserved
        assertTrue(newConfig.threadId().isPresent());
        assertEquals("test-thread", newConfig.threadId().get());
        
        // Verify executor is added
        assertTrue(newConfig.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).isPresent());
        assertEquals(customExecutor, 
            newConfig.metadata(RunnableConfig.DEFAULT_PARALLEL_EXECUTOR_KEY).get());
    }

    /**
     * Helper method to call protected buildNonStreamConfig using reflection.
     */
    private RunnableConfig buildNonStreamConfig(Agent agent, RunnableConfig config) throws Exception {
        Method method = Agent.class.getDeclaredMethod("buildNonStreamConfig", RunnableConfig.class);
        method.setAccessible(true);
        return (RunnableConfig) method.invoke(agent, config);
    }

}
