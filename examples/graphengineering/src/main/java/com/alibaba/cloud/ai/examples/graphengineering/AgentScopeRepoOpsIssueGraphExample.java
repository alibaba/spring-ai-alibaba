/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.graphengineering;

import com.alibaba.cloud.ai.agent.agentscope.AgentScopeAgent;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import org.springframework.util.StringUtils;
import reactor.core.scheduler.Schedulers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * RepoOps issue-driven Graph Engineering example with real AgentScope Java nodes.
 *
 * <p>
 * The graph reads an issue JSON payload, routes by issue type, and then invokes
 * AgentScope agents for triage, planning/diagnosis, implementation proposal,
 * review, and audit. Spring AI Alibaba Graph owns state, routing, fan-out/fan-in,
 * verifier loop-back, and final evidence.
 */
public class AgentScopeRepoOpsIssueGraphExample {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final String CLASSPATH_PREFIX = "classpath:";

	private static final String DEFAULT_ISSUE_REF = CLASSPATH_PREFIX + "issues/bug_issue.json";

	private static final String TOPOLOGY_RESOURCE = "repoops-graph-topology.yaml";

	private static final String GRAPH_ID_VALUE = "repoops-issue-lifecycle";

	private static final Pattern GITHUB_ISSUE_URL = Pattern
			.compile("^https://github\\.com/([^/]+)/([^/]+)/issues/(\\d+)(?:[/?#].*)?$");

	private static final Pattern ROUTE_CANDIDATE = Pattern
			.compile("(?im)^\\s*[-*]?\\s*(?:route_candidate|route|type)\\s*[:=]\\s*`?\\s*(bug|feature|question)\\b");

	private static final String ISSUE_FILE = "issue_file";

	private static final String GRAPH_ID = "graph_id";

	private static final String RUN_ID = "run_id";

	private static final String CURRENT_NODE = "current_node";

	private static final String ISSUE_URL = "issue_url";

	private static final String ISSUE_PAYLOAD = "issue_payload";

	private static final String ISSUE_TITLE = "issue_title";

	private static final String ISSUE_TYPE = "issue_type";

	private static final String ISSUE_SOURCE = "issue_source";

	private static final String ISSUE_REPOSITORY = "issue_repository";

	private static final String ISSUE_SUMMARY = "issue_summary";

	private static final String REQUIRES_REVIEW = "requires_review";

	private static final String REQUIRES_DEPLOY = "requires_deploy";

	private static final String TRIAGE_RESULT = "triage_result";

	private static final String ROUTE_DECISION = "route_decision";

	private static final String WORK_PLAN = "work_plan";

	private static final String IMPLEMENTATION_PROPOSAL = "implementation_proposal";

	private static final String REVIEW_DECISION = "review_decision";

	private static final String REVIEW_GATE_RESULT = "review_gate_result";

	private static final String AUDIT_REPORT = "audit_report";

	private static final String EVIDENCE_BUNDLE = "evidence_bundle";

	private static final String NODE_TRACE = "node_trace";

	private static final String ROUTE_HISTORY = "route_history";

	private static final List<String> GRAPH_NODE_IDS = List.of(
			"initialize_run",
			"read_issue",
			"agentscope_triager",
			"route_issue",
			"agentscope_bug_diagnoser",
			"agentscope_feature_planner",
			"agentscope_question_responder",
			"record_question_evidence",
			"agentscope_implementer",
			"agentscope_reviewer",
			"review_gate",
			"agentscope_auditor");

	private static final List<String> GRAPH_STATE_KEYS = List.of(
			GRAPH_ID,
			RUN_ID,
			CURRENT_NODE,
			ISSUE_FILE,
			ISSUE_URL,
			ISSUE_PAYLOAD,
			ISSUE_TITLE,
			ISSUE_TYPE,
			ISSUE_SOURCE,
			ISSUE_REPOSITORY,
			ISSUE_SUMMARY,
			REQUIRES_REVIEW,
			REQUIRES_DEPLOY,
			TRIAGE_RESULT,
			ROUTE_DECISION,
			WORK_PLAN,
			IMPLEMENTATION_PROPOSAL,
			REVIEW_DECISION,
			REVIEW_GATE_RESULT,
			AUDIT_REPORT,
			NODE_TRACE,
			ROUTE_HISTORY,
			EVIDENCE_BUNDLE);

	public static CompiledGraph createGraph(String dashScopeApiKey) throws GraphStateException {
		validateTopologyContract();

		Model model = createModel(dashScopeApiKey);
		AgentScopeAgent triager = createAgentScopeNode(model, "repoops_agentscope_triager",
				"RepoOps issue triage worker",
				"""
				You are the RepoOps intake and triage worker. Use only the supplied issue payload.
				Classify the issue, identify repository impact, priority, owner hint, required
				review gates, and missing context. Preserve source and repository fields.
				""",
				"""
				Issue payload:
				{issue_payload}

				Return a concise triage result. The first line must be exactly:
				route_candidate: bug|feature|question

				Then include:
				- type
				- priority
				- repository
				- source
				- owner_hint
				- required_review
				- required_deploy
				- missing_context
				""",
				TRIAGE_RESULT);
		AgentScopeAgent bugDiagnoser = createAgentScopeNode(model, "repoops_agentscope_bug_diagnoser",
				"RepoOps bug diagnosis worker",
				"""
				You are a RepoOps bug diagnosis worker. Use only the issue payload and triage
				result. Produce a realistic reproduction and diagnosis plan. Do not claim that
				tests or code were changed.
				""",
				"""
				Issue payload:
				{issue_payload}

				Triage:
				{triage_result}

				Return:
				1. reproduction plan
				2. likely fault boundary
				3. target files or modules to inspect
				4. regression test proposal
				5. unresolved evidence gaps
				""",
				WORK_PLAN);
		AgentScopeAgent featurePlanner = createAgentScopeNode(model, "repoops_agentscope_feature_planner",
				"RepoOps feature planning worker",
				"""
				You are a RepoOps feature planning worker. Use only the issue payload and triage
				result. Produce a design and implementation plan that can be reviewed.
				""",
				"""
				Issue payload:
				{issue_payload}

				Triage:
				{triage_result}

				Return:
				1. user story
				2. non-goals
				3. design sketch
				4. acceptance criteria
				5. review and deploy gates
				""",
				WORK_PLAN);
		AgentScopeAgent questionResponder = createAgentScopeNode(model, "repoops_agentscope_question_responder",
				"RepoOps question response worker",
				"""
				You are a RepoOps support response worker. Use only the issue payload and triage
				result. Answer the question and identify whether docs or runbooks should be
				updated.
				""",
				"""
				Issue payload:
				{issue_payload}

				Triage:
				{triage_result}

				Return:
				1. direct answer
				2. supporting rationale
				3. doc/runbook update candidate
				4. whether implementation is required
				""",
				WORK_PLAN);
		AgentScopeAgent implementer = createAgentScopeNode(model, "repoops_agentscope_implementer",
				"RepoOps implementation proposal worker",
				"""
				You are a RepoOps implementation worker. Produce a reviewable patch proposal from
				the issue payload and work plan. Do not invent actual diffs. Describe files,
				tests, commands, and rollout risk.
				""",
				"""
				Issue payload:
				{issue_payload}

				Work plan:
				{work_plan}

				Return:
				1. proposed changes
				2. tests to add or run
				3. commands to verify
				4. release/deploy impact
				5. risks
				""",
				IMPLEMENTATION_PROPOSAL);
		AgentScopeAgent reviewer = createAgentScopeNode(model, "repoops_agentscope_reviewer",
				"RepoOps reviewer worker",
				"""
				You are the independent RepoOps reviewer. Check whether the implementation proposal
				is supported by the issue and work plan. Start your answer with PASS when the
				proposal is reviewable and audit-ready. Start with FAIL only for concrete blocking
				gaps.
				""",
				"""
				Issue payload:
				{issue_payload}

				Triage:
				{triage_result}

				Work plan:
				{work_plan}

				Implementation proposal:
				{implementation_proposal}

				Return PASS or FAIL first, then:
				1. blocking findings
				2. required tests
				3. release/deploy notes
				4. audit evidence required
				""",
				REVIEW_DECISION);
		AgentScopeAgent auditor = createAgentScopeNode(model, "repoops_agentscope_auditor",
				"RepoOps evidence auditor worker",
				"""
				You are the RepoOps evidence auditor. Produce a final lifecycle report using only
				the issue payload and graph state. Preserve repository, source, issue type, review
				decision, and deploy requirement.
				""",
				"""
				Issue file:
				{issue_file}

				Issue payload:
				{issue_payload}

				Triage:
				{triage_result}

				Work plan:
				{work_plan}

				Implementation proposal:
				{implementation_proposal}

				Review decision:
				{review_decision}

				Return:
				1. lifecycle route
				2. repository/source summary
				3. review decision
				4. deploy readiness
				5. evidence appendix
				""",
				AUDIT_REPORT);

		StateGraph graph = new StateGraph("agentscope_repoops_issue", keyStrategyFactory())
				.addNode("initialize_run", node_async(state -> initializeRun()))
				.addNode("read_issue", node_async(state -> normalizeIssue(state.value(ISSUE_FILE, String.class)
						.orElse(DEFAULT_ISSUE_REF), state)))
				.addNode("agentscope_triager", agentScopeAction("agentscope_triager", "agentic_worker", triager,
						TRIAGE_RESULT))
				.addNode("route_issue", node_async(AgentScopeRepoOpsIssueGraphExample::routeIssue))
				.addNode("agentscope_bug_diagnoser", agentScopeAction("agentscope_bug_diagnoser", "agentic_worker",
						bugDiagnoser, WORK_PLAN))
				.addNode("agentscope_feature_planner", agentScopeAction("agentscope_feature_planner", "agentic_worker",
						featurePlanner, WORK_PLAN))
				.addNode("agentscope_question_responder", agentScopeAction("agentscope_question_responder",
						"agentic_worker", questionResponder, WORK_PLAN))
				.addNode("agentscope_implementer", agentScopeAction("agentscope_implementer", "agentic_worker",
						implementer, IMPLEMENTATION_PROPOSAL))
				.addNode("agentscope_reviewer", agentScopeAction("agentscope_reviewer", "verifier_agent", reviewer,
						REVIEW_DECISION))
				.addNode("review_gate", node_async(AgentScopeRepoOpsIssueGraphExample::reviewGate))
				.addNode("agentscope_auditor", agentScopeAction("agentscope_auditor", "audit_agent", auditor,
						AUDIT_REPORT))
				.addNode("record_question_evidence", node_async(state -> Map.of(
						CURRENT_NODE, "record_question_evidence",
						IMPLEMENTATION_PROPOSAL, "Not required: question issue was answered without implementation workflow.",
						REVIEW_DECISION, "PASS: question issue does not require implementation review.",
						REVIEW_GATE_RESULT, "pass",
						NODE_TRACE, List.of(nodeTrace(state, "record_question_evidence", "deterministic_evidence",
								List.of(WORK_PLAN), List.of(IMPLEMENTATION_PROPOSAL, REVIEW_DECISION))),
						EVIDENCE_BUNDLE, List.of(evidence(state, "record_question_evidence",
								"question issue answered without implementation workflow")))))
				.addEdge(START, "initialize_run")
				.addEdge("initialize_run", "read_issue")
				.addEdge("read_issue", "agentscope_triager")
				.addEdge("agentscope_triager", "route_issue")
				.addConditionalEdges("route_issue", edge_async(state -> state.value(ROUTE_DECISION, String.class)
						.orElse("question")), Map.of(
								"bug", "agentscope_bug_diagnoser",
								"feature", "agentscope_feature_planner",
								"question", "agentscope_question_responder"))
				.addEdge("agentscope_bug_diagnoser", "agentscope_implementer")
				.addEdge("agentscope_feature_planner", "agentscope_implementer")
				.addEdge("agentscope_question_responder", "record_question_evidence")
				.addEdge("agentscope_implementer", "agentscope_reviewer")
				.addEdge("agentscope_reviewer", "review_gate")
				.addConditionalEdges("review_gate", edge_async(state -> state.value(REVIEW_GATE_RESULT, String.class)
								.orElse("fail")),
						Map.of("pass", "agentscope_auditor", "fail", "agentscope_implementer"))
				.addEdge("record_question_evidence", "agentscope_auditor")
				.addEdge("agentscope_auditor", END);

		return graph.compile();
	}

	public static void main(String[] args) throws Exception {
		if (args.length > 0 && "--validate-topology".equals(args[0])) {
			validateTopologyContract();
			System.out.println("Topology contract is aligned with the Java graph constants.");
			return;
		}

		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalArgumentException("Set AI_DASHSCOPE_API_KEY before running this example.");
		}

		String issueRef = args.length > 0 ? args[0] : DEFAULT_ISSUE_REF;
		try {
			CompiledGraph graph = createGraph(apiKey);
			graph.stream(Map.of(ISSUE_FILE, issueRef))
					.doOnNext(System.out::println)
					.blockLast();
		}
		finally {
			ParallelNode.shutdownDefaultExecutor();
			Schedulers.shutdownNow();
		}
	}

	private static Model createModel(String dashScopeApiKey) {
		return DashScopeChatModel.builder()
				.apiKey(dashScopeApiKey)
				.modelName("qwen-plus")
				.build();
	}

	private static AgentScopeAgent createAgentScopeNode(Model model, String name, String description,
			String systemPrompt, String instruction, String outputKey) {
		ReActAgent.Builder builder = ReActAgent.builder()
				.name(name)
				.description(description)
				.sysPrompt(systemPrompt)
				.model(model)
				.memory(new InMemoryMemory());

		return AgentScopeAgent.fromBuilder(builder)
				.name(name)
				.description(description)
				.instruction(instruction)
				.outputKey(outputKey)
				.includeContents(false)
				.returnReasoningContents(false)
				.build();
	}

	private static AsyncNodeActionWithConfig agentScopeAction(String nodeId, String nodeType, AgentScopeAgent agent,
			String outputKey) {
		return AsyncNodeActionWithConfig.node_async((state, config) ->
				Map.of(
						CURRENT_NODE, nodeId,
						outputKey, Objects.requireNonNull(agent.call(state.data()).getText()),
						NODE_TRACE, List.of(nodeTrace(state, nodeId, nodeType, agentInputKeys(outputKey),
								List.of(outputKey))),
						EVIDENCE_BUNDLE, List.of(evidence(state, nodeId, "produced " + outputKey))));
	}

	private static Map<String, Object> initializeRun() {
		String runId = UUID.randomUUID().toString();
		return Map.of(
				GRAPH_ID, GRAPH_ID_VALUE,
				RUN_ID, runId,
				CURRENT_NODE, "initialize_run",
				NODE_TRACE, List.of(Map.of(
						"graph_id", GRAPH_ID_VALUE,
						"run_id", runId,
						"node_id", "initialize_run",
						"node_type", "deterministic_initializer",
						"output_keys", List.of(GRAPH_ID, RUN_ID),
						"timestamp", Instant.now().toString())),
				EVIDENCE_BUNDLE, List.of(Map.of(
						"graph_id", GRAPH_ID_VALUE,
						"run_id", runId,
						"node_id", "initialize_run",
						"summary", "created graph run identity")));
	}

	private static Map<String, Object> routeIssue(OverAllState state) {
		String fallbackRoute = sanitizeRoute(state.value(ISSUE_TYPE, String.class).orElse("question"));
		String triageResult = state.value(TRIAGE_RESULT, String.class).orElse("");
		String route = routeFromTriage(triageResult);
		String reason;
		if (route == null) {
			route = fallbackRoute;
			reason = "fallback issue_type=" + fallbackRoute;
		}
		else {
			reason = "triage_result route_candidate=" + route;
		}
		return Map.of(
				CURRENT_NODE, "route_issue",
				ROUTE_DECISION, route,
				ROUTE_HISTORY, List.of(routeEntry(state, "route_issue", route, reason)),
				NODE_TRACE, List.of(nodeTrace(state, "route_issue", "deterministic_router",
						List.of(TRIAGE_RESULT, ISSUE_TYPE), List.of(ROUTE_DECISION))),
				EVIDENCE_BUNDLE, List.of(evidence(state, "route_issue", "routed issue to " + route)));
	}

	private static String routeFromTriage(String triageResult) {
		Matcher matcher = ROUTE_CANDIDATE.matcher(triageResult);
		if (matcher.find()) {
			return sanitizeRoute(matcher.group(1));
		}
		return null;
	}

	private static String sanitizeRoute(String route) {
		route = route.toLowerCase();
		if (!List.of("bug", "feature", "question").contains(route)) {
			return "question";
		}
		return route;
	}

	private static Map<String, Object> reviewGate(OverAllState state) {
		String result = reviewRoute(state);
		return Map.of(
				CURRENT_NODE, "review_gate",
				REVIEW_GATE_RESULT, result,
				ROUTE_HISTORY, List.of(routeEntry(state, "review_gate", result,
						"review_decision starts with PASS=" + "pass".equals(result))),
				NODE_TRACE, List.of(nodeTrace(state, "review_gate", "deterministic_verifier_router",
						List.of(REVIEW_DECISION), List.of(REVIEW_GATE_RESULT))),
				EVIDENCE_BUNDLE, List.of(evidence(state, "review_gate", "review gate result " + result)));
	}

	private static Map<String, Object> normalizeIssue(String issueRef, OverAllState state)
			throws IOException, InterruptedException {
		if (issueRef.startsWith("https://github.com/")) {
			return normalizeGitHubIssue(issueRef, state);
		}
		if (issueRef.startsWith(CLASSPATH_PREFIX)) {
			return normalizeIssueResource(issueRef, state);
		}
		return normalizeIssueFile(Path.of(issueRef), state);
	}

	private static Map<String, Object> normalizeIssueFile(Path issueFile, OverAllState state) throws IOException {
		Map<String, Object> issue = OBJECT_MAPPER.readValue(Files.readString(issueFile),
				new TypeReference<>() {
				});
		return normalizeLocalIssue(issueFile.toString(), issue, state);
	}

	private static Map<String, Object> normalizeIssueResource(String issueRef, OverAllState state) throws IOException {
		String resourceName = issueRef.substring(CLASSPATH_PREFIX.length());
		try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
			if (inputStream == null) {
				throw new FileNotFoundException("Classpath issue resource not found: " + resourceName);
			}
			Map<String, Object> issue = OBJECT_MAPPER.readValue(inputStream, new TypeReference<>() {
			});
			return normalizeLocalIssue(issueRef, issue, state);
		}
	}

	private static Map<String, Object> normalizeLocalIssue(String issueRef, Map<String, Object> issue, OverAllState state)
			throws IOException {
		String type = String.valueOf(issue.getOrDefault("type", "question")).toLowerCase();
		if (!List.of("bug", "feature", "question").contains(type)) {
			type = "question";
		}
		Map<String, Object> normalized = new HashMap<>();
		normalized.put(CURRENT_NODE, "read_issue");
		normalized.put(ISSUE_FILE, issueRef);
		normalized.put(ISSUE_PAYLOAD, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(issue));
		normalized.put(ISSUE_TITLE, String.valueOf(issue.getOrDefault("title", "")));
		normalized.put(ISSUE_TYPE, type);
		normalized.put(ISSUE_SOURCE, String.valueOf(issue.getOrDefault("source", "")));
		normalized.put(ISSUE_REPOSITORY, String.valueOf(issue.getOrDefault("repository", "")));
		normalized.put(ISSUE_SUMMARY, String.valueOf(issue.getOrDefault("summary", "")));
		normalized.put(REQUIRES_REVIEW,
				Boolean.parseBoolean(String.valueOf(issue.getOrDefault("requires_review", "false"))));
		normalized.put(REQUIRES_DEPLOY,
				Boolean.parseBoolean(String.valueOf(issue.getOrDefault("requires_deploy", "false"))));
		normalized.put(NODE_TRACE, List.of(nodeTrace(state, "read_issue", "deterministic_intake",
				List.of(ISSUE_FILE), List.of(ISSUE_PAYLOAD, ISSUE_TYPE, ISSUE_REPOSITORY))));
		normalized.put(EVIDENCE_BUNDLE, List.of(evidence(state, "read_issue", "loaded " + issueRef)));
		return normalized;
	}

	private static Map<String, Object> normalizeGitHubIssue(String issueUrl, OverAllState graphState)
			throws IOException, InterruptedException {
		Matcher matcher = GITHUB_ISSUE_URL.matcher(issueUrl);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Only GitHub issue URLs are supported: " + issueUrl);
		}

		String owner = matcher.group(1);
		String repo = matcher.group(2);
		String number = matcher.group(3);
		String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/issues/" + number;
		HttpResponse<String> response = sendGitHubIssueRequest(apiUrl);
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("GitHub issue request failed: status=" + response.statusCode());
		}

		Map<String, Object> githubIssue = OBJECT_MAPPER.readValue(response.body(), new TypeReference<>() {
		});
		if (githubIssue.containsKey("pull_request")) {
			throw new IllegalArgumentException("Expected an issue URL, but this URL points to a pull request: " + issueUrl);
		}

		String title = String.valueOf(githubIssue.getOrDefault("title", ""));
		String body = String.valueOf(githubIssue.getOrDefault("body", ""));
		List<String> labels = extractLabelNames(githubIssue.get("labels"));
		String type = classifyIssue(title, labels);
		Map<String, Object> normalized = new HashMap<>();
		normalized.put("title", title);
		normalized.put("type", type);
		normalized.put("source", "github");
		normalized.put("repository", owner + "/" + repo);
		normalized.put("summary", firstNonBlank(body, title));
		normalized.put("requires_review", !"question".equals(type));
		normalized.put("requires_deploy", false);
		normalized.put("url", issueUrl);
		normalized.put("number", githubIssue.get("number"));
		normalized.put("state", githubIssue.get("state"));
		normalized.put("author", login(githubIssue.get("user")));
		normalized.put("created_at", githubIssue.get("created_at"));
		normalized.put("updated_at", githubIssue.get("updated_at"));
		normalized.put("labels", labels);
		normalized.put("body", body);

		Map<String, Object> output = new HashMap<>();
		output.put(CURRENT_NODE, "read_issue");
		output.put(ISSUE_FILE, issueUrl);
		output.put(ISSUE_URL, issueUrl);
		output.put(ISSUE_PAYLOAD, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(normalized));
		output.put(ISSUE_TITLE, title);
		output.put(ISSUE_TYPE, type);
		output.put(ISSUE_SOURCE, "github");
		output.put(ISSUE_REPOSITORY, owner + "/" + repo);
		output.put(ISSUE_SUMMARY, firstNonBlank(body, title));
		output.put(REQUIRES_REVIEW, !"question".equals(type));
		output.put(REQUIRES_DEPLOY, false);
		output.put(NODE_TRACE, List.of(nodeTrace(graphState, "read_issue", "deterministic_intake",
				List.of(ISSUE_FILE), List.of(ISSUE_PAYLOAD, ISSUE_TYPE, ISSUE_REPOSITORY))));
		output.put(EVIDENCE_BUNDLE, List.of(evidence(graphState, "read_issue", "fetched " + issueUrl)));
		return output;
	}

	private static HttpResponse<String> sendGitHubIssueRequest(String apiUrl) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(20))
				.version(HttpClient.Version.HTTP_1_1)
				.build();
		HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
				.timeout(Duration.ofSeconds(20))
				.header("Accept", "application/vnd.github+json")
				.header("User-Agent", "spring-ai-alibaba-graphengineering-example")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.GET()
				.build();
		try {
			return client.send(request, HttpResponse.BodyHandlers.ofString());
		}
		catch (IOException ex) {
			return client.send(request, HttpResponse.BodyHandlers.ofString());
		}
	}

	private static List<String> extractLabelNames(Object labelsValue) {
		if (!(labelsValue instanceof List<?> labels)) {
			return List.of();
		}
		List<String> names = new ArrayList<>();
		for (Object label : labels) {
			if (label instanceof Map<?, ?> labelMap && labelMap.get("name") != null) {
				names.add(String.valueOf(labelMap.get("name")));
			}
		}
		return names;
	}

	private static String classifyIssue(String title, List<String> labels) {
		String text = (title + " " + String.join(" ", labels)).toLowerCase();
		if (text.contains("bug") || text.contains("regression") || text.contains("fix")) {
			return "bug";
		}
		if (text.contains("feature") || text.contains("enhancement")) {
			return "feature";
		}
		return "question";
	}

	private static String firstNonBlank(String first, String fallback) {
		return StringUtils.hasText(first) ? first : fallback;
	}

	private static String login(Object userValue) {
		if (userValue instanceof Map<?, ?> user && user.get("login") != null) {
			return String.valueOf(user.get("login"));
		}
		return "";
	}

	private static KeyStrategyFactory keyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put(GRAPH_ID, new ReplaceStrategy());
			strategies.put(RUN_ID, new ReplaceStrategy());
			strategies.put(CURRENT_NODE, new ReplaceStrategy());
			strategies.put(ISSUE_FILE, new ReplaceStrategy());
			strategies.put(ISSUE_URL, new ReplaceStrategy());
			strategies.put(ISSUE_PAYLOAD, new ReplaceStrategy());
			strategies.put(ISSUE_TITLE, new ReplaceStrategy());
			strategies.put(ISSUE_TYPE, new ReplaceStrategy());
			strategies.put(ISSUE_SOURCE, new ReplaceStrategy());
			strategies.put(ISSUE_REPOSITORY, new ReplaceStrategy());
			strategies.put(ISSUE_SUMMARY, new ReplaceStrategy());
			strategies.put(REQUIRES_REVIEW, new ReplaceStrategy());
			strategies.put(REQUIRES_DEPLOY, new ReplaceStrategy());
			strategies.put(TRIAGE_RESULT, new ReplaceStrategy());
			strategies.put(ROUTE_DECISION, new ReplaceStrategy());
			strategies.put(WORK_PLAN, new ReplaceStrategy());
			strategies.put(IMPLEMENTATION_PROPOSAL, new ReplaceStrategy());
			strategies.put(REVIEW_DECISION, new ReplaceStrategy());
			strategies.put(REVIEW_GATE_RESULT, new ReplaceStrategy());
			strategies.put(AUDIT_REPORT, new ReplaceStrategy());
			strategies.put(EVIDENCE_BUNDLE, new AppendStrategy(false));
			strategies.put(NODE_TRACE, new AppendStrategy(false));
			strategies.put(ROUTE_HISTORY, new AppendStrategy(false));
			strategies.put("messages", new AppendStrategy(false));
			return strategies;
		};
	}

	private static void validateTopologyContract() {
		String topology = readClasspathText(TOPOLOGY_RESOURCE);
		requireContractContains(topology, "id: " + GRAPH_ID_VALUE, "graph id");
		for (String nodeId : GRAPH_NODE_IDS) {
			requireContractContains(topology, "id: " + nodeId, "node " + nodeId);
		}
		for (String stateKey : GRAPH_STATE_KEYS) {
			requireContractContains(topology, "- " + stateKey, "state key " + stateKey);
		}
	}

	private static String readClasspathText(String resourceName) {
		try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
			if (inputStream == null) {
				throw new IllegalStateException("Classpath resource not found: " + resourceName);
			}
			return new String(inputStream.readAllBytes());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to read classpath resource: " + resourceName, ex);
		}
	}

	private static void requireContractContains(String topology, String expected, String description) {
		if (!topology.contains(expected)) {
			throw new IllegalStateException("Topology contract missing " + description + ": " + expected);
		}
	}

	private static List<String> agentInputKeys(String outputKey) {
		if (TRIAGE_RESULT.equals(outputKey)) {
			return List.of(ISSUE_PAYLOAD);
		}
		if (WORK_PLAN.equals(outputKey)) {
			return List.of(ISSUE_PAYLOAD, TRIAGE_RESULT);
		}
		if (IMPLEMENTATION_PROPOSAL.equals(outputKey)) {
			return List.of(ISSUE_PAYLOAD, WORK_PLAN);
		}
		if (REVIEW_DECISION.equals(outputKey)) {
			return List.of(ISSUE_PAYLOAD, TRIAGE_RESULT, WORK_PLAN, IMPLEMENTATION_PROPOSAL);
		}
		if (AUDIT_REPORT.equals(outputKey)) {
			return List.of(ISSUE_PAYLOAD, TRIAGE_RESULT, WORK_PLAN, IMPLEMENTATION_PROPOSAL, REVIEW_DECISION,
					REVIEW_GATE_RESULT, ROUTE_HISTORY);
		}
		return List.of(ISSUE_PAYLOAD);
	}

	private static Map<String, Object> nodeTrace(OverAllState state, String nodeId, String nodeType,
			List<String> inputKeys, List<String> outputKeys) {
		return Map.of(
				"graph_id", state.value(GRAPH_ID, String.class).orElse(GRAPH_ID_VALUE),
				"run_id", state.value(RUN_ID, String.class).orElse("unknown"),
				"node_id", nodeId,
				"node_type", nodeType,
				"input_keys", inputKeys,
				"output_keys", outputKeys,
				"timestamp", Instant.now().toString());
	}

	private static Map<String, Object> routeEntry(OverAllState state, String nodeId, String route, String reason) {
		return Map.of(
				"graph_id", state.value(GRAPH_ID, String.class).orElse(GRAPH_ID_VALUE),
				"run_id", state.value(RUN_ID, String.class).orElse("unknown"),
				"node_id", nodeId,
				"route", route,
				"reason", reason,
				"timestamp", Instant.now().toString());
	}

	private static Map<String, Object> evidence(OverAllState state, String nodeId, String summary) {
		return Map.of(
				"graph_id", state.value(GRAPH_ID, String.class).orElse(GRAPH_ID_VALUE),
				"run_id", state.value(RUN_ID, String.class).orElse("unknown"),
				"node_id", nodeId,
				"summary", summary,
				"timestamp", Instant.now().toString());
	}

	private static String reviewRoute(OverAllState state) {
		String decision = state.value(REVIEW_DECISION, String.class).orElse("FAIL");
		return decision.toUpperCase().startsWith("PASS") ? "pass" : "fail";
	}

}
