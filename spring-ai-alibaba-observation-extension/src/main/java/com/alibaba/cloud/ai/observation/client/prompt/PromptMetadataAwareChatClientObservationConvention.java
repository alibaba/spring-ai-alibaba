package com.alibaba.cloud.ai.observation.client.prompt;

import static com.alibaba.cloud.ai.observation.client.prompt.MetadataAttributes.AGENT_NAME;
import static com.alibaba.cloud.ai.observation.client.prompt.MetadataAttributes.PROMPT_KEY;
import static com.alibaba.cloud.ai.observation.client.prompt.MetadataAttributes.PROMPT_VERSION;
import static com.alibaba.cloud.ai.observation.client.prompt.MetadataAttributes.STUDIO_SOURCE;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import java.util.Collections;
import java.util.Map;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.lang.NonNull;

public class PromptMetadataAwareChatClientObservationConvention extends DefaultChatClientObservationConvention {

	@Override
	@NonNull
	public KeyValues getLowCardinalityKeyValues(@NonNull ChatClientObservationContext context) {
		return super.getLowCardinalityKeyValues(context);
	}

	@Override
	@NonNull
	public KeyValues getHighCardinalityKeyValues(@NonNull ChatClientObservationContext context) {
		KeyValues keyValues = super.getHighCardinalityKeyValues(context);
		keyValues = promptKey(keyValues, context);
		keyValues = promptVersion(keyValues, context);
		keyValues = agentName(keyValues, context);
		keyValues = studioSource(keyValues, context);
		return keyValues;
	}

	// request

	protected KeyValues promptKey(KeyValues keyValues, ChatClientObservationContext context) {
		Map<String, String> metadata = this.getMetadata(context);
		if (metadata.containsKey("promptKey")) {
			return keyValues.and(KeyValue.of(PROMPT_KEY, metadata.get("promptKey")));
		}
		return keyValues;
	}

	protected KeyValues promptVersion(KeyValues keyValues, ChatClientObservationContext context) {
		Map<String, String> metadata = this.getMetadata(context);
		if (metadata.containsKey("promptVersion")) {
			return keyValues.and(KeyValue.of(PROMPT_VERSION, metadata.get("promptVersion")));
		}
		return keyValues;
	}

	protected KeyValues agentName(KeyValues keyValues, ChatClientObservationContext context) {
		Map<String, String> metadata = this.getMetadata(context);
		if (metadata.containsKey("agentName")) {
			return keyValues.and(KeyValue.of(AGENT_NAME, metadata.get("agentName")));
		}
		return keyValues;
	}

	protected KeyValues studioSource(KeyValues keyValues, ChatClientObservationContext context) {
		Map<String, String> metadata = this.getMetadata(context);
		if (metadata.containsKey("studioSource")) {
			return keyValues.and(KeyValue.of(STUDIO_SOURCE, metadata.get("studioSource")));
		}
		return keyValues;
	}

	// FIXME remove openai assert
	private Map<String, String> getMetadata(ChatClientObservationContext context) {
		ChatOptions options = context.getRequest().prompt().getOptions();
		if (options instanceof OpenAiChatOptions) {
			Map<String, String> metadata = ((OpenAiChatOptions) options).getMetadata();
			if (metadata != null) {
				return metadata;
			}
		}
		return Collections.emptyMap();
	}

}
