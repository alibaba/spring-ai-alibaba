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
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMessageMediaReadConstraintsTest {

	@TempDir
	Path tempDir;

	@ParameterizedTest
	@EnumSource(MediaStringField.class)
	void shouldRetainEveryMediaStringAtConfiguredLimit(MediaStringField field) throws Exception {
		SpringAIJacksonStateSerializer serializer = serializer(128);
		UserMessage original = orderedMessage(media(field, 128));

		assertRoundTrips(original, original, serializer);
		var wire = serializer.objectMapper().readTree(serializer.objectMapper().writeValueAsString(original));
		assertEquals("string", wire.path("media").get(0).path("dataType").asText());
		assertTrue(wire.path("media").get(0).path("data").isTextual());
	}

	@ParameterizedTest
	@EnumSource(MediaStringField.class)
	void shouldRejectUnreadableMediaStringBeforeWritingState(MediaStringField field) throws Exception {
		assertWriteFailure(serializer(128), orderedMessage(media(field, 129)));
	}

	@ParameterizedTest
	@EnumSource(MediaStringField.class)
	void shouldPreserveCheckpointWhenAnyMediaStringIsRejected(MediaStringField field) throws Exception {
		assertRejectedSavePreservesCheckpoint(serializer(128), orderedMessage(media(field, 129)));
	}

	@ParameterizedTest
	@EnumSource(MediaStringField.class)
	void shouldHonorRelaxedLimitForEveryMediaString(MediaStringField field) throws Exception {
		SpringAIJacksonStateSerializer relaxed = serializer(256);
		UserMessage original = orderedMessage(media(field, 129));

		assertRoundTrips(original, original, relaxed);
		assertEquals(256, relaxed.objectMapper().getFactory().streamReadConstraints().getMaxStringLength());
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldKeepBinaryAndResourceLengthChecks(boolean resource) throws Exception {
		SpringAIJacksonStateSerializer serializer = serializer(128);
		UserMessage original = orderedMessage(binary(96, resource));
		assertRoundTrips(original, original, serializer);
		var wire = serializer.objectMapper().readTree(serializer.objectMapper().writeValueAsString(original));
		assertEquals("bytes", wire.path("media").get(0).path("dataType").asText());
		assertEquals(128, wire.path("media").get(0).path("data").asText().length());
		UserMessage oversized = orderedMessage(binary(97, resource));
		assertWriteFailure(serializer, oversized);
		assertRejectedSavePreservesCheckpoint(serializer, oversized);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldCheckDataTypeStringsAtTinyLimits(boolean binary) throws Exception {
		String dataType = binary ? "bytes" : "string";
		Media media = Media.builder()
			.mimeType(new MimeType("a", "b"))
			.id(null)
			.name("")
			.data(binary ? new byte[0] : "")
			.build();
		UserMessage original = UserMessage.builder().text("").metadata(Map.of()).media(media).build();
		ObjectMapper bounded = plainMapper(dataType.length() - 1);
		IOException failure = assertThrows(IOException.class, () -> bounded.writeValueAsString(original));
		assertConstraintCause(failure);

		ObjectMapper allowed = plainMapper(dataType.length());
		String json = allowed.writeValueAsString(original);
		assertEquals(dataType, allowed.readTree(json).path("media").get(0).path("dataType").asText());
		assertTrue(allowed.readTree(json).path("media").get(0).path("id").isNull());
		assertMessageEquals(original, allowed.readValue(json, UserMessage.class));
	}

	@Test
	void shouldRetainNullIdsAndGeneratedNames() throws Exception {
		Media media = Media.builder().mimeType(new MimeType("a", "b")).id(null).name(null).data("d").build();
		assertNotNull(media.getName());
		SpringAIJacksonStateSerializer serializer = serializer(128);
		UserMessage original = orderedMessage(media);

		assertRoundTrips(original, original, serializer);
		var wire = serializer.objectMapper().readTree(serializer.objectMapper().writeValueAsString(original));
		assertTrue(wire.path("media").get(0).path("id").isNull());
		assertEquals(media.getName(), wire.path("media").get(0).path("name").asText());
	}

	@Test
	void shouldAlsoCheckGeneratedMediaNames() throws Exception {
		Media media = Media.builder().mimeType(new MimeType("a", "b")).data("d").build();
		assertNotNull(media.getName());
		assertWriteFailure(serializer(media.getName().length() - 1), message(media));
	}

	@ParameterizedTest
	@EnumSource(UnknownData.class)
	void shouldOmitUnknownDataBeforeValidatingAttributes(UnknownData kind) throws Exception {
		Object data = switch (kind) {
			case OBJECT -> new Object() {
				@Override
				public String toString() {
					throw new AssertionError("Unsupported media must not be converted to text");
				}
			};
			case URI_AS_OBJECT -> URI.create("https://example.invalid/offline");
			case RESOURCE_AS_OBJECT -> new ByteArrayResource(new byte[1]);
		};
		Media unsupported = Media.builder()
			.mimeType(new MimeType("a", "b".repeat(127)))
			.id("i".repeat(129))
			.name("n".repeat(129))
			.data(data)
			.build();
		Media string = media(MediaStringField.STRING_DATA, 1);
		Media bytes = binary(3, false);
		SpringAIJacksonStateSerializer serializer = serializer(128);
		UserMessage original = message(unsupported, string, bytes, unsupported);
		UserMessage expected = message(string, bytes);
		assertEquals(serializer.objectMapper().writeValueAsString(expected),
				serializer.objectMapper().writeValueAsString(original));
		assertRoundTrips(original, expected, serializer);

		UserMessage unsupportedOnly = message(unsupported);
		assertFalse(serializer.objectMapper().writeValueAsString(unsupportedOnly).contains("\"media\""));
		assertRoundTrips(unsupportedOnly, message(), serializer);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldMeasureDecodedUtf16Length(boolean supplementary) throws Exception {
		String value = supplementary ? "\uD83D\uDE00".repeat(64) : "\n".repeat(128);
		Media.Builder allowed = Media.builder().mimeType(new MimeType("a", "b")).data("d").id("i").name("n");
		Media.Builder oversized = Media.builder().mimeType(new MimeType("a", "b")).data("d").id("i").name("n");
		if (supplementary) {
			allowed.id(value);
			oversized.id(value + "x");
		}
		else {
			allowed.name(value);
			oversized.name(value + "x");
		}
		SpringAIJacksonStateSerializer serializer = serializer(128);
		UserMessage original = orderedMessage(allowed.build());
		assertRoundTrips(original, original, serializer);
		assertWriteFailure(serializer, orderedMessage(oversized.build()));
	}

	private void assertRoundTrips(UserMessage original, UserMessage expected, SpringAIJacksonStateSerializer serializer)
			throws Exception {
		OverAllState cloned = serializer.cloneObject(Map.of("messages", List.of(original)));
		assertMessageEquals(expected, (UserMessage) ((List<?>) cloned.value("messages").orElseThrow()).get(0));
		Path folder = tempDir.resolve("roundtrip");
		FileSystemSaver writer = saver(folder, serializer, 0);
		RunnableConfig config = RunnableConfig.builder().threadId("media").build();
		Checkpoint checkpoint = checkpoint(original);
		RunnableConfig saved = writer.put(config, checkpoint);
		Checkpoint restored = freshSaver(folder, serializer).get(saved).orElseThrow();
		assertEquals(checkpoint.getId(), restored.getId());
		assertEquals(checkpoint.getNodeId(), restored.getNodeId());
		assertEquals(checkpoint.getNextNodeId(), restored.getNextNodeId());
		assertMessageEquals(expected, checkpointMessage(restored));
	}

	private void assertRejectedSavePreservesCheckpoint(SpringAIJacksonStateSerializer serializer, UserMessage rejected)
			throws Exception {
		Path folder = tempDir.resolve("rejected");
		FileSystemSaver writer = saver(folder, serializer, 1);
		RunnableConfig config = RunnableConfig.builder().threadId("media").build();
		UserMessage original = orderedMessage(media(MediaStringField.STRING_DATA, 1));
		Checkpoint seed = checkpoint(original);
		RunnableConfig saved = writer.put(config, seed);
		Path target = folder.resolve("thread-media.saver");
		byte[] before = Files.readAllBytes(target);
		for (RunnableConfig writeConfig : List.of(config, saved)) {
			IOException failure = assertThrows(IOException.class, () -> writer.put(writeConfig, checkpoint(rejected)));
			assertConstraintCause(failure);
			assertArrayEquals(before, Files.readAllBytes(target));
			Checkpoint cached = writer.get(config).orElseThrow();
			assertEquals(seed.getId(), cached.getId());
			assertMessageEquals(original, checkpointMessage(cached));
			FileSystemSaver reader = freshSaver(folder, serializer);
			Checkpoint restored = reader.get(saved).orElseThrow();
			assertEquals(seed.getId(), restored.getId());
			assertMessageEquals(original, checkpointMessage(restored));
			assertEquals(List.of(seed.getId()), reader.list(config).stream().map(Checkpoint::getId).toList());
			try (var files = Files.list(folder)) {
				assertEquals(List.of(target), files.toList());
			}
		}
	}

	private static FileSystemSaver saver(Path folder, SpringAIJacksonStateSerializer serializer, int cache) {
		return FileSystemSaver.builder()
			.targetFolder(folder)
			.stateSerializer(serializer)
			.maxCachedThreads(cache)
			.build();
	}

	private static FileSystemSaver freshSaver(Path folder, SpringAIJacksonStateSerializer serializer) {
		return saver(folder, new SpringAIJacksonStateSerializer(OverAllState::new, serializer.objectMapper().copy()),
				0);
	}

	private static Checkpoint checkpoint(UserMessage message) {
		return Checkpoint.builder()
			.nodeId("before")
			.nextNodeId("after")
			.state(Map.of("messages", List.of(message)))
			.build();
	}

	private static UserMessage checkpointMessage(Checkpoint checkpoint) {
		return (UserMessage) ((List<?>) checkpoint.getState().get("messages")).get(0);
	}

	private static void assertWriteFailure(SpringAIJacksonStateSerializer serializer, UserMessage message) {
		IOException failure = assertThrows(IOException.class,
				() -> serializer.dataToBytes(Map.of("messages", List.of(message))));
		assertConstraintCause(failure);
	}

	private static void assertConstraintCause(Throwable failure) {
		while (failure.getCause() != null) {
			failure = failure.getCause();
		}
		assertInstanceOf(StreamConstraintsException.class, failure);
	}

	private static SpringAIJacksonStateSerializer serializer(int limit) {
		return new SpringAIJacksonStateSerializer(OverAllState::new, constrainedMapper(limit));
	}

	private static ObjectMapper plainMapper(int limit) {
		SimpleModule module = new SimpleModule();
		SpringAIJacksonStateSerializer.registerMessageHandlers(module);
		return constrainedMapper(limit).registerModule(module);
	}

	private static ObjectMapper constrainedMapper(int limit) {
		return new ObjectMapper(JsonFactory.builder()
			.streamReadConstraints(StreamReadConstraints.builder().maxStringLength(limit).build())
			.build());
	}

	private static UserMessage orderedMessage(Media first) {
		return message(first, binary(3, false), media(MediaStringField.URI_DATA, 32));
	}

	private static UserMessage message(Media... media) {
		return UserMessage.builder().text("offline").metadata(Map.of("origin", "constraints")).media(media).build();
	}

	private static Media media(MediaStringField field, int length) {
		MimeType mimeType = new MimeType("a", "b");
		String id = "i";
		String name = "n";
		Object data = "d";
		switch (field) {
			case MIME_TYPE -> mimeType = new MimeType("a", "b".repeat(length - 2));
			case MIME_PARAMETERS -> mimeType = new MimeType("a", "b", Map.of("x", "p".repeat(length - 6)));
			case ID -> id = "i".repeat(length);
			case NAME -> name = "n".repeat(length);
			case STRING_DATA -> data = "d".repeat(length);
			case URI_DATA -> data = URI.create("https://example.invalid/" + "a".repeat(length - 24));
		}
		Media.Builder builder = Media.builder().mimeType(mimeType).id(id).name(name);
		return (data instanceof URI uri ? builder.data(uri) : builder.data(data)).build();
	}

	private static Media binary(int size, boolean resource) {
		byte[] data = new byte[size];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) i;
		}
		Media.Builder builder = Media.builder().mimeType(new MimeType("a", "b")).id("binary").name("offline.bin");
		return (resource ? builder.data(new ByteArrayResource(data)) : builder.data(data)).build();
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
				assertInstanceOf(byte[].class, after.getData());
				assertArrayEquals(bytes, after.getDataAsByteArray());
			}
			else {
				assertInstanceOf(String.class, after.getData());
				assertEquals(before.getData(), after.getData());
			}
		}
	}

	enum MediaStringField {

		MIME_TYPE, MIME_PARAMETERS, ID, NAME, STRING_DATA, URI_DATA

	}

	enum UnknownData {

		OBJECT, URI_AS_OBJECT, RESOURCE_AS_OBJECT

	}

}
