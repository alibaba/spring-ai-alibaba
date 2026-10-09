# Choosing a Multi-Agent Pattern

This directory contains both single-agent and multi-agent ways to add specialized behavior, delegation, and explicit control flow. These patterns are not maturity levels: choose the smallest one that matches the product's ownership and execution model, and combine patterns only when their boundaries remain observable.

The descriptions below reflect what the examples in this directory implement. Follow each linked README for its architecture and run instructions.

## Decision matrix

| Product need | Start with | What this example implements | Main trade-off |
| --- | --- | --- | --- |
| One agent needs optional domain instructions or resources | [`skills`](./skills/) | One `ReactAgent` sees skill descriptions and loads a full `SKILL.md` on demand through `read_skill` | Skill selection and added context must be observable |
| A request may need one or several relevant specialists | [`routing`](./routing/src/main/java/com/alibaba/cloud/ai/examples/multiagents/routing/simple/README.md) | `LlmRoutingAgent` selects one or more agents, creates a targeted sub-query for each, runs selected agents in parallel, and merges their outputs | Routing and each selected specialist add model calls; multi-source results add a synthesis call |
| A primary agent delegates bounded tasks, including background work | [`subagent`](./subagent/) | `Task` and `TaskOutput` tools invoke agents defined in Java or Markdown and return their results to the orchestrator | Task context, completion, and result retrieval need explicit handling |
| One user-facing coordinator dynamically invokes named specialist agents | [`supervisor`](./supervisor/) | A `ReactAgent` calls specialist `ReactAgent`s through `AgentTool` callbacks and synthesizes their results | The supervisor can become a latency, cost, and reliability bottleneck |
| One agent should change behavior as a process advances | [`handoffs-singleagent`](./handoffs-singleagent/) | One `ReactAgent` switches prompts and tools according to `current_step`; tools update state and a checkpointer can preserve it across turns when the same `thread_id` is reused | State transitions and stable thread identity become part of correctness |
| Ownership should move between distinct specialists during a graph run | [`handoffs-multiagent`](./handoffs-multiagent/) | Sales and support agents are separate graph nodes; transfer tools update `active_agent` and return control to the parent graph | Every transfer must preserve the right state and context |
| The topology is known and fits a common composition primitive | [`pipeline`](./pipeline/) | Prebuilt `SequentialAgent`, `ParallelAgent`, and `LoopAgent` examples cover chains, fan-out/fan-in, and iteration | Less graph code, but less freedom than a custom graph |
| The application needs custom nodes, state, and edges | [`workflow`](./workflow/) | The RAG and SQL examples build explicit `StateGraph`s around application steps and `ReactAgent` nodes | Maximum control requires more state, edge, and recovery design |

The routing module also shows how to embed an `LlmRoutingAgent` inside a larger [`StateGraph`](./routing/src/main/java/com/alibaba/cloud/ai/examples/multiagents/routing/graph/README.md) with preprocessing and postprocessing.

## A practical selection flow

1. **Is the main problem optional expertise, not independent execution?** Keep one agent and use `skills` for progressive disclosure.
2. **Should one agent change roles across a stateful, multi-turn process?** Use the single-agent handoff example. Use multi-agent handoffs only when separate agents must actually take ownership.
3. **Is the execution topology chosen before the request starts?** Use a pipeline when sequential, parallel, or loop composition is enough; use a custom workflow when you need application-specific nodes, state, or edges.
4. **Should an LLM select the relevant subset of specialists near the start?** Use routing. In this implementation the subset may contain one or multiple agents.
5. **Should a primary agent decide what work to delegate while solving the task?** Use subagents for a generic task lifecycle and optional background execution. Use a supervisor for a small, stable set of named specialist tools, especially when later calls depend on earlier results.
6. **Should a different specialist continue the conversation as its active owner?** Use multi-agent handoffs rather than treating delegation or routing as an ownership transfer.

## Similar patterns that are easy to confuse

### Skills vs. agents

A skill adds instructions or resources to one agent through progressive disclosure. It does not create an independently executing agent. Choose another pattern only when work needs its own agent context, lifecycle, or ownership.

### Routing vs. handoff

Routing makes one routing decision per invocation. That decision may select one specialist or fan out to several specialists in parallel before their outputs are merged. A handoff changes which agent is active during a conversation and may transfer control again later. Classifying a ticket into the sources needed to answer it is routing; moving an ongoing sales conversation to support is a handoff.

### Subagent vs. supervisor

Both patterns keep the primary agent as the user-facing owner. The subagent example exposes a generic task lifecycle through `Task` and `TaskOutput`, including background execution and later result retrieval. The supervisor example exposes each specialist as its own named, synchronous `AgentTool`; the supervisor can sequence those calls and owns the final response.

### Pipeline vs. workflow

`pipeline` is not limited to a fixed sequence. It demonstrates three prebuilt `FlowAgent` topologies: sequential, parallel, and loop. Use those primitives when they match the required shape. `workflow` is the lower-level choice when the application needs a custom `StateGraph`, such as the explicit rewrite/retrieve/agent stages in the RAG example or the database nodes around the SQL agent. The current workflow examples use linear graph edges; conditional branches, graph-level loops, and checkpointing must be added when an application needs them.

### Single-agent vs. multi-agent handoff

The single-agent example is a state machine: an interceptor changes one agent's prompt and visible tools as `current_step` advances. The multi-agent example performs an actual ownership transfer between separate sales and support agent nodes during one parent-graph run. The shipped multi-agent service starts a fresh graph invocation for each `run()` call; persistent ownership across user turns requires adding a parent-graph checkpointer and reusing a stable thread identity.

## Patterns can be composed

Use one pattern to define each boundary. For example, an application can route a result into a custom workflow, a supervisor's specialist can load skills, or a workflow node can invoke a prebuilt flow agent. Keep routing decisions, task IDs, active ownership, and graph state distinct so a trace can explain why every agent ran.

## Production readiness checklist

Before promoting an example pattern into a product, define:

- **Ownership:** which component produces the final answer and which component may contact the user;
- **Contracts:** inputs, outputs, state keys, and error behavior for every agent and tool boundary;
- **Budgets:** maximum model calls, tool calls, turns or iterations, latency, and token cost;
- **Context policy:** what history each specialist receives and what sensitive data must be filtered;
- **Recovery:** timeouts, retries, fallbacks, idempotency, loop termination, and human escalation;
- **Observability:** route selections, task IDs, handoffs, agent and tool spans, state transitions, and per-stage latency;
- **Evaluation:** task success plus the pattern-specific checks below.

## Evaluate orchestration, not only the final answer

| Pattern | Add these checks to answer-quality evaluation |
| --- | --- |
| Skills | Skill selection precision and recall; unnecessary skill loads; context added per skill |
| Routing | Selected-set precision and recall; sub-query quality; fan-out latency; merge faithfulness; invalid or empty decisions |
| Subagent | Target and delegated-prompt quality; background completion; `TaskOutput` retrieval; duplicate or orphaned work |
| Supervisor | Specialist-tool selection and ordering; dependent-action success; aggregation errors; model calls and cost |
| Single-agent handoff | Valid step transitions; state retained across turns; correct prompt and tool set per step |
| Multi-agent handoff | Correct active agent; unnecessary transfers; context and state retained across transfers within a graph run |
| Pipeline | Sequential stage correctness; parallel branch completion and merge correctness; loop termination and iteration count |
| Workflow | Node and edge coverage; state invariants; custom-node failures; recovery paths configured by the application |

Use a fixed evaluation set with happy paths, ambiguous and out-of-scope requests, tool failures, timeouts, and multi-turn follow-ups. For routing, include requests that require multiple specialists; for pipelines, cover every topology used by the product.

## Build and run an example

Each pattern directory has its own standalone Maven project and is not part of the repository root Maven reactor. From the repository root, a compile-only check can target one project directly:

```bash
./mvnw -f examples/multiagent-patterns/supervisor/pom.xml -DskipTests clean compile
```

Then follow that project's README to configure `AI_DASHSCOPE_API_KEY` and run its scenario. Several examples use stub business tools, but live orchestration still calls a `ChatModel`.

These example projects currently have no module-level `src/test` suites. A successful Maven lifecycle therefore confirms dependency resolution and Java compilation, not routing, delegation, handoff, or model behavior. Validate those behaviors with deterministic tests for state and tool boundaries plus a live evaluation set for model decisions.
