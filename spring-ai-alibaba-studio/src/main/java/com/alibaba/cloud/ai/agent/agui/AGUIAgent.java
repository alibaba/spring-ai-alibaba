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
package com.alibaba.cloud.ai.agent.agui;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static java.util.Objects.requireNonNull;

public class AGUIAgent {
	static final Logger log = LoggerFactory.getLogger(AGUIAgent.class);
	private final AtomicReference<String> streamingId = new AtomicReference<>();

	public final Flux<? extends AGUIEvent> run(BaseAgent agent, AGUIType.RunAgentInput input) {
		try {
			var runnableConfig = RunnableConfig.builder()
					.threadId(input.threadId())
					.addMetadata("user_id", StringUtils.hasLength(input.userId()) ? input.userId() : "user-1")
					.build();

			UserMessage userMessage = buildGraphInput(input);

			Flux<NodeOutput> nodeOutputFlux = agent.stream(userMessage, runnableConfig);

			// Transform NodeOutput flux to AGUIEvent flux
			var finalFlux = nodeOutputFlux
					.concatMap(output -> {
						if (output instanceof StreamingOutput streamingOutput) {
							// Handle streaming output
							var messageId = streamingId.get();
							if (messageId == null) {
								log.trace("STREAMING START");
								messageId = streamingId.updateAndGet(v -> newMessageId());

								if (streamingOutput.chunk() == null || streamingOutput.chunk().isEmpty()) {
									log.trace("STREAMING CHUNK IS EMPTY");
									return Flux.just(new AGUIEvent.TextMessageStartEvent(messageId));
								}
								else {
									log.trace("{}", streamingOutput.chunk());
									return Flux.just(
											new AGUIEvent.TextMessageStartEvent(messageId),
											new AGUIEvent.TextMessageContentEvent(messageId, streamingOutput.chunk())
									);
								}
							}
							else {
								if (streamingOutput.chunk() == null || streamingOutput.chunk().isEmpty()) {
									log.trace("STREAMING CHUNK IS EMPTY");
									return Flux.empty();
								}
								else {
									log.trace("{}", streamingOutput.chunk());
									return Flux.just(new AGUIEvent.TextMessageContentEvent(messageId, streamingOutput.chunk()));
								}
							}
						}
						else {
							// Handle regular NodeOutput
							var lastMerssageId = streamingId.get();
							log.trace("STREAMING END");
							streamingId.set(null);

							var newMessageId = newMessageId();
							var text = nodeOutputToText(output);

							if (text.isPresent()) {
								if (lastMerssageId != null) {
									return Flux.just(
											new AGUIEvent.TextMessageEndEvent(lastMerssageId),
											new AGUIEvent.TextMessageStartEvent(newMessageId),
											new AGUIEvent.TextMessageContentEvent(newMessageId, text.get()),
											new AGUIEvent.TextMessageEndEvent(newMessageId)
									);
								}
								else {
									return Flux.just(
											new AGUIEvent.TextMessageStartEvent(newMessageId),
											new AGUIEvent.TextMessageContentEvent(newMessageId, text.get()),
											new AGUIEvent.TextMessageEndEvent(newMessageId)
									);
								}
							}
							else {
								if (lastMerssageId != null) {
									return Flux.just(
											new AGUIEvent.TextMessageEndEvent(lastMerssageId),
											new AGUIEvent.TextMessageStartEvent(newMessageId),
											new AGUIEvent.TextMessageEndEvent(newMessageId)
									);
								}
								else {
									return Flux.just(
											new AGUIEvent.TextMessageStartEvent(newMessageId),
											new AGUIEvent.TextMessageEndEvent(newMessageId)
									);
								}
							}
						}
					})
					.concatWith(Flux.defer(() -> {
						// Handle completion logic (interruption detection)
						// Note: Since we're using Flux, we don't have access to AsyncGenerator.resultValue
						// This logic may need to be handled differently depending on your requirements
						log.trace("COMPLETE");

						// For now, we'll set no interruption
						// You may need to pass interruption metadata through the flux if needed
//                        GraphData currentGraphData = graphByThread.get(input.threadId());
//                        if (currentGraphData != null) {
//                            graphByThread.put(input.threadId(), currentGraphData.withInterruption(false));
//                        }

						return Flux.empty();
					}));

			return Mono.<AGUIEvent>just(
							new AGUIEvent.RunStartedEvent(input.threadId(), input.runId())
					)
					.concatWith(finalFlux.subscribeOn(Schedulers.single()))
					.concatWith(
							Mono.<AGUIEvent>just(
									new AGUIEvent.RunFinishedEvent(input.threadId(), input.runId())));

		}
		catch (Exception e) {
			return Flux.error(e);
		}

	}

	protected UserMessage buildGraphInput(AGUIType.RunAgentInput input) {

		var lastUserMessage = input.lastUserMessage()
				.map(AGUIMessage.TextMessage::content)
				.orElseThrow(() -> new IllegalStateException("last user message not found"));

		return new UserMessage(lastUserMessage);
	}

//    protected <State extends AgentState> List<Approval> onInterruption(AGUIType.RunAgentInput input, InterruptionMetadata<State> state ) {
//
//        var messages = state.state().<List<Message>>value("messages")
//                .orElseThrow( () -> new IllegalStateException("messages not found into given state"));
//
//        return lastOf(messages)
//                .flatMap(MessageUtil::asAssistantMessage)
//                .filter(AssistantMessage::hasToolCalls)
//                .map(AssistantMessage::getToolCalls)
//                .map( toolCalls ->
//                        toolCalls.stream().map( toolCall -> {
//                            var id = toolCall.id().isBlank() ?
//                                    UUID.randomUUID().toString() :
//                                    toolCall.id();
//                            return new Approval( id, toolCall.name(), toolCall.arguments() );
//                        }).toList()
//                )
//                .orElseGet(List::of);
//
//    }

	protected Optional<String> nodeOutputToText(NodeOutput output) {
		if (output.isEND() || output.isSTART()) {
			return Optional.empty();
		}

		List<Message> messages = output.state().value("messages", List.of());

		// Get the last message from the list
		if (messages == null || messages.isEmpty()) {
			return Optional.empty();
		}

		Message lastMessage = messages.get(messages.size() - 1);

		// Extract text from the message
		if (lastMessage instanceof AssistantMessage assistantMessage) {
			return Optional.ofNullable(assistantMessage.getText());
		}
		else if (lastMessage instanceof UserMessage userMessage) {
			return Optional.ofNullable(userMessage.getText());
		}
		else if (lastMessage instanceof ToolResponseMessage toolResponseMessage) {
			// Handle ToolResponseMessage by combining all tool responses
			StringBuilder toolContent = new StringBuilder();
			for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
				if (!toolContent.isEmpty()) {
					toolContent.append("\n");
				}
				toolContent.append("Tool Response [").append(response.id()).append("]: ");
				toolContent.append(response.responseData());
			}
			return toolContent.isEmpty() ? Optional.empty() : Optional.of(toolContent.toString());
		}
		else {
			// For any other message type, try to get text
			return Optional.ofNullable(lastMessage.getText());
		}
	}

	private String newMessageId() {
		return String.valueOf(System.currentTimeMillis());
	}

	public record Approval(String toolId, String toolName, String toolArgs) {
		public Approval {
			requireNonNull(toolId, "toolId cannot ne bull");
			requireNonNull(toolName, "toolName cannot ne bull");
			requireNonNull(toolArgs, "toolArgs cannot ne bull");
		}
	}

}
