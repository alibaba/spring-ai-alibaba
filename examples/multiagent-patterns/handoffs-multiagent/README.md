# Handoffs Multi-Agent Example

This module implements the **multiple agent subgraphs** handoffs pattern. Distinct sales and support agents exist as separate nodes in a StateGraph. Handoff tools navigate between agent nodes by updating `active_agent`, which the parent graph's conditional edges use for routing.

## Architecture

- **Separate agents as graph nodes**  
  Sales and support agents are separate nodes. Each invokes a ReactAgent (CompiledGraph). The parent StateGraph routes between them based on `active_agent`.

- **Handoff tools** (one class per tool for clarity)  
  - `TransferToSalesTool`: Support agent uses it to hand off to sales. Updates `active_agent`, uses `returnDirect=true` so the agent exits immediately.
  - `TransferToSupportTool`: Sales agent uses it to hand off to support.

- **Conditional routing**  
  - `route_initial`: START → sales_agent or support_agent (default: sales).
  - `route_after_sales`: If `active_agent` is support_agent → support_agent; else → END.
  - `route_after_support`: If `active_agent` is sales_agent → sales_agent; else → END.

## Design choices

1. **returnDirect on handoff tools**  
   Handoff tools use `@Tool(returnDirect = true)` so the agent exits immediately after the tool. No model response is generated. The parent graph's conditional edge then routes to the target agent.

2. **State update**  
   Tools use `ToolContextHelper.getStateForUpdate(toolContext)` to set `active_agent`. The update is merged into the graph state when the agent node completes.

3. **Context**  
   The full message history flows to the next agent. For production, consider summarizing or filtering context for token efficiency.

**Studio:** The two agents (`sales_agent`, `support_agent`) are Spring beans; Studio discovers them automatically via context scanning. No custom `AgentLoader` is required.

## Project layout

```
examples/multiagent-patterns/handoffs-multiagent/
├── pom.xml
├── README.md
└── src/main/
    ├── java/.../handoffs/
    │   ├── HandoffsApplication.java
    │   ├── MultiAgentHandoffsConfig.java   # StateGraph, agents, routing
    │   ├── MultiAgentHandoffsService.java  # invokes graph
    │   ├── MultiAgentHandoffsRunner.java   # optional demo runner
    │   ├── route/
    │   │   ├── RouteInitialAction.java     # START → sales or support
    │   │   ├── RouteAfterSalesAction.java  # sales → support or END
    │   │   └── RouteAfterSupportAction.java # support → sales or END
    │   ├── state/
    │   │   └── MultiAgentStateConstants.java
    │   └── tools/
    │       ├── TransferToSalesTool.java    # support → sales handoff
    │       └── TransferToSupportTool.java  # sales → support handoff
    └── resources/
        └── application.yml
```

## How to run

### Prerequisites

- JDK 17+
- Maven 3.6+
- **DashScope API key**: `export AI_DASHSCOPE_API_KEY=your-key`

### Build

From the repo root or module directory:

```bash
cd examples/multiagent-patterns/handoffs-multiagent
./mvnw -B package -DskipTests
```

### Run the demo on startup

Set `handoffs.runner.enabled=true` (in `application.yml` or env), then start the app. The runner sends a support-style query; the sales agent (default) may hand off to support.

```bash
# application.yml: handoffs.runner.enabled: true
# or
export HANDOFFS_RUNNER_ENABLED=true
java -jar target/handoffs-multiagent-0.0.1-SNAPSHOT.jar
```

Or run without the demo (app starts and waits for your own calls):

```bash
./mvnw spring-boot:run
```

### Chat UI (recommended)

Start the app (without the runner), then open the built-in chat UI in your browser:

```bash
./mvnw spring-boot:run
# Open http://localhost:8080/chatui/index.html
```

The UI lets you type messages and see Sales / Support agent responses and handoffs in real time.

### Using in your own code

```java
@Autowired
MultiAgentHandoffsService service;

var result = service.run("Hi, I'm having trouble with my account login. Can you help?");
result.messages().forEach(msg -> System.out.println(msg.getText()));
```

## Configuration

- **`spring.ai.dashscope.api-key`**  
  Required. Defaults to `AI_DASHSCOPE_API_KEY` env var.

- **`handoffs.runner.enabled`**  
  If `true`, runs the multi-agent handoffs demo on startup. Default: `false`.
