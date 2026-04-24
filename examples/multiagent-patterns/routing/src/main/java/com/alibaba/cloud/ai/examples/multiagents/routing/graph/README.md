# Routing Graph Example

This example demonstrates using **LlmRoutingAgent as a node** in a StateGraph, with **preprocessing**, **postprocessing**, and a **dedicated merge node** inside the routing agent for result synthesis.

## Architecture

- **StateGraph flow**: `START → preprocess → routing (LlmRoutingAgent) → postprocess → END`
- **PreprocessNode**: Query enrichment, validation, metadata (traceId, timestamp). Outputs `messages` and `input` for the routing agent.
- **LlmRoutingAgent (as node)**: Internally: routing decision → parallel specialist agents → **merge node** → merged result. The merge node synthesizes sub-agent outputs via LLM.
- **PostprocessNode**: Formatting, logging, metadata. Produces `final_answer`.

## Design choices

1. **RoutingAgent as graph node**  
   The LlmRoutingAgent is added via `graph.addNode("routing", routerAgent.getAndCompileGraph())`. Its compiled graph runs as a subgraph, receiving state from preprocess and passing output to postprocess.

2. **Merge node inside RoutingAgent**  
   `RoutingGraphBuildingStrategy` now includes a dedicated `RoutingMergeNode` between sub-agents and exit. Sub-agents connect to the merge node; the merge node synthesizes results and writes `merged_result` to state.

3. **Pre/post processing**  
   Preprocess: validation, enrichment, trace metadata. Postprocess: formatted answer with metadata header and observability.

## Project layout

```
examples/multiagents/routing-graph/
├── README.md
├── pom.xml
└── src/main/
    ├── java/.../routinggraph/
    │   ├── RoutingGraphApplication.java
    │   ├── RoutingGraphConfig.java       # StateGraph, agents, routingGraph bean
    │   ├── RoutingGraphService.java      # invokes graph
    │   ├── RoutingGraphRunner.java      # optional demo runner
    │   └── node/
    │       ├── PreprocessNode.java      # query enrichment, validation, metadata
    │       └── PostprocessNode.java     # formatting, metadata
    ├── java/.../routing/
    │   └── tools/
    │       ├── GitHubStubTools.java
    │       ├── NotionStubTools.java
    │       └── SlackStubTools.java
    └── resources/
        └── application.yml
```

## How to run

### Prerequisites

- JDK 17+
- Maven 3.6+
- **DashScope API key** for the chat model.

Set it:

```bash
export AI_DASHSCOPE_API_KEY=your-dashscope-api-key
```

### Build

From the repo root:

```bash
./mvnw -pl :routing-graph -am -B package -DskipTests
```

Or from this directory:

```bash
cd examples/multiagents/routing-graph
mvn -B package -DskipTests
```

### Run the application

Default: the app starts **without** running the demo:

```bash
java -jar target/routing-graph-0.0.1-SNAPSHOT.jar
# or
./mvnw -pl :routing-graph spring-boot:run
```

To run the **demo** on startup:

```bash
export routing-graph.runner.enabled=true
# or in application.yml: routing-graph.runner.enabled: true
```

### Using the routing graph in your own code

Inject `RoutingGraphService` and call `run(query)`:

```java
@Autowired
RoutingGraphService routingGraphService;

var result = routingGraphService.run("How do I authenticate API requests?");
System.out.println("Final answer: " + result.finalAnswer());
```

## Configuration

- **`spring.ai.dashscope.api-key`**  
  Required. Defaults to `AI_DASHSCOPE_API_KEY` env var.

- **`routing-graph.runner.enabled`**  
  If `true`, runs the single-query demo on startup. Default: `false`.

## Example flow

1. User: "How do I authenticate API requests?"
2. **Preprocess**: Validates query, enriches, adds traceId and timestamp. Outputs `messages` and `input`.
3. **LlmRoutingAgent** (as node): LLM routes to e.g. GitHub and Notion. Sub-agents run in parallel. **Merge node** synthesizes results into `merged_result`.
4. **Postprocess**: Formats final answer with metadata header.
5. Result returned to user.

## References

- [Subgraph as CompiledGraph](../../documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphAsCompiledGraphExample.java) – agent as graph node.
- Spring AI Alibaba: LlmRoutingAgent, StateGraph, RoutingMergeNode, ReactAgent.
