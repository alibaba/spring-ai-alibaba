package com.alibaba.cloud.ai.observation.model;

import static com.alibaba.cloud.ai.observation.model.semconv.MessageMode.LANGFUSE;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import com.alibaba.cloud.ai.observation.model.semconv.InputOutputModel.ChatMessage;
import com.alibaba.cloud.ai.observation.model.semconv.InputOutputUtils;
import com.alibaba.cloud.ai.observation.model.semconv.MessageMode;
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

public class ChatModelInputObservationHandler implements ObservationHandler<ChatModelObservationContext> {

	private static final Logger logger = LoggerFactory.getLogger(ChatModelInputObservationHandler.class);

	private final AttributeKey<String> inputMessagesKey;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public ChatModelInputObservationHandler(MessageMode mode) {
		if (mode == LANGFUSE) {
			inputMessagesKey = stringKey("input.values");
		}
		else {
			inputMessagesKey = stringKey("gen_ai.input.messages");
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
				otelSpan.setAttribute(inputMessagesKey, outputMessages);
			}
		}
	}

	@Nullable
	private String getOutputMessages(ChatModelObservationContext context) {
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
