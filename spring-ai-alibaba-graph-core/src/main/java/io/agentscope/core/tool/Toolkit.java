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
package io.agentscope.core.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

public class Toolkit {

	private final List<ToolCallback> tools = new CopyOnWriteArrayList<>();

	/**
	 * Register a tool object by scanning for methods annotated with @Tool.
	 * @param toolObject the object containing tool methods
	 */
	public void registerTool(Object toolObject) {
		Assert.notNull(toolObject, "Tool object cannot be null");

		Class<?> clazz = toolObject.getClass();
		Method[] methods = clazz.getDeclaredMethods();

		for (Method method : methods) {
			if (method.isAnnotationPresent(Tool.class)) {
				registerToolMethod(toolObject, method);
			}
		}
	}

	/**
	 * Register a single tool method.
	 */
	private void registerToolMethod(Object toolObject, Method method) {
		Tool toolAnnotation = method.getAnnotation(Tool.class);

		// Determine tool name
		String toolName = StringUtils.hasText(toolAnnotation.name()) ? toolAnnotation.name() : method.getName();

		// Determine tool description
		String description = StringUtils.hasText(toolAnnotation.description()) ? toolAnnotation.description()
				: "Tool: " + toolName;

		// Create a BiFunction wrapper for the method
		BiFunction<Object, org.springframework.ai.chat.model.ToolContext, Object> toolFunction = createToolFunction(
				toolObject, method);

		// Determine input type from method parameters
		Class<?> inputType = determineInputType(method);

		// Create FunctionToolCallback
		FunctionToolCallback.Builder<Object, Object> builder = FunctionToolCallback.builder(toolName, toolFunction)
			.description(description)
			.inputType(inputType);

		// Set return direct if specified
		if (toolAnnotation.returnDirect()) {
			builder.toolMetadata(ToolMetadata.builder().returnDirect(true).build());
		}

		ToolCallback toolCallback = builder.build();
		tools.add(toolCallback);
	}

	/**
	 * Create a BiFunction wrapper for the tool method.
	 */
	private BiFunction<Object, org.springframework.ai.chat.model.ToolContext, Object> createToolFunction(
			Object toolObject, Method method) {
		return (input, context) -> {
			try {
				method.setAccessible(true);
				Parameter[] parameters = method.getParameters();

				if (parameters.length == 0) {
					return method.invoke(toolObject);
				}
				else if (parameters.length == 1) {
					return method.invoke(toolObject, input);
				}
				else if (parameters.length == 2) {
					return method.invoke(toolObject, input, context);
				}
				else {
					throw new IllegalArgumentException("Tool method can have at most 2 parameters: input and context");
				}
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to invoke tool method: " + method.getName(), e);
			}
		};
	}

	/**
	 * Determine the input type for the tool method.
	 */
	private Class<?> determineInputType(Method method) {
		Parameter[] parameters = method.getParameters();

		if (parameters.length == 0) {
			return Void.class;
		}
		else {
			return parameters[0].getType();
		}
	}

	public ToolCallbackProvider toolCallbackProvider() {
		return new ToolCallbackProvider() {
			@Override
			public ToolCallback[] getToolCallbacks() {
				return tools.toArray(new ToolCallback[0]);
			}
		};
	}

}
