# Routing (Classify → Parallel Agents → Synthesize) Example

This example implements the **routing** multi-agent pattern with Spring AI Alibaba using **LlmRoutingAgent**. The router classifies the user query, invokes the relevant specialist agents (GitHub, Notion, Slack) **in parallel**, and **synthesizes** their results into a single answer.

## Architecture

- **LlmRoutingAgent**  
  The core routing component. An LLM analyzes the query and routes to one or more specialist agents with targeted sub-queries. Each agent receives its sub-question via the `{agentName_input}` placeholder in its instruction. Selected agents run in parallel via the framework's graph-based parallel conditional edges.
- **Specialist agents**  
  - **GitHub agent**: Tools `search_code`, `search_issues`, `search_prs`; answers about code, API references, implementation details.  
  - **Notion agent**: Tools `search_notion`, `get_page`; answers about internal docs, processes, wikis.  
  - **Slack agent**: Tools `search_slack`, `get_thread`; answers from team discussions and threads.

- **RouterService**  
  Wraps `LlmRoutingAgent`: invokes it with the user query, collects outputs from state, and synthesizes them via a final LLM call into one coherent answer.

## Design choices

1. **LlmRoutingAgent**  
   The classify and parallel agent invocation are handled by `LlmRoutingAgent`, which uses the framework's built-in LLM-based routing with per-agent sub-queries. Sub-agent instructions use `{github_input}`, `{notion_input}`, `{slack_input}` placeholders to receive routed queries.

2. **State types**  
   `Classification` (source, query) and `AgentOutput` (source, result) are derived from the graph state. `RouterResult` holds query, classifications, results, and final answer.

3. **Stub tools (@Tool annotation)**  
   GitHub, Notion, and Slack tools use `@Tool` / `@ToolParam` annotations and are wired via `methodTools()`. Stubbed for demo; replace with real API calls in production.

4. **RouterService**  
   A thin wrapper that invokes `LlmRoutingAgent` and adds the synthesis step. Can also use `routerAgent.invoke(query)` directly for routing-only (no synthesis).

## Project layout

```
examples/multiagents/routing/
├── README.md
├── pom.xml
└── src/main/
    ├── java/.../routing/
    │   ├── RoutingApplication.java
    │   ├── RoutingConfig.java          # githubAgent, notionAgent, slackAgent, routerAgent (LlmRoutingAgent), routerService
    │   ├── RoutingRunner.java         # optional demo runner
    │   ├── RouterService.java          # invoke LlmRoutingAgent → synthesize
    │   ├── state/
    │   │   ├── Classification.java
    │   │   └── AgentOutput.java
    │   └── tools/
    │       ├── GitHubStubTools.java    # search_code, search_issues, search_prs
    │       ├── NotionStubTools.java    # search_notion, get_page
    │       └── SlackStubTools.java     # search_slack, get_thread
    └── resources/
        └── application.yml
```

## How to run

### Prerequisites

- JDK 17+
- Maven 3.6+
- **DashScope API key** for the chat model (classifier, specialist agents, and synthesizer all use it).

Set it:

```bash
export AI_DASHSCOPE_API_KEY=your-dashscope-api-key
```

### Build

From the repo root:

```bash
./mvnw -pl :routing -am -B package -DskipTests
```

Or from this directory:

```bash
cd examples/multiagents/routing
mvn -B package -DskipTests
```

### Run the application

Default: the app starts **without** running the demo:

```bash
java -jar target/routing-0.0.1-SNAPSHOT.jar
# or
./mvnw -pl :routing spring-boot:run
```

To run the **demo** on startup (one query: "How do I authenticate API requests?"):

Set:

```bash
export routing.runner.enabled=true
# or in application.yml: routing.runner.enabled: true
```

Then start the app. The runner will log classifications, then the synthesized final answer.

### Using the router in your own code

Inject `RouterService` and call `run(query)`:

```java
@Autowired
RouterService routerService;

RouterService.RouterResult result = routerService.run("How do I authenticate API requests?");
System.out.println("Classifications: " + result.classifications());
System.out.println("Final answer: " + result.finalAnswer());
```

You can also call `synthesize(query, results)` separately if you need to re-synthesize with different results.

## Configuration

- **`spring.ai.dashscope.api-key`**  
  Required. Defaults to `AI_DASHSCOPE_API_KEY` env var.

- **`routing.runner.enabled`**  
  If `true`, runs the single-query demo on startup. Default: `false`.

## Example flow

1. User: "How do I authenticate API requests?"
2. **LlmRoutingAgent**: LLM routes to e.g. GitHub and Notion with targeted sub-queries (`github_input`, `notion_input`).
3. **Parallel agents**: GitHub agent and Notion agent run in parallel with their sub-queries; each returns a short answer (from stub tools).
4. **Synthesize**: RouterService collects outputs and invokes LLM to combine results into one coherent answer (JWT, OAuth2, API keys, token refresh, etc.).
5. The combined answer is returned to the user.

This mirrors the reference: router classifies, parallel specialist agents run, and a synthesis step produces the final response.

