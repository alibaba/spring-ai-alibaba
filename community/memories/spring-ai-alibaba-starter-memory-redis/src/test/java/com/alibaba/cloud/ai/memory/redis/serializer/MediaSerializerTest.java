package com.alibaba.cloud.ai.memory.redis.serializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * MediaSerializerTest
 *
 * @author benym
 * @since 2025/9/2 22:21
 */
@RunWith(MockitoJUnitRunner.class)
public class MediaSerializerTest {

	@Test
	public void should_serializeSuccess_when_serializeMessage_given_stringData() {
		var userMessage = UserMessage.builder()
			.text("Explain what do you see on this picture?")
			.media(List.of(Media.builder()
				.mimeType(MimeTypeUtils.IMAGE_PNG)
				.data(URI.create("https://docs.spring.io/spring-ai/reference/_images/multimodal.test.png"))
				.build()))
			.build();
		ObjectMapper objectMapper = JsonMapper.builder()
			.configure(MapperFeature.AUTO_DETECT_GETTERS, false)
			.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
			.visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
			.build();
		assertDoesNotThrow(() -> {
			objectMapper.writeValueAsString(userMessage);
		});
	}

	@Test
	public void should_serializeSuccess_when_serializeMessage_given_byteData() {
		var userMessage = UserMessage.builder()
			.text("Explain what do you see on this picture?")
			.media(List.of(Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data(new byte[] { 1, 2, 3 }).build()))
			.build();
		ObjectMapper objectMapper = JsonMapper.builder()
			.configure(MapperFeature.AUTO_DETECT_GETTERS, false)
			.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
			.visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
			.build();
		assertDoesNotThrow(() -> {
			objectMapper.writeValueAsString(userMessage);
		});
	}

}
