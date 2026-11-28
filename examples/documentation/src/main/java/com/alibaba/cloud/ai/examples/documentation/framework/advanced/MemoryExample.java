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
package com.alibaba.cloud.ai.examples.documentation.framework.advanced;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.memory.LongTermMemoryTools;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

/** Thread-local notes, approval recovery, and scoped cross-thread preferences. */
public class MemoryExample {

	private static final Logger logger = LoggerFactory.getLogger(MemoryExample.class);

	private final ChatModel chatModel;

	public MemoryExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	public static void main(String[] args) throws GraphRunnerException {
		var api = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
		var model = DashScopeChatModel.builder().dashScopeApi(api).build();
		new MemoryExample(model).runAllExamples();
	}

	public void runAllExamples() throws GraphRunnerException {
		threadStateAndApproval();
		scopedRecall();
	}

	private void threadStateAndApproval() throws GraphRunnerException {
		var noteState = new AgentHook() {
			@Override
			public String getName() {
				return "thread_notes";
			}

			@Override
			public Map<String, KeyStrategy> getKeyStrategys() {
				return Map.of("notes", new AppendStrategy());
			}
		};
		var addNote = FunctionToolCallback.builder("add_thread_note",
				(BiFunction<Note, ToolContext, String>) (note, context) -> {
					int previousNotes = ToolContextHelper.getState(context).orElseThrow()
							.<List<String>>value("notes").orElseGet(List::of).size();
					ToolContextHelper.getStateForUpdate(context).orElseThrow().put("notes", List.of(note.text()));
					return "Recorded note " + (previousNotes + 1);
				}).description("Record one note in the current thread").inputType(Note.class)
				.toolMetadata(ToolMetadata.builder().requiresConfirmation(true).build()).build();

		var agent = ReactAgent.builder().name("thread-notes").model(this.chatModel)
				.saver(new MemorySaver()).hooks(noteState).tools(addNote).build();
		var config = RunnableConfig.builder().threadId("authorized-thread-1").build();
		agent.invokeAndGetOutput("Use add_thread_note once to record that Atlas ships on Friday.", config);
		var request = agent.getPendingInterruption(config).orElseThrow();
		// In an application, these decisions come from the authenticated reviewer.
		var decisions = request.toolFeedbacks().stream()
				.map(call -> ToolFeedback.builder(call).result(FeedbackResult.APPROVED).build()).toList();
		var feedback = InterruptionMetadata.builder(request).toolFeedbacks(decisions).build();
		agent.invokeAndGetOutput(Map.of(), RunnableConfig.builder(config).addHumanFeedback(feedback).build());
		logger.info("Thread notes: {}", agent.getCurrentState(config).state().value("notes").orElseThrow());
	}

	private void scopedRecall() throws GraphRunnerException {
		var store = new MemoryStore();
		var first = config("authorized-thread-2", "tenant-a", "alice");
		var second = config("authorized-thread-3", "tenant-a", "alice");
		store.putItem(StoreItem.of(namespace(first), "reply_language", Map.of("language", "zh-CN")));

		var memory = LongTermMemoryTools.builder().store(store).namespace(MemoryExample::namespace)
				.maxSearchResults(5).pageCharacters(1024).maxCallsPerToolPerTurn(8).build();
		var agent = ReactAgent.builder().name("preference-recall").model(this.chatModel)
				.saver(new MemorySaver()).tools(memory.tools()).build();

		agent.call("This thread is about the Atlas project.", first);
		var reply = agent.call("Use read_memory to retrieve reply_language, then tell me the preference.", second);
		logger.info("Cross-thread preference: {}", reply.getText());
		logger.info("Allowlisted deterministic preference: {}", preferredLanguage(store, second));
	}

	static String preferredLanguage(Store store, RunnableConfig config) {
		var scope = namespace(config);
		if (scope.isEmpty()) {
			return "en-US";
		}
		return store.getItem(scope, "reply_language").map(item -> item.getValue().get("language"))
				.filter(String.class::isInstance).map(String.class::cast)
				.filter(language -> List.of("en-US", "zh-CN").contains(language)).orElse("en-US");
	}

	private static List<String> namespace(RunnableConfig config) {
		return config.metadata("tenant").filter(String.class::isInstance).map(String.class::cast)
				.flatMap(tenant -> config.metadata("user").filter(String.class::isInstance).map(String.class::cast)
					.map(user -> List.of(tenant, "support", user, "preferences")))
				.orElseGet(List::of);
	}

	private static RunnableConfig config(String threadId, String tenant, String user) {
		// Resolve these values from application authorization, then build the run config.
		return RunnableConfig.builder().threadId(threadId).addMetadata("tenant", tenant).addMetadata("user", user).build();
	}

	private record Note(String text) {
	}

}

