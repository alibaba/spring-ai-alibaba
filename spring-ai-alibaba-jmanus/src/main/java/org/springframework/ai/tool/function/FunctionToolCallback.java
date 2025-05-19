/*
 * Copyright 2023-2025 the original author or authors.
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

package org.springframework.ai.tool.function;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

/**
 * TODO, a copy to fix issue https://github.com/spring-projects/spring-ai/issues/2497 This
 * would not be a concern if we later change how we define tool callbacks in this project.
 *
 * A {@link ToolCallback} implementation to invoke functions as tools.
 *
 * @author Thomas Vitale
 * @since 1.0.0
 */
public class FunctionToolCallback<I, O> implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(FunctionToolCallback.class);

	private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();

	private static final ToolMetadata DEFAULT_TOOL_METADATA = ToolMetadata.builder().build();

	private final ToolDefinition toolDefinition;

	private final ToolMetadata toolMetadata;

	private final Type toolInputType;

	private final BiFunction<I, ToolContext, O> toolFunction;

	private final ToolCallResultConverter toolCallResultConverter;

	public FunctionToolCallback(ToolDefinition toolDefinition, @Nullable ToolMetadata toolMetadata, Type toolInputType,
			BiFunction<I, ToolContext, O> toolFunction, @Nullable ToolCallResultConverter toolCallResultConverter) {
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

	@Override
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return this.toolMetadata;
	}

	@Override
	public String call(String toolInput) {
		return call(toolInput, null);
	}

	@Override
	public String call(String toolInput, @Nullable ToolContext toolContext) {
		Assert.hasText(toolInput, "toolInput cannot be null or empty");

		logger.debug("Starting execution of tool: {}", this.toolDefinition.name());

		// TODO, fix issue https://github.com/spring-projects/spring-ai/issues/2497
		I request;
		if (toolInputType == String.class) {
			request = (I) toolInput;
		}
		else {
			request = JsonParser.fromJson(toolInput, toolInputType);
		}
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
	 * Build a {@link FunctionToolCallback} from a {@link BiFunction}.
	 */
	public static <I, O> Builder<I, O> builder(String name, BiFunction<I, ToolContext, O> function) {
		return new Builder<>(name, function);
	}

	/**
	 * Build a {@link FunctionToolCallback} from a {@link Function}.
	 */
	public static <I, O> Builder<I, O> builder(String name, Function<I, O> function) {
		Assert.notNull(function, "function cannot be null");
		return new Builder<>(name, (request, context) -> function.apply(request));
	}

	/**
	 * Build a {@link FunctionToolCallback} from a {@link Supplier}.
	 */
	public static <O> Builder<Void, O> builder(String name, Supplier<O> supplier) {
		Assert.notNull(supplier, "supplier cannot be null");
		Function<Void, O> function = input -> supplier.get();
		return builder(name, function).inputType(Void.class);
	}

	/**
	 * Build a {@link FunctionToolCallback} from a {@link Consumer}.
	 */
	public static <I> Builder<I, Void> builder(String name, Consumer<I> consumer) {
		Assert.notNull(consumer, "consumer cannot be null");
		Function<I, Void> function = (I input) -> {
			consumer.accept(input);
			return null;
		};
		return builder(name, function);
	}

	public static final class Builder<I, O> {

		private String name;

		private String description;

		private String inputSchema;

		private Type inputType;

		private ToolMetadata toolMetadata;

		private BiFunction<I, ToolContext, O> toolFunction;

		private ToolCallResultConverter toolCallResultConverter;

		private Builder(String name, BiFunction<I, ToolContext, O> toolFunction) {
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

		public FunctionToolCallback<I, O> build() {
			Assert.notNull(this.inputType, "inputType cannot be null");
			var toolDefinition = DefaultToolDefinition.builder()
				.name(this.name)
				.description(StringUtils.hasText(this.description) ? this.description
						: ToolUtils.getToolDescriptionFromName(this.name))
				.inputSchema(StringUtils.hasText(this.inputSchema) ? this.inputSchema
						: JsonSchemaGenerator.generateForType(this.inputType))
				.build();
			return new FunctionToolCallback<>(toolDefinition, this.toolMetadata, this.inputType, this.toolFunction,
					this.toolCallResultConverter);
		}

	}

}
