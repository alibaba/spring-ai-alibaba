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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.context.AgentToolContext;
import com.alibaba.cloud.ai.graph.agent.context.CheckpointContextCompaction;
import com.alibaba.cloud.ai.graph.agent.context.ContextWindowPolicy;
import com.alibaba.cloud.ai.graph.agent.context.ModelCapacity;
import com.alibaba.cloud.ai.graph.agent.context.ToolResultArchive;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.modelfallback.ModelFallbackInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.agent.memory.LongTermMemoryTools;
import com.alibaba.cloud.ai.graph.agent.model.ModelRoutingPolicy;
import com.alibaba.cloud.ai.graph.agent.model.ModelTarget;
import com.alibaba.cloud.ai.graph.agent.tool.DeferredToolCatalog;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.FileSystemStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.util.JsonHelper;

/** Runs focused context-management recipes with local model responses. */
public final class ContextManagementRecipes {

	private static final Logger logger = LoggerFactory.getLogger(ContextManagementRecipes.class);

	private static final JsonHelper JSON = new JsonHelper();

	private static final Pattern REFERENCE = Pattern.compile("reference \"([a-f0-9]{64})\"");

	private static final String HISTORY = "Archived inventory discussion. ".repeat(100);

	private static final String SUMMARY = "The earlier inventory discussion is complete.";

	private static final String REPORT = "Inventory page evidence. ".repeat(100);

	private static final String ARCHIVE_AGENT = "inventory";

	private ContextManagementRecipes() {
	}

	public static void main(String[] args) throws Exception {
		Path directory = args.length == 0 ? Files.createTempDirectory("context-recipes-") : Path.of(args[0]);
		logger.info("Model budget: {}", routeWithCapacity());
		logger.info("Archive and checkpoint: {}", archiveAcrossRuns(directory.resolve("archive")));
		logger.info("Scoped delegation: {}", delegateWithOnDemandContext(directory.resolve("delegate")));
	}

	public static RoutingResult routeWithCapacity() throws GraphRunnerException {
		var primaryCalls = new AtomicInteger();
		var primary = new LocalModel(prompt -> {
			primaryCalls.incrementAndGet();
			return answer("compact model answered");
		});
		var fallback = new LocalModel(prompt -> answer("wide model answered"));
		var smallBudget = ContextWindowPolicy.builder(new ModelCapacity(64, 32, 16)).build();
		var wideBudget = ContextWindowPolicy.builder(new ModelCapacity(2048, 1024, 128)).build();
		var routes = ModelRoutingPolicy.builder("general")
			.route("general", new ModelTarget(fallback, wideBudget))
			.route("compact", new ModelTarget(primary, smallBudget))
			.selector(request -> "inventory".equals(request.getContext().get("workload")) ? "compact" : "general")
			.build();
		var agent = ReactAgent.builder().name("capacity_recipe").modelRouting(routes)
			.interceptors(ModelFallbackInterceptor.builder().addFallbackModel(fallback, wideBudget).build())
			.build();
		var config = RunnableConfig.builder().addMetadata("workload", "inventory").build();
		var result = agent.call("Summarize this inventory evidence. ".repeat(15), config);
		return new RoutingResult(result.getText(), primaryCalls.get());
	}

	public static ArchiveResult archiveAcrossRuns(Path directory) throws GraphRunnerException {
		var summaryCalls = new AtomicInteger();
		ChatModel summarizer = new LocalModel(prompt -> {
			summaryCalls.incrementAndGet();
			return answer(SUMMARY);
		});
		var compaction = CheckpointContextCompaction.builder(summarizer, 300).keepRecentTurns(1).build();
		String threadId = "inventory-" + UUID.randomUUID();
		var thread = RunnableConfig.builder().threadId(threadId).addMetadata("tenant", "tenant-a").build();
		var producer = FunctionToolCallback.builder("inventory_report", (Map<String, Object> input) -> REPORT)
			.description("Produce a detailed inventory report").inputType(Map.class).build();
		var initialModel = new LocalModel(prompt -> result(prompt, "report").isEmpty()
				? tool("report", "inventory_report", Map.of()) : answer("Inventory report archived."));
		var first = archiveAgent(directory, initialModel, compaction, producer);
		var initial = first.invokeAndGetOutput(List.of(new UserMessage(HISTORY),
				new AssistantMessage("Prior review complete."), new UserMessage("Archive the latest report.")), thread)
			.orElseThrow();
		var reference = reference(result(initial.state().<List<Message>>value("messages").orElseThrow(), "report"));

		var summaryRestored = new AtomicBoolean();
		var resumedModel = new LocalModel(prompt -> {
			if (result(prompt, "page").isEmpty()) {
				summaryRestored.set(prompt.getInstructions().stream().map(Message::getText)
					.anyMatch(text -> text != null && text.contains(SUMMARY)));
				return tool("page", ToolResultArchive.READ_TOOL_NAME, Map.of("reference", reference, "page", 1));
			}
			return answer(result(prompt, "page"));
		});
		var reopened = archiveAgent(directory, resumedModel, compaction, producer);
		var restored = reopened.call("Read the first page of the archived report.", thread);
		var page = JSON.fromJsonToMap(restored.getText());
		var otherTenant = RunnableConfig.builder().threadId(threadId).addMetadata("tenant", "tenant-b").build();
		boolean foreignReadRejected = archive(directory).readPage(otherTenant, ARCHIVE_AGENT, reference, 1).isEmpty();
		return new ArchiveResult((String) page.get("text"), ((Number) page.get("nextPage")).intValue(),
				summaryCalls.get(), summaryRestored.get(), foreignReadRejected);
	}

	public static DelegationResult delegateWithOnDemandContext(Path directory) throws GraphRunnerException {
		var registry = ClasspathSkillRegistry.builder().classpathPath("context-recipes/skills")
			.basePath(directory.resolve("skill-cache").toString()).build();
		var skills = SkillsAgentHook.builder().skillRegistry(registry).onDemand(2, 512).build();
		var memory = new FileSystemStore(directory.resolve("memory"));
		memory.putItem(StoreItem.of(List.of("inventory", "tenant-a"), "review_channel", Map.of("channel", "warehouse-a")));
		var memoryTools = LongTermMemoryTools.builder().store(memory)
			.namespace(config -> config.metadata("tenant").filter(String.class::isInstance).map(String.class::cast)
				.map(tenant -> List.of("inventory", tenant)).orElseGet(List::of))
			.maxSearchResults(2).pageCharacters(128).build();
		var childSawOnlySelectedData = new AtomicBoolean();
		var analystModel = new LocalModel(prompt -> {
			if (result(prompt, "skill-search").isEmpty()) {
				var childInput = prompt.getUserMessages().stream().map(UserMessage::getText).toList();
				childSawOnlySelectedData.set(childInput.stream().anyMatch(text -> text.contains("INV-42"))
						&& childInput.stream().noneMatch(text -> text.contains("Parent-only")
								|| text.contains("Review inventory case")));
				return tool("skill-search", "search_skills", Map.of("query", "inventory"));
			}
			String skill = result(prompt, "skill-read");
			if (skill.isEmpty()) {
				return tool("skill-read", "read_skill", Map.of("skill_name", "inventory-review"));
			}
			if (!skill.contains("Use the delegated case ID")) {
				throw new IllegalStateException("The inventory skill was not loaded");
			}
			if (result(prompt, "memory-search").isEmpty()) {
				return tool("memory-search", "search_memory", Map.of("query", "channel"));
			}
			if (result(prompt, "memory-read").isEmpty()) {
				return tool("memory-read", "read_memory", Map.of("key", "review_channel"));
			}
			var page = JSON.fromJsonToMap(result(prompt, "memory-read"));
			var memoryData = JSON.fromJsonToMap((String) page.get("content"));
			return answer("Inventory reviewed for " + memoryData.get("channel"));
		});
		var analyst = ReactAgent.builder().name("inventory_analyst").description("Review the delegated inventory case")
			.model(analystModel).outputKey("review").hooks(List.of(skills)).tools(memoryTools.tools()).build();
		var policy = AgentToolContext.builder().stateKeys("case_id").metadataKeys("tenant").outputKeys("review").build();
		var loaded = new AtomicInteger();
		var catalog = DeferredToolCatalog.builder().tool("inventory_analyst", "Review an inventory case", () -> {
			loaded.incrementAndGet();
			return AgentTool.create(analyst, policy);
		}).build();
		var workspaceModel = new LocalModel(prompt -> {
			if (result(prompt, "discover").isEmpty()) {
				return tool("discover", "search_tools", Map.of("query", "inventory"));
			}
			if (result(prompt, "load").isEmpty()) {
				return tool("load", "load_tools", Map.of("names", List.of("inventory_analyst")));
			}
			if (result(prompt, "delegate").isEmpty()) {
				return tool("delegate", "inventory_analyst", Map.of("input", "Review the assigned case."));
			}
			return answer(result(prompt, "delegate"));
		});
		var glossary = FunctionToolCallback.builder("lookup_glossary", (Map<String, Object> input) -> "glossary")
			.description("Look up terminology").inputType(Map.class).build();
		var selector = ToolSelectionInterceptor.builder()
			.selectionModel(new LocalModel(prompt -> answer("{\"tools\":[\"inventory_analyst\"]}")))
			.maxTools(1).build();
		var parent = ReactAgent.builder().name("inventory_coordinator").model(workspaceModel)
			.deferredTools(catalog).tools(glossary).interceptors(selector).build();
		var config = RunnableConfig.builder().threadId("delegation").addMetadata("tenant", "tenant-a").build();
		var state = Map.<String, Object>of("messages", List.of(new UserMessage("Review inventory case INV-42.")),
				"case_id", "INV-42", "internal_notes", "Parent-only investigation.");
		var result = parent.call(state, config);
		return new DelegationResult(result.getText(), loaded.get(), childSawOnlySelectedData.get());
	}

	private static ReactAgent archiveAgent(Path directory, ChatModel model, CheckpointContextCompaction compaction,
			ToolCallback producer) {
		return ReactAgent.builder().name(ARCHIVE_AGENT).model(model).tools(producer)
			.toolResultArchive(archive(directory)).checkpointContextCompaction(compaction)
			.saver(FileSystemSaver.builder().targetFolder(directory.resolve("checkpoints")).build()).build();
	}

	private static ToolResultArchive archive(Path directory) {
		return ToolResultArchive.builder(new FileSystemStore(directory.resolve("store")))
			.ownerScope(config -> config.metadata("tenant").filter(String.class::isInstance).map(String.class::cast)
				.orElse(""))
			.thresholdChars(500).pageSizeChars(128).build();
	}

	private static String reference(String text) {
		var matcher = REFERENCE.matcher(text);
		if (!matcher.find()) {
			throw new IllegalStateException("No archive reference was produced");
		}
		return matcher.group(1);
	}

	private static String result(Prompt prompt, String id) {
		return result(prompt.getInstructions(), id);
	}

	private static String result(List<Message> messages, String id) {
		return messages.stream().filter(ToolResponseMessage.class::isInstance).map(ToolResponseMessage.class::cast)
			.flatMap(message -> message.getResponses().stream()).filter(response -> id.equals(response.id()))
			.map(ToolResponseMessage.ToolResponse::responseData).findFirst().orElse("");
	}

	private static ChatResponse tool(String id, String name, Map<String, Object> arguments) {
		var call = new ToolCall(id, "function", name, JSON.toJson(arguments));
		return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(call)).build())));
	}

	private static ChatResponse answer(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private record LocalModel(Function<Prompt, ChatResponse> response) implements ChatModel {

		@Override
		public ChatResponse call(Prompt prompt) {
			return this.response.apply(prompt);
		}

		@Override
		public ToolCallingChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}

	}

	public record RoutingResult(String answer, int primaryCalls) {
	}

	public record ArchiveResult(String firstPage, int nextPage, int summaryCalls, boolean summaryRestored,
			boolean foreignReadRejected) {
	}

	public record DelegationResult(String answer, int factoriesCalled, boolean childSawOnlySelectedData) {
	}

}
