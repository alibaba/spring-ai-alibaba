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
package com.alibaba.cloud.ai.function;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.resolution.TypeResolverHelper;
import org.springframework.ai.util.json.schema.SchemaType;
import org.springframework.util.Assert;

@EqualsAndHashCode
public class AgentFunctionCallbackWrapper<I, O> implements BiFunction<I, ToolContext, O>, ToolCallback {

	@Getter
	private final String name;

	@Getter
	private final String description;

	@Getter
	private final String inputTypeSchema;

	private final Class<I> inputType;

	private final ObjectMapper objectMapper;

	private final Function<O, String> responseConverter;

	private final BiFunction<I, ToolContext, O> biFunction;

	protected AgentFunctionCallbackWrapper(String name, String description, String inputTypeSchema, Class<I> inputType,
			Function<O, String> responseConverter, ObjectMapper objectMapper, BiFunction<I, ToolContext, O> function) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(description, "Description must not be null");
		Assert.notNull(inputType, "InputType must not be null");
		Assert.notNull(inputTypeSchema, "InputTypeSchema must not be null");
		Assert.notNull(responseConverter, "ResponseConverter must not be null");
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		Assert.notNull(function, "Function must not be null");
		this.name = name;
		this.description = description;
		this.inputType = inputType;
		this.inputTypeSchema = inputTypeSchema;
		this.responseConverter = responseConverter;
		this.objectMapper = objectMapper;
		this.biFunction = function;
	}

	public O convertResponse(ToolResponseMessage.ToolResponse toolResponse) {
		try {
			Class<O> targetClass = resolveOutputType(biFunction);
			return objectMapper.readValue(toolResponse.responseData(), targetClass);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String call(String functionInput, ToolContext toolContext) {
		I request = fromJson(functionInput, inputType);
		O response = apply(request, toolContext);
		return this.responseConverter.apply(response);
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return ToolDefinition.builder().name(name).description(description).inputSchema(inputTypeSchema).build();
	}

	public String call(String functionArguments) {
		I request = fromJson(functionArguments, inputType);
		return andThen(responseConverter).apply(request, null);
	}

	private <T> T fromJson(String json, Class<T> targetClass) {
		try {
			return objectMapper.readValue(json, targetClass);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public O apply(I input, ToolContext context) {
		return biFunction.apply(input, context);
	}

	private static <I, O> Class<O> resolveOutputType(BiFunction<I, ToolContext, O> biFunction) {
		return (Class<O>) TypeResolverHelper
			.getBiFunctionArgumentClass((Class<? extends BiFunction<?, ?, ?>>) biFunction.getClass(), 2);
	}

	public static <I, O> Builder<I, O> builder(BiFunction<I, ToolContext, O> biFunction) {
		return new Builder<>(biFunction);
	}

	public static <I, O> Builder<I, O> builder(Function<I, O> function) {
		return new Builder<>(function);
	}

	public static class Builder<I, O> {

		private String name;

		private String description;

		private Class<I> inputType;

		private final BiFunction<I, ToolContext, O> biFunction;

		private final Function<I, O> function;

		private SchemaType schemaType;

		private Function<O, String> responseConverter;

		private String inputTypeSchema;

		private ObjectMapper objectMapper;

		public Builder(BiFunction<I, ToolContext, O> biFunction) {
			this.schemaType = SchemaType.JSON_SCHEMA;
			this.responseConverter = ModelOptionsUtils::toJsonString;
			this.objectMapper = (new ObjectMapper()).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
				.registerModule(new JavaTimeModule());
			Assert.notNull(biFunction, "Function must not be null");
			this.biFunction = biFunction;
			this.function = null;
		}

		public Builder(Function<I, O> function) {
			this.schemaType = SchemaType.JSON_SCHEMA;
			this.responseConverter = ModelOptionsUtils::toJsonString;
			this.objectMapper = (new ObjectMapper()).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
				.registerModule(new JavaTimeModule());
			Assert.notNull(function, "Function must not be null");
			this.biFunction = null;
			this.function = function;
		}

		public Builder<I, O> withName(String name) {
			Assert.hasText(name, "Name must not be empty");
			this.name = name;
			return this;
		}

		public Builder<I, O> withDescription(String description) {
			Assert.hasText(description, "Description must not be empty");
			this.description = description;
			return this;
		}

		public Builder<I, O> withInputType(Class<I> inputType) {
			this.inputType = inputType;
			return this;
		}

		public Builder<I, O> withResponseConverter(Function<O, String> responseConverter) {
			Assert.notNull(responseConverter, "ResponseConverter must not be null");
			this.responseConverter = responseConverter;
			return this;
		}

		public Builder<I, O> withInputTypeSchema(String inputTypeSchema) {
			Assert.hasText(inputTypeSchema, "InputTypeSchema must not be empty");
			this.inputTypeSchema = inputTypeSchema;
			return this;
		}

		public Builder<I, O> withObjectMapper(ObjectMapper objectMapper) {
			Assert.notNull(objectMapper, "ObjectMapper must not be null");
			this.objectMapper = objectMapper;
			return this;
		}

		public Builder<I, O> withSchemaType(SchemaType schemaType) {
			Assert.notNull(schemaType, "SchemaType must not be null");
			this.schemaType = schemaType;
			return this;
		}

		public AgentFunctionCallbackWrapper<I, O> build() {
			Assert.hasText(name, "Name must not be empty");
			Assert.hasText(description, "Description must not be empty");
			Assert.notNull(responseConverter, "ResponseConverter must not be null");
			Assert.notNull(objectMapper, "ObjectMapper must not be null");
			if (inputType == null) {
				if (function != null) {
					inputType = resolveInputType(function);
				}
				else {
					inputType = resolveInputType(biFunction);
				}
			}

			if (inputTypeSchema == null) {
				boolean upperCaseTypeValues = schemaType == SchemaType.OPEN_API_SCHEMA;
				inputTypeSchema = ModelOptionsUtils.getJsonSchema(inputType, upperCaseTypeValues);
			}

			BiFunction<I, ToolContext, O> finalBiFunction = biFunction != null ? biFunction
					: (request, context) -> function.apply(request);

			return new AgentFunctionCallbackWrapper<>(name, description, inputTypeSchema, inputType, responseConverter,
					objectMapper, finalBiFunction);
		}

		private static <I, O> Class<I> resolveInputType(BiFunction<I, ToolContext, O> biFunction) {
			return (Class<I>) TypeResolverHelper
				.getBiFunctionInputClass((Class<? extends BiFunction<?, ?, ?>>) biFunction.getClass());
		}

		private static <I, O> Class<I> resolveInputType(Function<I, O> function) {
			return (Class<I>) TypeResolverHelper
				.getFunctionInputClass((Class<? extends Function<?, ?>>) function.getClass());
		}

	}

}
