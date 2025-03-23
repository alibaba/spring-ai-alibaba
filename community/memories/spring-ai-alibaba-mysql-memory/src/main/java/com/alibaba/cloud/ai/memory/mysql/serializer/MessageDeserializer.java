package com.alibaba.cloud.ai.memory.mysql.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author yingzi
 * @date 2025/3/23:15:12
 */
public class MessageDeserializer extends JsonDeserializer<Message> {

	private static final Logger logger = LoggerFactory.getLogger(MessageDeserializer.class);

	public Message deserialize(JsonParser p, DeserializationContext ctxt) {
		ObjectMapper mapper = (ObjectMapper) p.getCodec();
		JsonNode node = null;
		Message message = null;
		try {
			node = mapper.readTree(p);
			String messageType = node.get("messageType").asText();
			switch (messageType) {
				case "USER" -> message = new UserMessage(node.get("text").asText(),
						mapper.convertValue(node.get("media"), new TypeReference<Collection<Media>>() {
						}), mapper.convertValue(node.get("metadata"), new TypeReference<Map<String, Object>>() {
						}));
				case "ASSISTANT" -> message = new AssistantMessage(node.get("text").asText());
				default -> throw new IllegalArgumentException("Unknown message type: " + messageType);
			}
			;
		}
		catch (IOException e) {
			logger.error("Error deserializing message", e);
		}
		return message;
	}

}
