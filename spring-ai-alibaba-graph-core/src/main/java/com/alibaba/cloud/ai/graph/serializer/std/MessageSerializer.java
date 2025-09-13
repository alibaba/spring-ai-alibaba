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

import com.alibaba.cloud.ai.graph.serializer.Serializer;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class MessageSerializer implements Serializer<Message> {

	final UserMessageSerializer user = new UserMessageSerializer();

	final AssistantMessageSerializer assistant = new AssistantMessageSerializer();

	final SystemMessageSerializer system = new SystemMessageSerializer();

	final ToolResponseMessageSerializer tool = new ToolResponseMessageSerializer();

	@Override
	public void write(Message object, ObjectOutput out) throws IOException {
		out.writeObject(object.getMessageType());

		switch (object.getMessageType()) {
			case USER -> user.write((UserMessage) object, out);
			case ASSISTANT -> assistant.write((AssistantMessage) object, out);
			case SYSTEM -> system.write((SystemMessage) object, out);
			case TOOL -> tool.write((ToolResponseMessage) object, out);
			default -> throw new IllegalArgumentException("Unsupported message type: " + object.getMessageType());
		}
	}

	@Override
	public Message read(ObjectInput in) throws IOException, ClassNotFoundException {

		MessageType type = (MessageType) in.readObject();

		return switch (type) {
			case ASSISTANT -> assistant.read(in);
			case USER -> user.read(in);
			case SYSTEM -> system.read(in);
			case TOOL -> tool.read(in);
		};
	}

}
