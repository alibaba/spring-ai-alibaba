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
package com.alibaba.cloud.ai.graph.plain_text;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMessageMediaSerializationTest {

	private final SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);

	@TempDir
	Path tempDir;

	@Test
	void shouldRetainUriMediaWhenCloningState() throws Exception {
		UserMessage original = message(uriMedia());
		assertMessageEquals(original, cloneMessage(original));
	}

	@Test
	void shouldRetainByteMediaWhenCloningState() throws Exception {
		UserMessage original = message(byteMedia());
		assertMessageEquals(original, cloneMessage(original));
	}

	@Test
	void shouldRetainMixedMediaOrderAndDefaultAttributes() throws Exception {
		Media defaultAttributes = new Media(MimeTypeUtils.IMAGE_PNG, URI.create("https://example.invalid/default.png"));
		UserMessage original = message(uriMedia(), byteMedia(), defaultAttributes);
		assertMessageEquals(original, cloneMessage(original));
	}

	@Test
	void shouldRetainUriMediaAfterReloadingCheckpoint() throws Exception {
		UserMessage original = message(uriMedia());
		assertMessageEquals(original, checkpointRoundTrip(original));
	}

	@Test
	void shouldRetainByteMediaAfterReloadingCheckpoint() throws Exception {
		UserMessage original = message(byteMedia());
		assertMessageEquals(original, checkpointRoundTrip(original));
	}

	@Test
	void shouldSupportHandlersWithoutDefaultTyping() throws Exception {
		SimpleModule module = new SimpleModule();
		SpringAIJacksonStateSerializer.registerMessageHandlers(module);
		ObjectMapper mapper = new ObjectMapper().registerModule(module);
		UserMessage original = message(uriMedia(), byteMedia());

		UserMessage restored = mapper.readValue(mapper.writeValueAsString(original), UserMessage.class);
		assertMessageEquals(original, restored);
	}

	@Test
	void shouldKeepOmittingUnsupportedMediaWhenCloningState() throws Exception {
		UserMessage original = message(unsupportedMedia());
		UserMessage expected = message();

		assertEquals(serializer.objectMapper().writeValueAsString(expected),
				serializer.objectMapper().writeValueAsString(original));
		assertMessageEquals(expected, cloneMessage(original));
	}

	@Test
	void shouldKeepOmittingUnsupportedMediaAfterReloadingCheckpoint() throws Exception {
		assertMessageEquals(message(), checkpointRoundTrip(message(unsupportedMedia())));
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 2 })
	void shouldRetainStandardMediaAlongsideUnsupportedData(int unsupportedIndex) throws Exception {
		Media uri = uriMedia();
		Media bytes = byteMedia();
		List<Media> media = new ArrayList<>(List.of(uri, bytes));
		media.add(unsupportedIndex, unsupportedMedia());
		UserMessage original = message(media.toArray(Media[]::new));
		UserMessage expected = message(uri, bytes);

		assertMessageEquals(expected, cloneMessage(original));
		assertMessageEquals(expected, checkpointRoundTrip(original));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", ",\"media\":null", ",\"media\":[]" })
	void shouldReadLegacyMessagesWithNoMedia(String mediaField) throws Exception {
		String json = """
				{"@class":"org.springframework.ai.chat.messages.UserMessage",
				 "text":"legacy message","metadata":{"origin":"old checkpoint"}%s}
				""".formatted(mediaField);
		UserMessage expected = UserMessage.builder()
			.text("legacy message")
			.metadata(Map.of("origin", "old checkpoint"))
			.build();

		assertMessageEquals(expected, serializer.objectMapper().readValue(json, UserMessage.class));
	}

	@Test
	void shouldKeepTextOnlyWireFormatWithoutMediaField() throws Exception {
		UserMessage original = message();
		String json = serializer.objectMapper().writeValueAsString(original);
		assertFalse(serializer.objectMapper().readTree(json).has("media"));
		assertMessageEquals(original, cloneMessage(original));
	}

	private UserMessage cloneMessage(UserMessage original) throws Exception {
		OverAllState cloned = serializer.cloneObject(Map.of("messages", List.of(original)));
		return (UserMessage) ((List<?>) cloned.value("messages").orElseThrow()).get(0);
	}

	private UserMessage checkpointRoundTrip(UserMessage original) throws Exception {
		FileSystemSaver writer = FileSystemSaver.builder()
			.targetFolder(tempDir)
			.stateSerializer(serializer)
			.maxCachedThreads(0)
			.build();
		RunnableConfig config = RunnableConfig.builder().threadId("media").build();
		Checkpoint checkpoint = Checkpoint.builder()
			.nodeId("before")
			.nextNodeId("after")
			.state(Map.of("messages", List.of(original)))
			.build();
		RunnableConfig saved = writer.put(config, checkpoint);
		assertTrue(Files.size(tempDir.resolve("thread-media.saver")) > 0);

		FileSystemSaver reader = FileSystemSaver.builder()
			.targetFolder(tempDir)
			.stateSerializer(new SpringAIJacksonStateSerializer(OverAllState::new))
			.maxCachedThreads(0)
			.build();
		Checkpoint restored = reader.get(saved).orElseThrow();
		assertEquals(checkpoint.getId(), restored.getId());
		assertEquals(checkpoint.getNodeId(), restored.getNodeId());
		assertEquals(checkpoint.getNextNodeId(), restored.getNextNodeId());
		return (UserMessage) ((List<?>) restored.getState().get("messages")).get(0);
	}

	private static UserMessage message(Media... media) {
		return UserMessage.builder()
			.text("Describe these offline fixtures")
			.metadata(Map.of("fixture", "media", "sequence", 1))
			.media(media)
			.build();
	}

	private static Media uriMedia() {
		return Media.builder()
			.mimeType(MimeTypeUtils.IMAGE_PNG)
			.data(URI.create("https://example.invalid/fixture.png"))
			.id("uri-image")
			.name("uri-fixture.png")
			.build();
	}

	private static Media byteMedia() {
		byte[] png = Base64.getDecoder()
			.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/h2kAAAAASUVORK5CYII=");
		return Media.builder()
			.mimeType(MimeTypeUtils.IMAGE_PNG)
			.data(new ByteArrayResource(png))
			.id("byte-image")
			.name("byte-fixture.png")
			.build();
	}

	private static Media unsupportedMedia() {
		Object data = new Object() {
			@Override
			public String toString() {
				throw new AssertionError("Unsupported media must not be coerced to text");
			}
		};
		return Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data(data).build();
	}

	private static void assertMessageEquals(UserMessage expected, UserMessage actual) {
		assertEquals(expected.getText(), actual.getText());
		assertEquals(expected.getMetadata(), actual.getMetadata());
		assertEquals(expected.getMedia().size(), actual.getMedia().size());
		for (int i = 0; i < expected.getMedia().size(); i++) {
			Media before = expected.getMedia().get(i);
			Media after = actual.getMedia().get(i);
			assertEquals(before.getMimeType(), after.getMimeType());
			assertEquals(before.getId(), after.getId());
			assertEquals(before.getName(), after.getName());
			if (before.getData() instanceof byte[] bytes) {
				assertArrayEquals(bytes, after.getDataAsByteArray());
			}
			else {
				assertEquals(before.getData(), after.getData());
			}
		}
	}

}
