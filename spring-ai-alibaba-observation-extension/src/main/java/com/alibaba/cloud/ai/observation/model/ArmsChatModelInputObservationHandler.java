package com.alibaba.cloud.ai.observation.model;

import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.ChatMessage;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class ArmsChatModelInputObservationHandler implements ObservationHandler<ChatModelObservationContext> {

	private static final Logger logger = LoggerFactory.getLogger(ArmsChatModelInputObservationHandler.class);

	private static final AttributeKey<String> GEN_AI_INPUT_MESSAGES = AttributeKey.stringKey("gen_ai.input.messages");

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void onStop(ChatModelObservationContext context) {
		TracingObservationHandler.TracingContext tracingContext = context
			.get(TracingObservationHandler.TracingContext.class);
		Span otelSpan = OpenTelemetrySpanBridge.retrieveOtelSpan(tracingContext);

		if (otelSpan != null) {
			String outputMessages = getInputMessages(context);
			if (outputMessages != null) {
				otelSpan.setAttribute(GEN_AI_INPUT_MESSAGES, outputMessages);
			}
		}
	}

	@Nullable
	private String getInputMessages(ChatModelObservationContext context) {
		if (CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
			return null;
		}

		List<ChatMessage> messages = context.getRequest()
			.getInstructions()
			.stream()
			.map(InputOutputUtils::convertFromMessage)
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
