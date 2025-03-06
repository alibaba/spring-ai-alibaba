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
package com.alibaba.cloud.ai.dashscope.agent;

import com.alibaba.cloud.ai.agent.Agent;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi.DashScopeAgentRequest;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi.DashScopeAgentResponse;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestInput.DashScopeAgentRequestMessage;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestParameters.DashScopeAgentRequestRagOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Title Dashscope low level agent.<br>
 * Description Dashscope low level agent.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public final class DashScopeAgent extends Agent {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeAgent.class);

	public static final String REQUEST_ID = "request_id";

	public static final String USAGE = "usage";

	public static final String OUTPUT = "output";

	private final DashScopeAgentOptions options;

	private final DashScopeAgentApi dashScopeAgentApi;

	public DashScopeAgent(DashScopeAgentApi dashScopeAgentApi) {
		this.dashScopeAgentApi = dashScopeAgentApi;
		this.options = DashScopeAgentOptions.builder()
			.withSessionId(null)
			.withMemoryId(null)
			.withIncrementalOutput(false)
			.withHasThoughts(false)
			.withImages(null)
			.withBizParams(null)
			.build();
	}

	public DashScopeAgent(DashScopeAgentApi dashScopeAgentApi, DashScopeAgentOptions options) {
		this.dashScopeAgentApi = dashScopeAgentApi;
		this.options = options;
	}

	@Override
	public ChatResponse call(Prompt prompt) {
		DashScopeAgentRequest request = toRequest(prompt, false);

		ResponseEntity<DashScopeAgentResponse> response = this.dashScopeAgentApi.call(request);

		if (response == null || response.getBody() == null) {
			logger.warn("app call error: request: {}", request);
			return null;
		}

		return toChatResponse(response.getBody());
	}

	@Override
	public Flux<ChatResponse> stream(Prompt prompt) {
		DashScopeAgentRequest request = toRequest(prompt, true);

		Flux<DashScopeAgentResponse> response = this.dashScopeAgentApi.stream(request);

		return Flux.from(response)
			.flatMap(result -> Flux.just(toChatResponse(result)))
			.publishOn(Schedulers.parallel());
	}

	private DashScopeAgentRequest toRequest(Prompt prompt, Boolean stream) {
		if (prompt == null) {
			throw new IllegalArgumentException("option is null");
		}

		DashScopeAgentOptions runtimeOptions = mergeOptions(prompt.getOptions());
		String appId = runtimeOptions.getAppId();

		if (appId == null || appId.isEmpty()) {
			throw new IllegalArgumentException("appId must be set");
		}

		DashScopeAgentRagOptions ragOptions = runtimeOptions.getRagOptions();
		return new DashScopeAgentRequest(appId,
				new DashScopeAgentRequest.DashScopeAgentRequestInput(null, prompt.getInstructions()
					.stream()
					.map(msg -> new DashScopeAgentRequestMessage(msg.getMessageType().getValue(), msg.getText()))
					.toList(), runtimeOptions.getSessionId(), runtimeOptions.getMemoryId(), runtimeOptions.getImages(),
						runtimeOptions.getBizParams()),
				new DashScopeAgentRequest.DashScopeAgentRequestParameters(runtimeOptions.getHasThoughts(),
						stream && runtimeOptions.getIncrementalOutput(),
						ragOptions == null ? null
								: new DashScopeAgentRequestRagOptions(ragOptions.getPipelineIds(),
										ragOptions.getFileIds(), ragOptions.getMetadataFilter(), ragOptions.getTags(),
										ragOptions.getStructuredFilter(), ragOptions.getSessionFileIds())));
	}

	private ChatResponse toChatResponse(DashScopeAgentResponse response) {
		DashScopeAgentResponse.DashScopeAgentResponseOutput output = response.output();
		DashScopeAgentResponse.DashScopeAgentResponseUsage usage = response.usage();
		String text = output.text();

		if (text == null) {
			text = "";
		}

		Map<String, Object> metadata = new HashMap<>();
		metadata.put(REQUEST_ID, response.requestId());
		metadata.put(USAGE, usage);
		metadata.put(OUTPUT, output);

		var assistantMessage = new AssistantMessage(text, metadata);
		var generationMetadata = ChatGenerationMetadata.builder().finishReason(output.finishReason()).build();
		Generation generation = new Generation(assistantMessage, generationMetadata);

		return new ChatResponse(List.of(generation));
	}

	private DashScopeAgentOptions mergeOptions(ChatOptions chatOptions) {
		DashScopeAgentOptions agentOptions = ModelOptionsUtils.copyToTarget(chatOptions, ChatOptions.class,
				DashScopeAgentOptions.class);
		return ModelOptionsUtils.merge(agentOptions, this.options, DashScopeAgentOptions.class);
	}

}
