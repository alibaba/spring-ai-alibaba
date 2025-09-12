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
import java.util.List;
import java.util.Map;

class AssistantMessageSerializer implements NullableObjectSerializer<AssistantMessage> {

	@Override
	public void write(AssistantMessage object, ObjectOutput out) throws IOException {
		writeNullableUTF(object.getText(), out);
		out.writeObject(object.getMetadata());
		writeNullableObject(object.getToolCalls(), out);
		// out.writeObject(object.getMedia());
	}

	@Override
	@SuppressWarnings("unchecked")
	public AssistantMessage read(ObjectInput in) throws IOException, ClassNotFoundException {
		var text = readNullableUTF(in).orElse(null);
		var metadata = (Map<String, Object>) in.readObject();
		var toolCalls = (List<AssistantMessage.ToolCall>) readNullableObject(in).orElseGet(List::of);
		return new AssistantMessage(text, metadata, toolCalls);
	}

}
