/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.serializer.std;

import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

class ZhiPuAIAssistantMessageSerializer implements NullableObjectSerializer<Object> {

	private static final String MESSAGE_CLASS_NAME = "org.springframework.ai.zhipuai.ZhiPuAiAssistantMessage";

	private static final String BUILDER_CLASS_NAME = MESSAGE_CLASS_NAME + "$Builder";

	@Override
	@SuppressWarnings("unchecked")
	public void write(Object object, ObjectOutput out) throws IOException {
		String text = (String) invoke(object, "getText");
		writeNullableUTF(text, out);

		Map<String, Object> metadata = (Map<String, Object>) invoke(object, "getMetadata");
		out.writeObject(metadata);

		List<AssistantMessage.ToolCall> toolCalls = (List<AssistantMessage.ToolCall>) invoke(object, "getToolCalls");
		writeNullableObject(toolCalls, out);

		String reasoningContent = (String) invoke(object, "getReasoningContent");
		writeNullableUTF(reasoningContent, out);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object read(ObjectInput in) throws IOException, ClassNotFoundException {
		var text = readNullableUTF(in).orElse(null);
		var metadata = (Map<String, Object>) in.readObject();
		var toolCalls = (List<AssistantMessage.ToolCall>) readNullableObject(in).orElseGet(List::of);
		var reasoningContent = readNullableUTF(in).orElse(null);

		return buildMessage(text, metadata, toolCalls, reasoningContent);
	}

	private static Object buildMessage(String text, Map<String, Object> metadata,
			List<AssistantMessage.ToolCall> toolCalls, String reasoningContent) throws IOException, ClassNotFoundException {
		try {
			Class<?> builderClass = Class.forName(BUILDER_CLASS_NAME);
			Object builder = builderClass.getDeclaredConstructor().newInstance();
			invokeBuilder(builderClass, builder, "content", String.class, text);
			invokeBuilder(builderClass, builder, "reasoningContent", String.class, reasoningContent);
			invokeBuilder(builderClass, builder, "properties", Map.class, metadata);
			invokeBuilder(builderClass, builder, "toolCalls", List.class, toolCalls);
			return builderClass.getMethod("build").invoke(builder);
		}
		catch (ClassNotFoundException e) {
			throw e;
		}
		catch (ReflectiveOperationException e) {
			throw new IOException("Failed to construct ZhiPuAiAssistantMessage", e);
		}
	}

	private static Object invoke(Object target, String methodName) throws IOException {
		try {
			return target.getClass().getMethod(methodName).invoke(target);
		}
		catch (IllegalAccessException | NoSuchMethodException e) {
			throw new IOException("Failed to read ZhiPuAiAssistantMessage." + methodName + "()", e);
		}
		catch (InvocationTargetException e) {
			throw new IOException("Failed to read ZhiPuAiAssistantMessage." + methodName + "()", e.getCause());
		}
	}

	private static void invokeBuilder(Class<?> builderClass, Object builder, String methodName, Class<?> parameterType,
			Object value) throws ReflectiveOperationException {
		builderClass.getMethod(methodName, parameterType).invoke(builder, value);
	}

}
