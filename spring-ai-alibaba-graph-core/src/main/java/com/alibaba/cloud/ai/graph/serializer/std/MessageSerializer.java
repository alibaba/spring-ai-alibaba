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

import com.alibaba.cloud.ai.graph.serializer.MessagePartsCodec;
import com.alibaba.cloud.ai.graph.serializer.Serializer;

import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

public class MessageSerializer implements Serializer<Message> {

	private static final String PARTS_FORMAT = "message-parts-v1";

	final UserMessageSerializer user = new UserMessageSerializer();

	final AssistantMessageSerializer assistant = new AssistantMessageSerializer();

	final SystemMessageSerializer system = new SystemMessageSerializer();

	final ToolResponseMessageSerializer tool = new ToolResponseMessageSerializer();

	@Override
	public void write(Message object, ObjectOutput out) throws IOException {
		MessageType type = object.getMessageType();
		if (object instanceof AbstractMessage message && type != MessageType.SYSTEM && !canWriteLegacy(message)) {
			out.writeObject(PARTS_FORMAT);
			out.writeObject(type);
			out.writeObject(object.getMetadata());
			out.writeObject(MessagePartsCodec.write(message.getParts()));
			return;
		}
		out.writeObject(type);

		switch (type) {
			case ASSISTANT -> assistant.write((AssistantMessage) object, out);
			case USER -> user.write((UserMessage) object, out);
			case SYSTEM -> system.write((SystemMessage) object, out);
			case TOOL -> tool.write((ToolResponseMessage) object, out);
		}
	}

	private static boolean canWriteLegacy(AbstractMessage message) {
		if (message instanceof UserMessage userMessage) {
			String text = userMessage.getText();
			return text != null && new UserMessage(text).getParts().equals(message.getParts());
		}
		if (message instanceof AssistantMessage assistantMessage) {
			var calls = assistantMessage.getToolCalls();
			return fitsLegacyUtf(assistantMessage.getText())
					&& calls.stream().allMatch(call -> fitsLegacyUtf(call.id()) && fitsLegacyUtf(call.type())
							&& fitsLegacyUtf(call.name()) && fitsLegacyUtf(call.arguments()))
					&& AssistantMessage.builder().content(assistantMessage.getText()).toolCalls(calls)
							.build().getParts().equals(message.getParts());
		}
		if (message instanceof ToolResponseMessage toolMessage) {
			var responses = toolMessage.getResponses();
			return responses.stream().allMatch(response -> fitsLegacyUtf(response.id())
					&& fitsLegacyUtf(response.name()) && fitsLegacyUtf(response.responseData()))
					&& ToolResponseMessage.builder().responses(responses).build().getParts().equals(message.getParts());
		}
		return false;
	}

	private static boolean fitsLegacyUtf(String text) {
		// ObjectOutput.writeUTF uses a two-byte length and up to three bytes per char.
		return text == null || text.length() <= 65535 / 3;
	}

	@Override
	public Message read(ObjectInput in) throws IOException, ClassNotFoundException {

		Object format = in.readObject();
		if (PARTS_FORMAT.equals(format)) {
			return readParts(in);
		}

		return switch ((MessageType) format) {
			case ASSISTANT -> assistant.read(in);
			case USER -> user.read(in);
			case SYSTEM -> system.read(in);
			case TOOL -> tool.read(in);
		};
	}

	@SuppressWarnings("unchecked")
	private Message readParts(ObjectInput in) throws IOException, ClassNotFoundException {
		MessageType type = (MessageType) in.readObject();
		var metadata = (Map<String, Object>) in.readObject();
		var parts = MessagePartsCodec.read((String) in.readObject());
		return switch (type) {
			case ASSISTANT -> AssistantMessage.builder().parts(parts).properties(metadata).build();
			case USER -> UserMessage.builder().parts(parts).metadata(metadata).build();
			case TOOL -> ToolResponseMessage.builder().parts(parts).metadata(metadata).build();
			default -> throw new IOException("Unexpected message type for message parts checkpoint: " + type);
		};
	}

}
