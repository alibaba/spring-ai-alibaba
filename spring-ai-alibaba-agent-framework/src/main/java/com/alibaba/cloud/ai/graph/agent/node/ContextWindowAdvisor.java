/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.cloud.ai.graph.agent.context.ContextWindowExceededException;
import com.alibaba.cloud.ai.graph.agent.context.ContextWindowPolicy;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallResults;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.part.MessagePart;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallDecisions;

import reactor.core.publisher.Flux;

final class ContextWindowAdvisor implements CallAdvisor, StreamAdvisor {

	private static final String CLEARED_RESULT = "[Earlier tool result omitted]";

	private final ContextWindowPolicy policy;

	private final RequestToolScope.ToolsAdvisor capture;

	ContextWindowAdvisor(ContextWindowPolicy policy, RequestToolScope.ToolsAdvisor capture) {
		this.policy = policy;
		this.capture = capture;
	}

	@Override
	public String getName() {
		return "ModelContextWindow";
	}

	@Override
	public int getOrder() {
		return this.capture.getOrder();
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		Attempt first = prepare(request);
		try {
			return chain.nextCall(first.request());
		}
		catch (RuntimeException ex) {
			if (!this.policy.isInputOverflow(ex)) {
				throw ex;
			}
			Attempt retry = retryAttempt(first, ex);
			try {
				return chain.copy((CallAdvisor) this).nextCall(retry.request());
			}
			catch (RuntimeException retryError) {
				if (this.policy.isInputOverflow(retryError)) {
					throw exceeded(retry, retryError);
				}
				throw retryError;
			}
		}
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
		return Flux.defer(() -> {
			Attempt first = prepare(request);
			AtomicBoolean emitted = new AtomicBoolean();
			return Flux.defer(() -> chain.nextStream(first.request()))
				.doOnNext(response -> emitted.set(true))
				.onErrorResume(this.policy::isInputOverflow, error -> {
					if (emitted.get()) {
						return Flux.error(exceeded(first, error));
					}
					Attempt retry = retryAttempt(first, error);
					return Flux.defer(() -> chain.copy((StreamAdvisor) this).nextStream(retry.request()))
						.onErrorMap(this.policy::isInputOverflow, retryError -> exceeded(retry, retryError));
				});
		});
	}

	private Attempt prepare(ChatClientRequest request) {
		ChatClientRequest outbound = this.capture.prepare().apply(request);
		Attempt attempt = new Attempt(outbound, this.policy.estimateInputTokens(outbound.prompt()));
		if (attempt.tokens() > this.policy.targetInputTokens()) {
			attempt = compact(attempt);
		}
		if (attempt.tokens() > this.policy.maxInputTokens()) {
			throw exceeded(attempt, null);
		}
		return attempt;
	}

	private Attempt retryAttempt(Attempt first, Throwable cause) {
		Attempt retry = compact(first);
		if (retry.tokens() >= first.tokens() || retry.tokens() > this.policy.maxInputTokens()) {
			throw exceeded(first, cause);
		}
		return retry;
	}

	private ContextWindowExceededException exceeded(Attempt attempt, Throwable cause) {
		return new ContextWindowExceededException(attempt.tokens(), this.policy.maxInputTokens(), cause);
	}

	private Attempt compact(Attempt attempt) {
		Prompt original = attempt.request().prompt();
		List<Message> messages = original.getInstructions();
		int currentTurn = messages.size() - 1;
		while (currentTurn >= 0 && messages.get(currentTurn).getMessageType() != MessageType.USER) {
			currentTurn--;
		}
		List<Message> edited = new ArrayList<>(messages);
		for (int i = 0; i < currentTurn; i++) {
			if (!(messages.get(i) instanceof AssistantMessage assistant) || !assistant.hasToolCalls()) {
				continue;
			}
			int end = ToolCallResults.responseEnd(messages, i);
			List<ToolResultPart> results = new ArrayList<>();
			for (int j = i + 1; j < end; j++) {
				results.addAll(((ToolResponseMessage) messages.get(j)).getResults());
			}
			if (ToolCallResults.settledKeys(assistant, results).containsAll(ToolCallDecisions.toolCallKeys(assistant))) {
				for (int j = i + 1; j < end; j++) {
					edited.set(j, shorten((ToolResponseMessage) messages.get(j)));
				}
			}
			i = end - 1;
		}
		if (edited.equals(messages)) {
			return attempt;
		}
		Prompt prompt = new Prompt(edited, original.getOptions());
		int tokens = this.policy.estimateInputTokens(prompt);
		return tokens < attempt.tokens()
				? new Attempt(new ChatClientRequest(prompt, attempt.request().context()), tokens) : attempt;
	}

	private ToolResponseMessage shorten(ToolResponseMessage response) {
		List<MessagePart> parts = response.getParts().stream()
			.map(part -> part instanceof ToolResultPart result ? shorten(result) : part)
			.toList();
		return parts.equals(response.getParts()) ? response
				: ToolResponseMessage.builder().parts(parts).metadata(response.getMetadata()).build();
	}

	private ToolResultPart shorten(ToolResultPart result) {
		if (result.payload() != null || result.isError()
				|| result.content().stream().anyMatch(part -> !(part instanceof TextPart))) {
			return result;
		}
		long length = result.content().stream().map(TextPart.class::cast).mapToLong(part -> part.text().length()).sum();
		if (length <= CLEARED_RESULT.length()) {
			return result;
		}
		return new ToolResultPart(result.id(), result.name(), List.of(TextPart.of(CLEARED_RESULT)), false,
				null, result.attributes());
	}

	private record Attempt(ChatClientRequest request, int tokens) {
	}

}
