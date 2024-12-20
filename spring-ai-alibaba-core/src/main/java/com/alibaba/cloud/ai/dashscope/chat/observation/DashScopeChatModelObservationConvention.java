package com.alibaba.cloud.ai.dashscope.chat.observation;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.fastjson.JSON;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Dashscope conventions to populate observations for chat model operations.
 *
 * @author Lumian
 * @since 1.0.0
 */
public class DashScopeChatModelObservationConvention extends DefaultChatModelObservationConvention {

	public static final String DEFAULT_NAME = "gen_ai.client.operation";

	private static final String ILLEGAL_STOP_CONTENT = "<illegal_stop_content>";

	@Override
	public String getName() {
		return DEFAULT_NAME;
	}

	// Request

	protected KeyValues requestStopSequences(KeyValues keyValues, ChatModelObservationContext context) {
		if (context.getRequestOptions() instanceof DashScopeChatOptions) {
			List<Object> stop = ((DashScopeChatOptions) context.getRequestOptions()).getStop();
			if (CollectionUtils.isEmpty(stop)) {
				return keyValues;
			}
			KeyValue.of(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_STOP_SEQUENCES, stop,
					Objects::nonNull);

			String stopSequences;
			try {
				stopSequences = JSON.toJSONString(stop);
			}
			catch (Exception e) {
				stopSequences = ILLEGAL_STOP_CONTENT;
			}
			return keyValues.and(
					ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_STOP_SEQUENCES.asString(),
					stopSequences);
		}

		return super.requestStopSequences(keyValues, context);
	}

}
