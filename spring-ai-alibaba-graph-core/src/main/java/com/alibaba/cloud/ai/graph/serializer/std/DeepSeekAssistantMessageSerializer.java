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
package com.alibaba.cloud.ai.graph.serializer.std;

import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

class DeepSeekAssistantMessageSerializer implements NullableObjectSerializer<Object> {

	private final Class<?> deepSeekClass;
	private final Constructor<?> constructor;

	public DeepSeekAssistantMessageSerializer() {
		try {
			this.deepSeekClass = Class.forName("org.springframework.ai.deepseek.DeepSeekAssistantMessage");
			// Find constructor: DeepSeekAssistantMessage(String text, String reasoningContent, Map<String, Object> metadata, List<ToolCall> toolCalls)
			this.constructor = deepSeekClass.getConstructor(String.class, String.class, 
				Map.class, List.class);
		}
		catch (ClassNotFoundException | NoSuchMethodException e) {
			throw new IllegalStateException("DeepSeekAssistantMessage class or constructor not found", e);
		}
	}

	@Override
	public void write(Object object, ObjectOutput out) throws IOException {
		if (!deepSeekClass.isInstance(object)) {
			throw new IllegalArgumentException("Expected DeepSeekAssistantMessage instance");
		}

		try {
			// Use reflection to call getText()
			Method getTextMethod = deepSeekClass.getMethod("getText");
			String text = (String) getTextMethod.invoke(object);
			writeNullableUTF(text, out);

			// Use reflection to call getMetadata()
			Method getMetadataMethod = deepSeekClass.getMethod("getMetadata");
			@SuppressWarnings("unchecked")
			Map<String, Object> metadata = (Map<String, Object>) getMetadataMethod.invoke(object);
			out.writeObject(metadata);

			// Use reflection to call getToolCalls()
			Method getToolCallsMethod = deepSeekClass.getMethod("getToolCalls");
			@SuppressWarnings("unchecked")
			List<AssistantMessage.ToolCall> toolCalls = (List<AssistantMessage.ToolCall>) getToolCallsMethod.invoke(object);
			writeNullableObject(toolCalls, out);

			// Use reflection to call getReasoningContent()
			Method getReasoningContentMethod = deepSeekClass.getMethod("getReasoningContent");
			String reasoningContent = (String) getReasoningContentMethod.invoke(object);
			writeNullableUTF(reasoningContent, out);
			// out.writeObject(object.getMedia());
		}
		catch (Exception e) {
			if (e instanceof IOException) {
				throw (IOException) e;
			}
			throw new IOException("Failed to serialize DeepSeekAssistantMessage", e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object read(ObjectInput in) throws IOException, ClassNotFoundException {
		var text = readNullableUTF(in).orElse(null);
		var metadata = (Map<String, Object>) in.readObject();
		var toolCalls = (List<AssistantMessage.ToolCall>) readNullableObject(in).orElseGet(List::of);
		var reasoningContent = readNullableUTF(in).orElse(null);

		try {
			return constructor.newInstance(text, reasoningContent, metadata, toolCalls);
		}
		catch (Exception e) {
			throw new IOException("Failed to deserialize DeepSeekAssistantMessage", e);
		}
	}

}

