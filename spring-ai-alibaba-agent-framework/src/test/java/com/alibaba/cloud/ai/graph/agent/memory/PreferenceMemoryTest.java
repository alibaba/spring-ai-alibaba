/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.memory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import javax.sql.DataSource;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.Key;
import com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.Receipt;
import com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.Status;
import com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.ToolWritePermit;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.util.JsonHelper;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.Key.REPLY_FORMAT;
import static com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.Key.REPLY_LANGUAGE;
import static com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.READ_PREFERENCE;
import static com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.SAVE_PREFERENCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreferenceMemoryTest {

	private static final JsonHelper JSON = new JsonHelper();

	private static final String PERMIT = "preferencePermit";

	private static final String SAVE_LANGUAGE = "{\"key\":\"reply_language\",\"value\":\"zh-CN\",\"expectedVersion\":0}";

	@TempDir
	Path directory;

	@Test
	void savesReadsCorrectsAndDeletesAcrossAuthorizedThreads() throws Exception {
		var data = h2(this.directory.resolve("lifecycle"));
		var memory = memory(data);
		memory.initializeSchema();
		var first = config("first", "tenant-a", "alice", true);
		var otherThread = config("second", "tenant-a", "alice", false);

		assertThat(memory.tools()).extracting(tool -> tool.getToolDefinition().name()).containsExactly(READ_PREFERENCE);
		assertThat(memory.read(first, REPLY_LANGUAGE).version()).isZero();
		assertThat(memory.save(first, REPLY_LANGUAGE, "zh-CN", 0, "language-1"))
				.isEqualTo(new Receipt(Status.SAVED, 1, false));
		assertThat(memory.save(first, REPLY_FORMAT, "bullets", 0, "format-1"))
				.isEqualTo(new Receipt(Status.SAVED, 1, false));
		var reopened = memory(h2(this.directory.resolve("lifecycle")));
		assertThat(readWithAgent(reopened, otherThread, REPLY_LANGUAGE))
				.containsEntry("version", 1).containsEntry("value", "zh-CN");
		var format = reopened.read(otherThread, REPLY_FORMAT);
		assertThat(format.item().orElseThrow().getValue()).containsEntry("value", "bullets")
				.containsEntry("schemaVersion", 1).containsEntry("source", "application");
		assertThat(readWithAgent(reopened, otherThread, REPLY_FORMAT)).containsEntry("value", "bullets");
		assertThat(reopened.read(config("third", "tenant-a", "bob", false), REPLY_LANGUAGE).value()).isEmpty();
		assertThat(reopened.read(config("fourth", "tenant-b", "alice", false), REPLY_LANGUAGE).value()).isEmpty();

		assertThat(reopened.save(first, REPLY_LANGUAGE, "en-US", 1, "language-2"))
				.isEqualTo(new Receipt(Status.SAVED, 2, false));
		assertThat(readWithAgent(reopened, otherThread, REPLY_LANGUAGE)).containsEntry("value", "en-US");
		assertThat(reopened.delete(first, REPLY_LANGUAGE, 2, "delete-language"))
				.isEqualTo(new Receipt(Status.DELETED, 3, false));
		assertThat(readWithAgent(reopened, otherThread, REPLY_LANGUAGE)).containsEntry("found", false).containsEntry("version", 3)
				.doesNotContainKey("value");
		assertThat(reopened.read(otherThread, REPLY_FORMAT).value()).contains("bullets");
		assertThat(reopened.save(first, REPLY_FORMAT, "markdown", 1, "format-2"))
				.isEqualTo(new Receipt(Status.SAVED, 2, false));
		assertThat(readWithAgent(reopened, otherThread, REPLY_FORMAT)).containsEntry("value", "markdown");
		assertThat(reopened.delete(first, REPLY_FORMAT, 2, "delete-format"))
				.isEqualTo(new Receipt(Status.DELETED, 3, false));
		assertThat(readWithAgent(reopened, otherThread, REPLY_FORMAT))
				.containsEntry("found", false).containsEntry("version", 3).doesNotContainKey("value");
		assertThat(reopened.save(first, REPLY_LANGUAGE, "zh-CN", 0, "language-1"))
				.isEqualTo(new Receipt(Status.SAVED, 1, true));
		assertThat(reopened.save(first, REPLY_LANGUAGE, "zh-CN", 2, "stale"))
				.isEqualTo(new Receipt(Status.VERSION_CONFLICT, 3, false));
		assertThat(reopened.save(first, REPLY_LANGUAGE, "en-US", 3, "stale").status())
				.isEqualTo(Status.OPERATION_CONFLICT);
		assertThat(reopened.delete(first, REPLY_LANGUAGE, 2, "delete-language"))
				.isEqualTo(new Receipt(Status.DELETED, 3, true));
		assertThat(reopened.read(otherThread, REPLY_LANGUAGE).value()).isEmpty();
	}

	@Test
	void enforcesScopeAuthorizationAndAllowedValues() {
		var memory = memory(h2(this.directory.resolve("permissions")));
		memory.initializeSchema();
		var onlyRead = config("reader", "tenant", "alice", false);
		var writer = config("writer", "tenant", "alice", true);
		assertThatThrownBy(() -> memory.read(config("invalid", "tenant", "../alice", true), REPLY_LANGUAGE))
				.isInstanceOf(SecurityException.class);
		assertThatThrownBy(() -> memory.save(onlyRead, REPLY_LANGUAGE, "zh-CN", 0, "denied"))
				.isInstanceOf(SecurityException.class);
		assertThatThrownBy(() -> memory.delete(onlyRead, REPLY_LANGUAGE, 0, "denied"))
				.isInstanceOf(SecurityException.class);
		assertThatThrownBy(() -> memory.save(writer, REPLY_LANGUAGE, "arbitrary text", 0, "invalid-language"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> memory.save(writer, REPLY_FORMAT, "arbitrary text", 0, "invalid-format"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(memory.read(writer, REPLY_LANGUAGE).version()).isZero();
	}

	@Test
	void rollsBackThePreferenceWhenItsReceiptCannotBeStored() throws Exception {
		var data = h2(this.directory.resolve("rollback"));
		var memory = memory(data);
		memory.initializeSchema();
		try (var connection = data.getConnection(); var statement = connection.createStatement()) {
			statement.executeUpdate("""
					ALTER TABLE saa_agent_preference_receipts ADD CONSTRAINT deny_test_receipt
					CHECK (operation_id <> 'failed-receipt')
					""");
		}
		var config = config("writer", "tenant", "alice", true);
		assertThatThrownBy(() -> memory.save(config, REPLY_FORMAT, "markdown", 0, "failed-receipt"))
				.isInstanceOf(IllegalStateException.class);
		assertThat(memory.read(config, REPLY_FORMAT).version()).isZero();
		assertThat(memory.save(config, REPLY_FORMAT, "markdown", 0, "valid-receipt").status()).isEqualTo(Status.SAVED);
	}

	@Test
	void comparesVersionsAcrossIndependentH2Connections() throws Exception {
		var database = this.directory.resolve("concurrent");
		verifyConcurrentCorrections(h2(database), h2(database));
	}

	@ParameterizedTest
	@CsvSource({ "REPLY_LANGUAGE,zh-CN,en-US", "REPLY_FORMAT,paragraphs,bullets" })
	void replaysTheApprovedReceiptAfterACommittedWriteAndFailedCheckpoint(Key key, String originalValue,
			String newerValue) throws Exception {
		var database = this.directory.resolve("approval-memory");
		verifyApprovedReceiptAfterCheckpointFailure(h2(database), h2(database),
				this.directory.resolve("approval-checkpoints"), key, originalValue, Optional.of(newerValue));
	}

	static void verifyApprovedReceiptAfterCheckpointFailure(DataSource initialConnection, DataSource reopenedConnection,
			Path checkpoints, Key key, String originalValue, Optional<String> newerValue) throws Exception {
		var memory = toolMemory(initialConnection);
		memory.initializeSchema();
		var config = config("approval-thread", "tenant", "alice", true);
		var saver = new FailToolCheckpoint(saver(checkpoints));
		String arguments = JSON.toJson(Map.of("key", key.storeKey(), "value", originalValue, "expectedVersion", 0));
		var agent = savingAgent(memory, saver, arguments);
		var pause = (InterruptionMetadata) agent.invokeAndGetOutput("Remember my preference for future conversations.", config)
				.orElseThrow();
		assertThat(memory.read(config, key).value()).isEmpty();

		var approved = ToolFeedback.builder(pause.toolFeedbacks().get(0)).result(FeedbackResult.APPROVED).build();
		var permit = memory.permitSave(config, pause, approved, "approved-preference");
		var authorized = RunnableConfig.builder(config).addMetadata(PERMIT, permit).build();
		var feedback = InterruptionMetadata.builder(pause).toolFeedbacks(List.of(approved)).build();
		saver.failNext.set(true);
		assertThatThrownBy(() -> agent.invokeAndGetOutput(Map.of(),
				RunnableConfig.builder(authorized).addHumanFeedback(feedback).build()))
				.hasStackTraceContaining("Simulated tool checkpoint failure");

		var reopened = toolMemory(reopenedConnection);
		assertThat(reopened.read(config, key).item().orElseThrow().getValue())
				.containsEntry("source", "approved_tool").containsEntry("value", originalValue);
		var secondThread = config("correction-thread", "tenant", "alice", true);
		if (newerValue.isPresent()) {
			assertThat(reopened.save(secondThread, key, newerValue.orElseThrow(), 1, "newer-correction"))
					.isEqualTo(new Receipt(Status.SAVED, 2, false));
		}
		else {
			assertThat(reopened.delete(secondThread, key, 1, "later-deletion"))
					.isEqualTo(new Receipt(Status.DELETED, 2, false));
		}

		var restarted = savingAgent(reopened, saver(checkpoints), arguments);
		var restoredPermit = JSON.fromJson(JSON.toJson(permit), ToolWritePermit.class);
		var retryConfig = RunnableConfig.builder(config).addMetadata(PERMIT, restoredPermit).resume().build();
		var retry = restarted.stream(Map.of(), retryConfig).blockLast(Duration.ofSeconds(10));
		assertThat(answerJson(retry)).containsEntry("status", "SAVED").containsEntry("version", 1)
				.containsEntry("replayed", true);
		assertThat(reopened.read(secondThread, key).version()).isEqualTo(2);
		var recalled = assertThat(readWithAgent(reopened, secondThread, key))
				.containsEntry("version", 2).containsEntry("found", newerValue.isPresent());
		if (newerValue.isPresent()) {
			recalled.containsEntry("value", newerValue.orElseThrow());
		}
		else {
			recalled.doesNotContainKey("value");
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void rechecksWriteAuthorizationAndNamespaceAfterApproval(boolean switchNamespace) throws Exception {
		var memory = toolMemory(h2(this.directory.resolve("revoked-memory")));
		memory.initializeSchema();
		var config = config("revoked", "tenant", "alice", true);
		var agent = savingAgent(memory, new MemorySaver(), SAVE_LANGUAGE);
		var pause = (InterruptionMetadata) agent.invokeAndGetOutput("Remember Chinese.", config).orElseThrow();
		var approved = ToolFeedback.builder(pause.toolFeedbacks().get(0)).result(FeedbackResult.APPROVED).build();
		var permit = memory.permitSave(config, pause, approved, "revoked-language");
		var feedback = InterruptionMetadata.builder(pause).toolFeedbacks(List.of(approved)).build();
		var changedScopeOrAuthorization = switchNamespace ? config("revoked", "tenant", "bob", true)
				: config("revoked", "tenant", "alice", false);
		var revoked = RunnableConfig.builder(changedScopeOrAuthorization)
				.addMetadata(PERMIT, permit).addHumanFeedback(feedback).build();
		var output = agent.invokeAndGetOutput(Map.of(), revoked).orElseThrow();
		assertThat(answerJson(output)).containsEntry("status", "DENIED");
		assertThat(memory.read(config, REPLY_LANGUAGE).version()).isZero();
		assertThat(memory.read(revoked, REPLY_LANGUAGE).version()).isZero();
	}

	static void verifyConcurrentCorrections(DataSource firstConnection, DataSource secondConnection) throws Exception {
		var first = memory(firstConnection);
		var second = memory(secondConnection);
		first.initializeSchema();
		var left = config("left", "race-tenant", "alice", true);
		var right = config("right", "race-tenant", "alice", true);
		first.save(left, REPLY_FORMAT, "paragraphs", 0, "initial-format");

		var changes = race(() -> first.save(left, REPLY_FORMAT, "markdown", 1, "left-edit"),
				() -> second.save(right, REPLY_FORMAT, "bullets", 1, "right-edit"));
		assertThat(changes).extracting(Receipt::status).containsExactlyInAnyOrder(Status.SAVED, Status.VERSION_CONFLICT);
		assertThat(changes).extracting(Receipt::version).containsOnly(2L);
		assertThat(second.read(right, REPLY_FORMAT).value())
				.contains(changes.get(0).status() == Status.SAVED ? "markdown" : "bullets");

		var duplicates = race(() -> first.save(left, REPLY_LANGUAGE, "zh-CN", 0, "same-request"),
				() -> second.save(right, REPLY_LANGUAGE, "zh-CN", 0, "same-request"));
		assertThat(duplicates).containsExactlyInAnyOrder(new Receipt(Status.SAVED, 1, false),
				new Receipt(Status.SAVED, 1, true));
		assertThat(second.read(right, REPLY_LANGUAGE).version()).isEqualTo(1);

		var reused = race(() -> first.save(left, REPLY_FORMAT, "paragraphs", 2, "reused-request"),
				() -> second.save(right, REPLY_LANGUAGE, "en-US", 1, "reused-request"));
		assertThat(reused).extracting(Receipt::status).containsExactlyInAnyOrder(Status.SAVED, Status.OPERATION_CONFLICT);
		assertThat(first.read(left, REPLY_FORMAT).version() + second.read(right, REPLY_LANGUAGE).version()).isEqualTo(4);
	}

	private static List<Receipt> race(Callable<Receipt> first, Callable<Receipt> second) throws Exception {
		var pool = Executors.newFixedThreadPool(2);
		var ready = new CountDownLatch(2);
		var start = new CountDownLatch(1);
		try {
			var tasks = List.of(first, second).stream().map(action -> pool.submit(() -> {
				ready.countDown();
				assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
				return action.call();
			})).toList();
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			return List.of(tasks.get(0).get(10, TimeUnit.SECONDS), tasks.get(1).get(10, TimeUnit.SECONDS));
		}
		finally {
			pool.shutdownNow();
		}
	}

	private static PreferenceMemory memory(DataSource data) {
		return builder(data).build();
	}

	private static PreferenceMemory toolMemory(DataSource data) {
		return builder(data).toolWritePermit(config -> config.metadata(PERMIT).filter(ToolWritePermit.class::isInstance)
				.map(ToolWritePermit.class::cast).orElse(null)).build();
	}

	private static PreferenceMemory.Builder builder(DataSource data) {
		return PreferenceMemory.builder(data)
				.namespace(config -> config.metadata("tenant").filter(String.class::isInstance).map(String.class::cast)
						.flatMap(tenant -> config.metadata("user").filter(String.class::isInstance).map(String.class::cast)
								.map(user -> List.of(tenant, "support", user, "preferences")))
						.orElseGet(List::of))
				.authorizeWrites(config -> config.metadata("writeAllowed").filter(Boolean.TRUE::equals).isPresent());
	}

	private static RunnableConfig config(String thread, String tenant, String user, boolean writeAllowed) {
		return RunnableConfig.builder().threadId(thread).addMetadata("tenant", tenant)
				.addMetadata("user", user).addMetadata("writeAllowed", writeAllowed).build();
	}

	private static DataSource h2(Path path) {
		var source = new JdbcDataSource();
		source.setURL("jdbc:h2:file:" + path.toAbsolutePath());
		source.setUser("sa");
		return source;
	}

	private static FileSystemSaver saver(Path path) {
		return FileSystemSaver.builder().targetFolder(path).build();
	}

	private static ReactAgent savingAgent(PreferenceMemory memory, BaseCheckpointSaver saver, String arguments) {
		return ReactAgent.builder().name("preference-save").model(model(SAVE_PREFERENCE, arguments))
				.tools(memory.tools()).saver(saver).build();
	}

	private static Map<String, Object> readWithAgent(PreferenceMemory memory, RunnableConfig config, Key key) throws Exception {
		var agent = ReactAgent.builder().name("preference-read")
				.model(model(READ_PREFERENCE, JSON.toJson(Map.of("key", key.storeKey())))).tools(memory.tools()).build();
		return JSON.fromJsonToMap(agent.call("Read my saved response preference.", config).getText());
	}

	private static Map<String, Object> answerJson(NodeOutput output) {
		var messages = output.state().<List<Message>>value("messages").orElseThrow();
		return JSON.fromJsonToMap(messages.get(messages.size() - 1).getText());
	}

	private static StubModel model(String name, String arguments) {
		return new StubModel(prompt -> {
			var toolResponse = prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance)
					.map(ToolResponseMessage.class::cast).reduce((previous, current) -> current);
			var message = toolResponse.map(result -> new AssistantMessage(result.getResponses().get(0).responseData()))
					.orElseGet(() -> AssistantMessage.builder().toolCalls(List.of(
							new AssistantMessage.ToolCall("preference-call", "function", name, arguments))).build());
			return new ChatResponse(List.of(new Generation(message)));
		});
	}

	private record StubModel(Function<Prompt, ChatResponse> responder) implements ChatModel {

		@Override
		public ChatResponse call(Prompt prompt) {
			return this.responder.apply(prompt);
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

		@Override
		public ToolCallingChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}

	}

	private static final class FailToolCheckpoint implements BaseCheckpointSaver {

		private final FileSystemSaver delegate;

		private final AtomicBoolean failNext = new AtomicBoolean();

		private FailToolCheckpoint(FileSystemSaver delegate) {
			this.delegate = delegate;
		}

		@Override
		public Collection<Checkpoint> list(RunnableConfig config) {
			return this.delegate.list(config);
		}

		@Override
		public Optional<Checkpoint> get(RunnableConfig config) {
			return this.delegate.get(config);
		}

		@Override
		public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
			if (RunnableConfig.AGENT_TOOL_NAME.equals(checkpoint.getNodeId()) && this.failNext.compareAndSet(true, false)) {
				throw new IllegalStateException("Simulated tool checkpoint failure");
			}
			return this.delegate.put(config, checkpoint);
		}

		@Override
		public Tag release(RunnableConfig config) throws Exception {
			return this.delegate.release(config);
		}

	}

}
