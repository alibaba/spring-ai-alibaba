package com.alibaba.cloud.ai.observation.model;

import static com.alibaba.cloud.ai.observation.constants.MetadataAttributes.AGENT_IP;
import static com.alibaba.cloud.ai.observation.constants.MetadataAttributes.AGENT_NAME;
import static com.alibaba.cloud.ai.observation.constants.MetadataAttributes.PROMPT_KEY;
import static com.alibaba.cloud.ai.observation.constants.MetadataAttributes.PROMPT_TEMPLATE;
import static com.alibaba.cloud.ai.observation.constants.MetadataAttributes.PROMPT_VARIABLE;
import static com.alibaba.cloud.ai.observation.constants.MetadataAttributes.PROMPT_VERSION;
import static com.alibaba.cloud.ai.observation.constants.MetadataAttributes.STUDIO_SOURCE;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import java.util.Collections;
import java.util.Map;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.lang.NonNull;

public class PromptMetadataAwareChatModelObservationConvention extends DefaultChatModelObservationConvention {

	@Override
	@NonNull
	public KeyValues getLowCardinalityKeyValues(@NonNull ChatModelObservationContext context) {
		return super.getLowCardinalityKeyValues(context);
	}

	@Override
	@NonNull
	public KeyValues getHighCardinalityKeyValues(@NonNull ChatModelObservationContext context) {
		KeyValues keyValues = super.getHighCardinalityKeyValues(context);
		keyValues = promptKey(keyValues, context);
		keyValues = promptVersion(keyValues, context);
		keyValues = promptTemplate(keyValues, context);
		keyValues = promptVariables(keyValues, context);
		keyValues = agentName(keyValues, context);
		keyValues = agentIp(keyValues, context);
		keyValues = studioSource(keyValues, context);

		return keyValues;
	}

	// request

	protected KeyValues promptKey(KeyValues keyValues, ChatModelObservationContext context) {
		Map<String, String> metadata = this.getMetadata(context);
		if (metadata.containsKey("promptKey")) {
			return keyValues.and(KeyValue.of(PROMPT_KEY, metadata.get("promptKey")));
		}
		return keyValues;
	}

	protected KeyValues promptVersion(KeyValues keyValues, ChatModelObservationContext context) {
		Map<String, String> metadata = this.getMetadata(context);
		if (metadata.containsKey("promptVersion")) {
			return keyValues.and(KeyValue.of(PROMPT_VERSION, metadata.get("promptVersion")));
		}
		return keyValues;
	}

	protected KeyValues promptTemplate(KeyValues keyValues, ChatModelObservationContext context) {
		Map<String, String> metadata = this.getMetadata(context);
		if (metadata.containsKey("promptTemplate")) {
			return keyValues.and(KeyValue.of(PROMPT_TEMPLATE, metadata.get("promptTemplate")));
		}
		return keyValues;
	}

	protected KeyValues promptVariables(KeyValues keyValues, ChatModelObservationContext context) {
		Map<String, String> metadata = this.getMetadata(context);
		if (metadata.containsKey("promptVariables")) {
			return keyValues.and(KeyValue.of(PROMPT_VARIABLE, metadata.get("promptVariables")));
		}
		return keyValues;
	}

	protected KeyValues agentName(KeyValues keyValues, ChatModelObservationContext context) {
		Map<String, String> metadata = this.getMetadata(context);
		if (metadata.containsKey("agentName")) {
			return keyValues.and(KeyValue.of(AGENT_NAME, metadata.get("agentName")));
		}
		return keyValues;
	}

	protected KeyValues agentIp(KeyValues keyValues, ChatModelObservationContext context) {
		Map<String, String> metadata = this.getMetadata(context);
		if (metadata.containsKey("agentIp")) {
			return keyValues.and(KeyValue.of(AGENT_IP, metadata.get("agentIp")));
		}
		return keyValues;
	}

	protected KeyValues studioSource(KeyValues keyValues, ChatModelObservationContext context) {
		Map<String, String> metadata = this.getMetadata(context);
		if (metadata.containsKey("studioSource")) {
			return keyValues.and(KeyValue.of(STUDIO_SOURCE, metadata.get("studioSource")));
		}
		return keyValues;
	}

	// FIXME remove openai assert
	private Map<String, String> getMetadata(ChatModelObservationContext context) {
		ChatOptions options = context.getRequest().getOptions();
		if (options instanceof OpenAiChatOptions) {
			Map<String, String> metadata = ((OpenAiChatOptions) options).getMetadata();
			if (metadata != null) {
				return metadata;
			}
		}
		return Collections.emptyMap();
	}

}
