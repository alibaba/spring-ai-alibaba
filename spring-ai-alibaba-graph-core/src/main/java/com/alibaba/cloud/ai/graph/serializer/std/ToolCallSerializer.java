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

class ToolCallSerializer implements NullableObjectSerializer<AssistantMessage.ToolCall> {

	@Override
	public void write(AssistantMessage.ToolCall object, ObjectOutput out) throws IOException {

		writeNullableUTF(object.id(), out);
		writeNullableUTF(object.type(), out);
		writeNullableUTF(object.name(), out);
		writeNullableUTF(object.arguments(), out);

	}

	@Override
	public AssistantMessage.ToolCall read(ObjectInput in) throws IOException, ClassNotFoundException {
		var id = readNullableUTF(in);
		var type = readNullableUTF(in);
		var name = readNullableUTF(in);
		var arguments = readNullableUTF(in);

		return new AssistantMessage.ToolCall(id.orElse(null), type.orElse(null), name.orElse(null),
				arguments.orElse(null));
	}

}
