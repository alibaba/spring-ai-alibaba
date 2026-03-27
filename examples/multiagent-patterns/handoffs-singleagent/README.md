# Handoffs Single-Agent Example

This module implements the **handoffs (state machine)** pattern with a single agent. The agent’s behavior changes dynamically based on workflow state: **tools** update state variables (`current_step`, `warranty_status`, `issue_type`), and a **model interceptor** reads the current step to apply the right system prompt and tools.

## Architecture

- **Single agent, step-based configuration**  
  One ReactAgent runs the whole flow. Each “step” is a different configuration (system prompt + tool set) of the same agent, selected by a **ModelInterceptor** that reads `current_step` from the request context (graph state).

- **State-driven steps**  
  - `warranty_collector`: Ask if the device is under warranty; only tool: `record_warranty_status`.
  - `issue_classifier`: Ask for issue description and classify as hardware/software; only tool: `record_issue_type`.
  - `resolution_specialist`: Provide solution or escalate; tools: `provide_solution`, `escalate_to_human`.

- **Tools that update state**  
  `record_warranty_status` and `record_issue_type` write to the graph state via `ToolContextHelper.getStateForUpdate(toolContext)` and set `current_step` to the next step. The framework merges these updates into the graph state so the next model call sees the new step.

- **Checkpointer**  
  A `MemorySaver` is used so that state (and thus `current_step`, `warranty_status`, `issue_type`) persists across turns when you use the same `thread_id` in `RunnableConfig`.

## Design choices

1. **State keys**  
   `current_step`, `warranty_status`, `issue_type` are stored in the graph state. A **Hook** adds key strategies (ReplaceStrategy) for these keys so they merge correctly when tools return updates.

2. **Step-config interceptor**  
   `StepConfigInterceptor` runs before each model call. It reads `current_step` from the request context, looks up the step config, and overrides the system message and the list of tools so the model only sees the tools for that step.

3. **Tool responses**  
   State-updating tools return a plain string. The framework turns that into the tool response message; the state update is applied separately via the tool context update map.

## Project layout

```
examples/multiagent-patterns/handoffs-singleagent/
├── pom.xml
├── README.md
└── src/main/
    ├── java/.../handoffs/singleagent/
    │   ├── HandoffsApplication.java
    │   ├── HandoffsConfig.java           # supportAgent bean (tools, hook, saver)
    │   ├── HandoffsRunner.java           # optional 4-turn demo runner
    │   ├── support/
    │   │   ├── SupportStateConstants.java
    │   │   ├── StepConfigInterceptor.java   # step-based prompt + tools
    │   │   └── HandoffsSupportHook.java     # key strategies + interceptor
    │   └── tools/
    │       └── SupportTools.java         # record_warranty_status, record_issue_type, provide_solution, escalate_to_human
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
cd examples/multiagent-patterns/handoffs-singleagent
./mvnw -B package -DskipTests
```

### Run the application

Default: the app starts **without** running the demo:

```bash
java -jar target/handoffs-singleagent-0.0.1-SNAPSHOT.jar
# or
./mvnw spring-boot:run
```

### Run the four-turn demo on startup

Set `handoffs.run-examples=true`. The runner uses a fixed `thread_id` so the checkpointer keeps state across the four turns:

1. Turn 1: User says phone screen is cracked → agent asks about warranty.
2. Turn 2: User says still under warranty → agent asks to describe the issue.
3. Turn 3: User says screen physically cracked from drop → agent records hardware.
4. Turn 4: User asks what to do → agent gives warranty repair instructions.

```bash
# application.yml: handoffs.run-examples: true
# or
export HANDOFFS_RUN_EXAMPLES=true
java -jar target/handoffs-singleagent-0.0.1-SNAPSHOT.jar
```

### Using the support agent in your own code

Inject the agent and call it with a **stable thread_id** so state persists across turns:

```java
@Qualifier("supportAgent")
@Autowired
ReactAgent supportAgent;

RunnableConfig config = RunnableConfig.builder().threadId("my-support-session").build();

// Turn 1
AssistantMessage r1 = supportAgent.call(new UserMessage("Hi, my device is broken"), config);

// Turn 2 (same thread_id → state is loaded from checkpoint)
AssistantMessage r2 = supportAgent.call(new UserMessage("Yes, it's still under warranty"), config);
```

Without a checkpointer and without reusing `thread_id`, each call would start from a clean state and the step machine would not advance.

## Configuration

- **`spring.ai.dashscope.api-key`**  
  Required. Defaults to `AI_DASHSCOPE_API_KEY` env var.

- **`handoffs.run-examples`**  
  If `true`, runs the four-turn demo on startup. Default: `false`.
