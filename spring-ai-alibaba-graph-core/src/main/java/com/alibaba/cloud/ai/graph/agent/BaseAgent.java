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
package com.alibaba.cloud.ai.graph.agent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.ChatResponse;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;

import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.Trigger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Abstract base class for all agents in the graph system. Contains common properties and
 * methods shared by different agent implementations.
 */
public abstract class BaseAgent {

	/** The agent's name. Must be a unique identifier within the graph. */
	protected String name;

	/**
	 * One line description about the agent's capability. The system can use this for
	 * decision-making when delegating control to different agents.
	 */
	protected String description;

	/** The output key for the agent's result */
	protected String outputKey;

	/**
	 * Protected constructor for initializing all base agent properties.
	 * @param name the unique name of the agent
	 * @param description the description of the agent's capability
	 * @param outputKey the output key for the agent's result
	 */
	protected BaseAgent(String name, String description, String outputKey) {
		this.name = name;
		this.description = description;
		this.outputKey = outputKey;
	}

	/**
	 * Default protected constructor for subclasses that need to initialize properties
	 * differently.
	 */
	protected BaseAgent() {
		// Allow subclasses to initialize properties through other means
	}

	/**
	 * Gets the agent's unique name.
	 * @return the unique name of the agent.
	 */
	public String name() {
		return name;
	}

	/**
	 * Gets the one-line description of the agent's capability.
	 * @return the description of the agent.
	 */
	public String description() {
		return description;
	}

	/**
	 * Gets the output key for the agent's result.
	 * @return the output key.
	 */
	public String outputKey() {
		return outputKey;
	}

	/**
	 * Abstract a complex agent into a simple node in the graph.
	 * @return the list of sub-agents.
	 */
	public abstract AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent)
			throws GraphStateException;

	public Optional<OverAllState> invoke(Map<String, Object> input) throws GraphStateException, GraphRunnerException {
		return invoke(input, RunnableConfig.builder().build());
	}

	public abstract Optional<OverAllState> invoke(Map<String, Object> input, RunnableConfig config)
			throws GraphStateException, GraphRunnerException;

	/**
	 * Schedule the agent task with trigger.
	 * @param trigger the schedule configuration
	 * @param input the agent input
	 * @return a ScheduledAgentTask instance for managing the scheduled task
	 */
	public ScheduledAgentTask schedule(Trigger trigger, Map<String, Object> input)
			throws GraphStateException, GraphRunnerException {
		ScheduleConfig scheduleConfig = ScheduleConfig.builder().trigger(trigger).inputs(input).build();
		return schedule(scheduleConfig);
	}

	/**
	 * Schedule the agent task with trigger.
	 * @param scheduleConfig the schedule configuration
	 * @return a ScheduledAgentTask instance for managing the scheduled task
	 */
	public abstract ScheduledAgentTask schedule(ScheduleConfig scheduleConfig)
			throws GraphStateException, GraphRunnerException;

	public AsyncGenerator<NodeOutput> stream(Map<String, Object> input)
			throws GraphStateException, GraphRunnerException {
		return stream(input, RunnableConfig.builder().build());
	}

	public abstract AsyncGenerator<NodeOutput> stream(Map<String, Object> input, RunnableConfig config)
			throws GraphStateException, GraphRunnerException;

	public Flux<ChatResponse> fluxStream(Map<String, Object> input) throws GraphStateException, GraphRunnerException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Logger logger = LoggerFactory.getLogger(BaseAgent.class);
		Sinks.Many<ChatResponse> sink = Sinks.many().unicast().onBackpressureBuffer();
		AsyncGenerator<NodeOutput> generator = stream(input);
		executor.submit(() -> {
			generator.forEachAsync(output -> {
				try {
					logger.info("output = {}", output);
					String nodeName = output.node();
					String content;
					if (output instanceof StreamingOutput streamingOutput) {
						content = JSON.toJSONString(Map.of(nodeName, streamingOutput.chunk()));
					}
					else {
						JSONObject nodeOutput = new JSONObject();
						nodeOutput.put("data", output.state().data());
						nodeOutput.put("node", nodeName);
						content = JSON.toJSONString(nodeOutput);
					}
					sink.tryEmitNext(ChatResponse.builder().data(content).build());
				}
				catch (Exception e) {
					throw new CompletionException(e);
				}
			}).thenAccept(v -> {
				sink.tryEmitComplete();
				executor.shutdown(); // 关闭线程池
			}).exceptionally(e -> {
				sink.tryEmitError(e);
				executor.shutdown(); // 关闭线程池
				return null;
			});
		});
		return sink.asFlux().doOnCancel(() -> {
			logger.info("Client disconnected from stream");
			executor.shutdown(); // 关闭线程池
		}).doOnError(e -> {
			logger.error("Error occurred during streaming", e);
			executor.shutdown(); // 关闭线程池
		});
	}

	public Flux<ServerSentEvent<String>> sseStream(Map<String, Object> input)
			throws GraphStateException, GraphRunnerException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Logger logger = LoggerFactory.getLogger(BaseAgent.class);
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		AsyncGenerator<NodeOutput> generator = stream(input);
		executor.submit(() -> {
			generator.forEachAsync(output -> {
				try {
					logger.info("output = {}", output);
					String nodeName = output.node();
					String content;
					if (output instanceof StreamingOutput streamingOutput) {
						content = JSON.toJSONString(Map.of(nodeName, streamingOutput.chunk()));
					}
					else {
						JSONObject nodeOutput = new JSONObject();
						nodeOutput.put("data", output.state().data());
						nodeOutput.put("node", nodeName);
						content = JSON.toJSONString(nodeOutput);
					}
					sink.tryEmitNext(ServerSentEvent.builder(content).build());
				}
				catch (Exception e) {
					throw new CompletionException(e);
				}
			}).thenAccept(v -> {
				sink.tryEmitComplete();
				executor.shutdown(); // 关闭线程池
			}).exceptionally(e -> {
				sink.tryEmitError(e);
				executor.shutdown(); // 关闭线程池
				return null;
			});
		});
		return sink.asFlux().doOnCancel(() -> {
			logger.info("Client disconnected from stream");
			executor.shutdown(); // 关闭线程池
		}).doOnError(e -> {
			logger.error("Error occurred during streaming", e);
			executor.shutdown(); // 关闭线程池
		});
	}

}
