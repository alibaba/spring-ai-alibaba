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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
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
import com.openai.errors.OpenAIServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

/** Drives a report from discovery and archival through a delegated approval and checkpoint resume. */
public final class OpenAiContextManagementExample implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(OpenAiContextManagementExample.class);

	static final String COORDINATOR = "report_coordinator";

	static final String OLD_NOTE = "OLDER_CASE_NOTE ".repeat(210);

	static final String REPORT = "Inventory result for case CX-42.\n" + "Line item: 15 units verified.\n".repeat(60)
			+ "End of synthetic report.";

	static final String TASK = "Audit case CX-42. Find and load the inventory report tool, fetch the report, delegate "
			+ "the audit, then use the archive reference to read the first two pages after approval.";

	private static final Duration RUN_TIMEOUT = Duration.ofMinutes(2);

	private static final String COORDINATOR_PROMPT = """
			You coordinate a report audit. Use one tool at a time.
			Discover the inventory report tool with search_tools and load it with load_tools before fetching.
			Keep its archive reference. Delegate the audit to audit_analyst with a short task.
			After the audit completes, read two archived pages and summarize the result.
			""";

	private static final String ANALYST_PROMPT = """
			You are the audit analyst. Use one tool at a time.
			Find the report audit skill and read it by name. Then request to record the audit.
			After approval, search long-term memory for the delivery label and read the matching key.
			Return the audit result with the delivery label.
			""";

	private final Path workspace;

	private final Settings settings;

	private final ExecutorService dispatcher = Executors.newCachedThreadPool();

	final AtomicInteger routeSelections = new AtomicInteger();

	final AtomicInteger providerOverflows = new AtomicInteger();

	final AtomicInteger reportFetches = new AtomicInteger();

	final AtomicInteger auditRecordings = new AtomicInteger();

	public OpenAiContextManagementExample(Path workspace, Settings settings) {
		this.workspace = workspace;
		this.settings = settings;
	}

	public InterruptionMetadata start(String threadId) throws GraphRunnerException {
		memoryStore().putItem(StoreItem.of(List.of("context-demo", "demo-tenant"), "delivery_policy",
				Map.of("label", "policy-42", "purpose", "delivery label for approved audits")));
		var history = List.of(new UserMessage(OLD_NOTE), new AssistantMessage("Earlier inventory task closed."),
				new UserMessage(TASK));
		var inputs = Map.<String, Object>of("messages", history, "case_id", "CX-42", "private_note", "parent-internal");
		NodeOutput output = coordinator().stream(inputs, config(threadId)).blockLast(RUN_TIMEOUT);
		if (output instanceof InterruptionMetadata pause && !pause.toolFeedbacks().isEmpty()) {
			return pause;
		}
		throw new IllegalStateException("The model did not request audit approval");
	}

	public NodeOutput resume(String threadId, InterruptionMetadata feedback) throws GraphRunnerException {
		var run = RunnableConfig.builder(config(threadId)).addHumanFeedback(feedback).build();
		return coordinator().stream(Map.of(), run).blockLast(RUN_TIMEOUT);
	}

	static InterruptionMetadata approve(InterruptionMetadata pause) {
		var feedback = InterruptionMetadata.builder(pause);
		pause.toolFeedbacks().forEach(tool -> feedback.addToolFeedback(
				ToolFeedback.builder(tool).result(FeedbackResult.APPROVED).build()));
		return feedback.build();
	}

	private ReactAgent coordinator() {
		var primary = openAi(this.settings.primaryModel(), 200);
		var fallback = openAi(this.settings.fallbackModel(), 600);
		var fallbackBudget = budget(16_000, 600);
		var routing = ModelRoutingPolicy.builder("general").route("general", new ModelTarget(fallback, fallbackBudget))
			.route("audit", new ModelTarget(primary, budget(this.settings.primaryInputLimit(), 200)))
			.selector(request -> {
				this.routeSelections.incrementAndGet();
				return "audit".equals(request.getContext().get("workflow")) ? "audit" : "general";
			}).build();
		var catalog = DeferredToolCatalog.builder().tool("fetch_report", "Fetch the inventory report for the case", () ->
			FunctionToolCallback.builder("fetch_report", (Map<String, Object> input) -> {
				this.reportFetches.incrementAndGet();
				return REPORT;
			}).description("Fetch the inventory report for the case").inputType(Map.class).build()).build();
		var archive = ToolResultArchive.builder(new FileSystemStore(this.workspace.resolve("archive")))
			.ownerScope(config -> tenant(config).orElse(""))
			.thresholdChars(600).pageSizeChars(128).build();
		var delegation = AgentToolContext.builder().stateKeys("case_id").metadataKeys("tenant")
			.outputKeys("audit_result").build();
		var selector = ToolSelectionInterceptor.builder().selectionModel(fallback).maxTools(3)
			.alwaysInclude("audit_analyst", ToolResultArchive.READ_TOOL_NAME).build();
		var unused = FunctionToolCallback.builder("lookup_reference", (Map<String, Object> input) -> "reference")
			.description("Look up a glossary reference unrelated to inventory").inputType(Map.class).build();

		return ReactAgent.builder().name(COORDINATOR).systemPrompt(COORDINATOR_PROMPT).modelRouting(routing)
			.interceptors(selector, ModelFallbackInterceptor.builder().addFallbackModel(fallback, fallbackBudget).build())
			.tools(AgentTool.create(analyst(fallback, fallbackBudget), delegation), unused)
			.deferredTools(catalog).toolResultArchive(archive)
			.checkpointContextCompaction(CheckpointContextCompaction.builder(fallback, 450).keepRecentTurns(1).build())
			.saver(saver("coordinator")).build();
	}

	private ReactAgent analyst(OpenAiChatModel model, ContextWindowPolicy budget) {
		var registry = ClasspathSkillRegistry.builder().classpathPath("context-management/skills")
			.basePath(this.workspace.resolve("skill-cache").toString()).build();
		var skills = SkillsAgentHook.builder().skillRegistry(registry).onDemand(2, 512).build();
		var memory = LongTermMemoryTools.builder().store(memoryStore())
			.namespace(config -> tenant(config).map(value -> List.of("context-demo", value)).orElseGet(List::of))
			.maxSearchResults(2).pageCharacters(128).build();
		var record = FunctionToolCallback.builder("record_audit", (Map<String, Object> input) -> {
			this.auditRecordings.incrementAndGet();
			return "Approved audit recorded for CX-42";
		}).description("Record the approved inventory audit for this case").inputType(Map.class)
			.toolMetadata(ToolMetadata.builder().requiresConfirmation(true).build()).build();
		return ReactAgent.builder().name("audit_analyst").description("Audit the delegated inventory case")
			.systemPrompt(ANALYST_PROMPT).model(model).contextWindowPolicy(budget).outputKey("audit_result")
			.hooks(List.of(skills)).tools(memory.tools()).tools(record).saver(saver("analyst")).build();
	}

	private OpenAiChatModel openAi(String model, int outputTokens) {
		return OpenAiChatModel.builder()
			.httpClientBuilderCustomizer(client -> client.dispatcherExecutorService(this.dispatcher))
			.options(OpenAiChatOptions.builder().baseUrl(this.settings.baseUrl())
				.apiKey(this.settings.apiKey()).model(model).maxTokens(outputTokens).maxRetries(0)
				.timeout(Duration.ofSeconds(30)).build()).build();
	}

	private ContextWindowPolicy budget(int inputLimit, int reserved) {
		return ContextWindowPolicy.builder(new ModelCapacity(32_768, inputLimit, reserved))
			.inputOverflowDetector(this::isProviderOverflow)
			.build();
	}

	private boolean isProviderOverflow(Throwable error) {
		if (error instanceof OpenAIServiceException service
				&& service.code().filter("context_length_exceeded"::equals).isPresent()) {
			this.providerOverflows.incrementAndGet();
			return true;
		}
		return false;
	}

	private static RunnableConfig config(String threadId) {
		return RunnableConfig.builder().threadId(threadId).addMetadata("tenant", "demo-tenant")
			.addMetadata("workflow", "audit").build();
	}

	private static Optional<String> tenant(RunnableConfig config) {
		return config.metadata("tenant").filter(String.class::isInstance).map(String.class::cast);
	}

	private FileSystemStore memoryStore() {
		return new FileSystemStore(this.workspace.resolve("memory"));
	}

	private FileSystemSaver saver(String name) {
		return FileSystemSaver.builder().targetFolder(this.workspace.resolve(name + "-checkpoints")).build();
	}

	@Override
	public void close() {
		this.dispatcher.shutdownNow();
	}

	public static void main(String[] args) throws Exception {
		var settings = new Settings(System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"),
				requiredEnvironment("OPENAI_API_KEY"), requiredEnvironment("OPENAI_PRIMARY_MODEL"),
				requiredEnvironment("OPENAI_FALLBACK_MODEL"),
				Integer.parseInt(System.getenv().getOrDefault("OPENAI_PRIMARY_INPUT_LIMIT", "128")));
		Path workspace = args.length == 0 ? Files.createTempDirectory("agent-context-") : Path.of(args[0]);
		String threadId = UUID.randomUUID().toString();
		var beforeRestart = new OpenAiContextManagementExample(workspace, settings);
		InterruptionMetadata pause;
		try (beforeRestart) {
			pause = beforeRestart.start(threadId);
		}
		logger.info("Paused for {} approval(s) in {}", pause.toolFeedbacks().size(), workspace.toAbsolutePath());

		// The approval is simulated for this example. Recording only updates a demo counter.
		try (var restarted = new OpenAiContextManagementExample(workspace, settings)) {
			var completed = restarted.resume(threadId, approve(pause));
			if (completed == null || completed instanceof InterruptionMetadata) {
				throw new IllegalStateException("The example did not complete after the audit approval");
			}
			var messages = completed.state().<List<Message>>value("messages").orElseThrow();
			logger.info("Final answer: {}", messages.get(messages.size() - 1).getText());
			logger.info("Report fetches before/after restart: {}/{}", beforeRestart.reportFetches.get(),
					restarted.reportFetches.get());
			logger.info("Audit recordings before/after restart: {}/{}", beforeRestart.auditRecordings.get(),
					restarted.auditRecordings.get());
		}
	}

	private static String requiredEnvironment(String name) {
		var value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Set the " + name + " environment variable");
		}
		return value;
	}

	public record Settings(String baseUrl, String apiKey, String primaryModel, String fallbackModel, int primaryInputLimit) {
	}

}
