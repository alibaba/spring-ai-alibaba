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
package com.alibaba.cloud.ai.tool.observation;

import com.alibaba.cloud.ai.tool.observation.ArmsToolCallingObservationDocumentation.HighCardinalityKeyNames;
import com.alibaba.cloud.ai.tool.observation.ArmsToolCallingObservationDocumentation.LowCardinalityKeyNames;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;
import org.springframework.util.StringUtils;

public class ArmsToolCallingObservationConvention implements ObservationConvention<ArmsToolCallingObservationContext> {

	public static final String DEFAULT_OPERATION_NAME = "execute_tool";

	public static final String SPAN_KIND = "TOOL";

	public static final String FRAMEWORK = "spring ai alibaba";

	@Override
	public boolean supportsContext(Context context) {
		return context instanceof ArmsToolCallingObservationContext;
	}

	@Override
	public String getName() {
		return DEFAULT_OPERATION_NAME;
	}

	@Override
	public String getContextualName(ArmsToolCallingObservationContext context) {
		if (StringUtils.hasText(context.getToolCall().name())) {
			return "%s %s".formatted(DEFAULT_OPERATION_NAME, context.getToolCall().name());
		}
		return DEFAULT_OPERATION_NAME;
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ArmsToolCallingObservationContext context) {
		return KeyValues.of(aiOperationType(context), spanKind(context), framework(context));
	}

	protected KeyValue aiOperationType(ArmsToolCallingObservationContext context) {
		return KeyValue.of(LowCardinalityKeyNames.AI_OPERATION_TYPE, DEFAULT_OPERATION_NAME);
	}

	protected KeyValue spanKind(ArmsToolCallingObservationContext context) {
		return KeyValue.of(LowCardinalityKeyNames.GEN_AI_SPAN_KIND, SPAN_KIND);
	}

	protected KeyValue framework(ArmsToolCallingObservationContext context) {
		return KeyValue.of(LowCardinalityKeyNames.GEN_AI_FRAMEWORK, FRAMEWORK);
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(ArmsToolCallingObservationContext context) {
		var keyValues = KeyValues.empty();
		// Request
		keyValues = toolCallId(keyValues, context);
		keyValues = toolCallName(keyValues, context);
		keyValues = toolDescription(keyValues, context);
		keyValues = toolParameters(keyValues, context);
		// Response
		keyValues = returnDirect(keyValues, context);
		keyValues = outputValue(keyValues, context);
		return keyValues;
	}

	// Request

	protected KeyValues toolCallId(KeyValues keyValues, ArmsToolCallingObservationContext context) {
		if (context.getToolCall().id() != null) {
			return keyValues.and(HighCardinalityKeyNames.GEN_AI_TOOL_CALL_ID.asString(),
					String.valueOf(context.getToolCall().id()));
		}
		return keyValues;
	}

	protected KeyValues toolCallName(KeyValues keyValues, ArmsToolCallingObservationContext context) {
		if (context.getToolCall().name() != null) {
			return keyValues
				.and(HighCardinalityKeyNames.GEN_AI_TOOL_NAME.asString(), String.valueOf(context.getToolCall().name()))
				.and(HighCardinalityKeyNames.TOOL_NAME.asString(), String.valueOf(context.getToolCall().name()));
		}
		return keyValues;
	}

	protected KeyValues toolDescription(KeyValues keyValues, ArmsToolCallingObservationContext context) {
		return keyValues.and(HighCardinalityKeyNames.TOOL_DESCRIPTION.asString(), context.getDescription());
	}

	protected KeyValues toolParameters(KeyValues keyValues, ArmsToolCallingObservationContext context) {
		if (context.getToolCall().arguments() != null) {
			return keyValues.and(HighCardinalityKeyNames.TOOL_PARAMETERS.asString(), context.getToolCall().arguments());
		}
		return keyValues;
	}

	protected KeyValues returnDirect(KeyValues keyValues, ArmsToolCallingObservationContext context) {
		return keyValues.and(HighCardinalityKeyNames.TOOL_RETURN_DIRECT.asString(),
				String.valueOf(context.isReturnDirect()));
	}

	// Response

	protected KeyValues outputValue(KeyValues keyValues, ArmsToolCallingObservationContext context) {
		if (StringUtils.hasText(context.getToolResult())) {
			return keyValues.and(HighCardinalityKeyNames.OUTPUT_VALUE.asString(), context.getToolResult());
		}
		return keyValues;
	}

}
