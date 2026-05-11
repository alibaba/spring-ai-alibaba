# AgentScope Multi-Agent Example

This module implements the **multiple agent subgraphs handoffs** pattern (same logic as [handoffs-multiagent](../handoffs-multiagent)), with the support agent using **AgentScope** via `AgentScopeAgent`. Distinct sales and support agents exist as separate nodes in a StateGraph. Handoff tools navigate between agent nodes by updating `active_agent`, which the parent graph's conditional edges use for routing.

## Architecture

- **Separate agents as graph nodes**  
  Sales and support agents are separate nodes. Sales uses Spring AI ReactAgent; support uses AgentScope ReActAgent via AgentScopeAgent with Toolkit. The parent StateGraph routes between them based on `active_agent`.

- **Handoff tools**  
  - `TransferToSupportTool`: Sales agent (ReactAgent) uses it to hand off to support. Updates `active_agent`, uses `returnDirect=true` so the agent exits immediately.
  - `TransferToSalesTool`: Support agent (AgentScopeAgent) uses it to hand off to sales. Registered via AgentScope Toolkit; uses `ToolContextHelper.getStateForUpdate()` to update `active_agent`.

- **Conditional routing**  
  - `route_initial`: START → sales_agent or support_agent (default: sales).
  - `route_after_sales`: If `active_agent` is support_agent → support_agent; else → END.
  - `route_after_support`: If `active_agent` is sales_agent → sales_agent; else → END.

## Modifying state in AgentScope tools

AgentScope tools receive `ToolContext` (auto-injected) and can read/update graph state:

```java
@Tool(name = "transfer_to_sales", description = "...")
public String transferToSales(
        @ToolParam(name = "reason", description = "...") String reason,
        ToolContext toolContext) {
    // Update state: put keys into the map; merged when node completes
    ToolContextHelper.getStateForUpdate(toolContext).ifPresent(update ->
            update.put("active_agent", "sales_agent"));
    return "Transferred to sales agent.";
}
```

**Read state:**
```java
OverAllState state = ToolContextHelper.getState(toolContext).orElse(null);
```

**Update state:**  
Put keys into `getStateForUpdate(toolContext)`; the graph must declare those keys in its key strategies (e.g. `ReplaceStrategy` for `active_agent` or `extraState`).

See `UpdateExtraStateTool` for a full example that reads state and updates `extraState`.

## Design choices

1. **returnDirect on handoff tools (Spring AI)**  
   Sales agent's TransferToSupportTool uses `@Tool(returnDirect = true)` so the agent exits immediately after the tool.

2. **State update**  
   Tools use `ToolContextHelper.getStateForUpdate(toolContext)` to set `active_agent` (or other keys). The update is merged into the graph state when the agent node completes.

3. **AgentScope Toolkit**  
   Support agent uses `io.agentscope.core.tool.Toolkit` and `ReActAgent.builder().toolkit(toolkit)`. Tools use `io.agentscope.core.tool.Tool` and `@ToolParam`; `ToolContext` is auto-injected.

## Project layout

```
examples/multiagent-patterns/agentscope/
├── pom.xml
├── README.md
└── src/main/
    ├── java/.../agentscope/
    │   ├── AgentScopeApplication.java
    │   ├── AgentScopeHandoffsConfig.java   # StateGraph, agents, routing
    │   ├── AgentScopeHandoffsService.java  # invokes graph
    │   ├── AgentScopeHandoffsRunner.java   # optional demo runner
    │   ├── route/
    │   │   ├── RouteInitialAction.java     # START → sales or support
    │   │   ├── RouteAfterSalesAction.java  # sales → support or END
    │   │   └── RouteAfterSupportAction.java # support → sales or END
    │   ├── state/
    │   │   └── AgentScopeStateConstants.java
    │       └── tools/
    │       ├── TransferToSalesTool.java    # support → sales (AgentScope Toolkit)
    │       ├── TransferToSupportTool.java  # sales → support (Spring AI)
    │       └── UpdateExtraStateTool.java  # example: read state + update extraState
    └── resources/
        └── application.yml
```

## How to run

### Prerequisites

- JDK 17+
- Maven 3.6+
- **DashScope API key**: `export AI_DASHSCOPE_API_KEY=your-key`

### Build

**Note:** This example depends on `spring-ai-alibaba-agent-framework` which includes AgentScope integration. If building from source, install the framework first:

```bash
# From repo root - install agent-framework to local Maven repo
./mvnw -pl spring-ai-alibaba-agent-framework -am -B install -DskipTests
```

Then build the example:

```bash
cd examples/multiagent-patterns/agentscope
../../../../mvnw -B package -DskipTests
```

### Run the demo on startup

Set `agentscope.runner.enabled=true` (in `application.yml` or env), then start the app:

```bash
export AGENTSCOPE_RUNNER_ENABLED=true
java -jar target/agentscope-0.0.1-SNAPSHOT.jar
```

Or run without the demo (app starts and waits for your own calls):

```bash
./mvnw spring-boot:run
```

### Chat UI (recommended)

Start the app, then open the built-in chat UI in your browser:

```bash
./mvnw spring-boot:run
# Open http://localhost:8080/chatui/index.html
```

The UI lets you type messages and see Sales / Support agent responses and handoffs in real time.

### Using in your own code

```java
@Autowired
AgentScopeHandoffsService service;

var result = service.run("Hi, I'm having trouble with my account login. Can you help?");
result.messages().forEach(msg -> System.out.println(msg.getText()));
```

## Configuration

- **`spring.ai.dashscope.api-key`**  
  Required. Defaults to `AI_DASHSCOPE_API_KEY` env var.

- **`agentscope.runner.enabled`**  
  If `true`, runs the AgentScope multi-agent handoffs demo on startup. Default: `false`.

## Related

- [handoffs-multiagent](../handoffs-multiagent) - Same pattern with both agents as Spring AI ReactAgent
- [AgentScope Java](https://java.agentscope.io/) - AgentScope framework documentation
