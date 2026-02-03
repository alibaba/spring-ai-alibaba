/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.sandbox;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.function.BiFunction;

public class RuntimeFunctionToolCallback<I, O> implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(RuntimeFunctionToolCallback.class);

	private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();

	private static final ToolMetadata DEFAULT_TOOL_METADATA = ToolMetadata.builder().build();

	private final ToolDefinition toolDefinition;

	private final ToolMetadata toolMetadata;

	private final Type toolInputType;

	private final SandboxAwareTool<I, O> toolFunction;

	private final ToolCallResultConverter toolCallResultConverter;

	public RuntimeFunctionToolCallback(ToolDefinition toolDefinition, @Nullable ToolMetadata toolMetadata, Type toolInputType,
			SandboxAwareTool<I, O> toolFunction, @Nullable ToolCallResultConverter toolCallResultConverter) {
		Assert.notNull(toolDefinition, "toolDefinition cannot be null");
		Assert.notNull(toolInputType, "toolInputType cannot be null");
		Assert.notNull(toolFunction, "toolFunction cannot be null");
		this.toolDefinition = toolDefinition;
		this.toolMetadata = toolMetadata != null ? toolMetadata : DEFAULT_TOOL_METADATA;
		this.toolFunction = toolFunction;
		this.toolInputType = toolInputType;
		this.toolCallResultConverter = toolCallResultConverter != null ? toolCallResultConverter
				: DEFAULT_RESULT_CONVERTER;
	}

	@NotNull
    @Override
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	@NotNull
    @Override
	public ToolMetadata getToolMetadata() {
		return this.toolMetadata;
	}

	public SandboxAwareTool<I, O> getToolFunction() {
		return this.toolFunction;
	}

	@NotNull
    @Override
	public String call(@NotNull String toolInput) {
		return call(toolInput, null);
	}

	@NotNull
    @Override
	public String call(@NotNull String toolInput, @Nullable ToolContext toolContext) {
		Assert.hasText(toolInput, "toolInput cannot be null or empty");

		logger.debug("Starting execution of tool: {}", this.toolDefinition.name());

		I request = JsonParser.fromJson(toolInput, this.toolInputType);
		O response = this.toolFunction.apply(request, toolContext);

		logger.debug("Successful execution of tool: {}", this.toolDefinition.name());

		return this.toolCallResultConverter.convert(response, null);
	}

	@Override
	public String toString() {
		return "FunctionToolCallback{" + "toolDefinition=" + this.toolDefinition + ", toolMetadata=" + this.toolMetadata
				+ '}';
	}

	/**
	 * Build a {@link RuntimeFunctionToolCallback} from a {@link BiFunction}.
	 */
	public static <I, O> Builder<I, O> builder(String name, SandboxAwareTool<I, O> function) {
		return new Builder<>(name, function);
	}


	public static final class Builder<I, O> {

		private String name;

		private String description;

		private String inputSchema;

		private Type inputType;

		private ToolMetadata toolMetadata;

		private SandboxAwareTool<I, O> toolFunction;

		private ToolCallResultConverter toolCallResultConverter;

		private Builder(String name, SandboxAwareTool<I, O> toolFunction) {
			Assert.hasText(name, "name cannot be null or empty");
			Assert.notNull(toolFunction, "toolFunction cannot be null");
			this.name = name;
			this.toolFunction = toolFunction;
		}

		public Builder<I, O> description(String description) {
			this.description = description;
			return this;
		}

		public Builder<I, O> inputSchema(String inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		public Builder<I, O> inputType(Type inputType) {
			this.inputType = inputType;
			return this;
		}

		public Builder<I, O> inputType(ParameterizedTypeReference<?> inputType) {
			Assert.notNull(inputType, "inputType cannot be null");
			this.inputType = inputType.getType();
			return this;
		}

		public Builder<I, O> toolMetadata(ToolMetadata toolMetadata) {
			this.toolMetadata = toolMetadata;
			return this;
		}

		public Builder<I, O> toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
			this.toolCallResultConverter = toolCallResultConverter;
			return this;
		}

		public RuntimeFunctionToolCallback<I, O> build() {
			Assert.notNull(this.inputType, "inputType cannot be null");
			var toolDefinition = DefaultToolDefinition.builder()
					.name(this.name)
					.description(StringUtils.hasText(this.description) ? this.description
							: ToolUtils.getToolDescriptionFromName(this.name))
					.inputSchema(StringUtils.hasText(this.inputSchema) ? this.inputSchema
							: JsonSchemaGenerator.generateForType(this.inputType))
					.build();
			return new RuntimeFunctionToolCallback<>(toolDefinition, this.toolMetadata, this.inputType, this.toolFunction,
					this.toolCallResultConverter);
		}

	}

}
