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

package com.alibaba.cloud.ai.observation.model;

import static com.alibaba.cloud.ai.observation.model.semconv.MessageMode.LANGFUSE;

import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.OutputMessage;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputUtils;
import com.alibaba.cloud.ai.observation.model.semconv.MessageMode;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatModelOutputObservationHandler implements ObservationHandler<ChatModelObservationContext> {

	private static final Logger logger = LoggerFactory.getLogger(ChatModelOutputObservationHandler.class);

	private final AttributeKey<String> outputMessagesKey;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public ChatModelOutputObservationHandler(MessageMode mode) {
		if (mode == LANGFUSE) {
			outputMessagesKey = AttributeKey.stringKey("output.values");
		}
		else {
			outputMessagesKey = AttributeKey.stringKey("gen_ai.output.messages");
		}
	}

	@Override
	public void onStop(ChatModelObservationContext context) {
		TracingObservationHandler.TracingContext tracingContext = context
			.get(TracingObservationHandler.TracingContext.class);
		Span otelSpan = OpenTelemetrySpanBridge.retrieveOtelSpan(tracingContext);

		if (otelSpan != null) {
			String outputMessages = getOutputMessages(context);
			if (outputMessages != null) {
				otelSpan.setAttribute(outputMessagesKey, outputMessages);
			}
		}
	}

	@Nullable
	private String getOutputMessages(ChatModelObservationContext context) {
		if (context.getResponse() == null || context.getResponse().getResults() == null
				|| CollectionUtils.isEmpty(context.getResponse().getResults())) {
			return null;
		}

		if (!StringUtils.hasText(context.getResponse().getResult().getOutput().getText())) {
			return "";
		}

		List<OutputMessage> messages = context.getResponse()
			.getResults()
			.stream()
			.filter(generation -> generation.getOutput() != null && generation.getMetadata() != null)
			.map(InputOutputUtils::convertFromGeneration)
			.toList();

		try {
			return objectMapper.writeValueAsString(messages);
		}
		catch (JsonProcessingException e) {
			logger.warn("Failed to convert output message to JSON string", e);
			return null;
		}
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof ChatModelObservationContext;
	}

}
