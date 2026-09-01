# Choosing a Multi-Agent Pattern

This directory contains several ways to compose agents. The patterns solve different product problems; they are not maturity levels and a larger agent graph is not automatically better.

Use this guide to choose the smallest pattern that matches the control flow, then open the pattern's README for its architecture and run instructions.

## Decision matrix

| Product need | Start with | Why | Main trade-off |
| --- | --- | --- | --- |
| One agent needs optional instructions or domain resources | [`skills`](./skills/) | Loads specialized capabilities only when needed | Skill selection and context loading must be observable |
| A request should go to exactly one specialist or workflow | [`routing`](./routing/) | Classifies once, then dispatches to the best destination | A bad route can prevent the right specialist from seeing the task |
| A primary agent delegates bounded work and consumes the result | [`subagent`](./subagent/) | Keeps orchestration central while specialists run in focused contexts | Delegated context and return contracts need careful design |
| A central coordinator invokes several specialist agents as tools | [`supervisor`](./supervisor/) | Supports dynamic delegation while preserving one user-facing owner | More model calls, latency, and supervisor failure modes |
| The active specialist should transfer the conversation to another specialist | [`handoffs-singleagent`](./handoffs-singleagent/) or [`handoffs-multiagent`](./handoffs-multiagent/) | Makes ownership changes explicit and preserves conversational continuity | State and context must survive every transfer |
| Work always passes through a fixed sequence of stages | [`pipeline`](./pipeline/) | Simple, predictable, and easy to measure stage by stage | Poor fit for branches, loops, or dynamic delegation |
| The process has deterministic branches, loops, retrieval, or stateful checks | [`workflow`](./workflow/) | Represents business control flow explicitly in a graph | More state schema, edge, and recovery logic to maintain |

## A practical selection flow

1. **Can one agent and a clear tool contract solve the task?** Keep one agent. Add a `skill` when the main problem is loading optional expertise, not coordinating independent actors.
2. **Is there one correct destination for each request?** Use `routing`. Do not introduce a supervisor if no cross-specialist collaboration is required.
3. **Is the order known before the request starts?** Use a `pipeline` for a straight sequence or a `workflow` for conditions and loops.
4. **Must the model choose and combine specialists dynamically?** Use `subagent` for bounded delegation or `supervisor` when one coordinator may call multiple specialists.
5. **Should another specialist become the owner of the conversation?** Use a handoff. Choose the single-agent version when behavior changes by state; choose the multi-agent version when specialists need separate agents and graph nodes.

## Similar patterns that are easy to confuse

### Routing vs. handoff

Routing is usually a one-time dispatch near the start of a request. A handoff changes the active owner during execution and may transfer control again later. If users need to continue a support conversation after qualification by sales, that is a handoff; if every ticket is classified once into billing or technical support, routing is usually sufficient.

### Subagent vs. supervisor

Both expose specialist agents to an orchestrator. In the subagent pattern, delegation is typically a bounded unit of work whose result returns to the primary agent. A supervisor treats multiple agents as tools and may coordinate several of them dynamically. Start with subagents when tasks and return values can be described explicitly.

### Pipeline vs. workflow

A pipeline is a fixed sequence. A workflow makes control flow and state explicit, so it fits retries, branches, validation gates, and loops. Avoid encoding a deterministic approval process in model instructions when graph edges can enforce it.

## Production readiness checklist

Before promoting an example pattern into a product, define:

- **Ownership:** which component produces the final answer and which components may contact the user;
- **Contracts:** typed inputs, outputs, state keys, and error behavior for every agent/tool boundary;
- **Budgets:** maximum turns, model calls, tool calls, latency, and token/cost limits;
- **Context policy:** what history each specialist receives and what sensitive data must be removed;
- **Recovery:** timeout, retry, fallback, idempotency, and human-escalation behavior;
- **Observability:** route/handoff decisions, agent/tool spans, state transitions, and per-stage latency;
- **Evaluation:** task success plus pattern-specific failure metrics listed below.

## Evaluate the orchestration, not only the final answer

| Pattern | Add these checks to answer-quality evaluation |
| --- | --- |
| Skills | Skill selection precision/recall; context added per selected skill |
| Routing | Route accuracy; fallback rate; confidence on out-of-scope requests |
| Subagent / supervisor | Delegation accuracy; duplicate work; aggregation errors; calls and cost per task |
| Handoffs | Correct destination; unnecessary transfers; state retained after transfer |
| Pipeline | Per-stage success and latency; earliest failing stage |
| Workflow | Edge/branch coverage; loop termination; checkpoint and resume behavior |

Use a fixed evaluation set that includes happy paths, ambiguous requests, out-of-scope requests, tool failures, timeouts, and multi-turn follow-ups. A pattern is ready when its orchestration decisions are explainable and repeatable, not merely when a few final responses look good.

## Running an example

Each pattern is an independent Maven project. Follow the selected directory's README and run Maven from that project, for example:

```bash
./mvnw -f examples/multiagent-patterns/supervisor clean test
```

Most live examples require a configured model provider API key. Unit tests and stubbed tools should remain the first validation step so orchestration behavior can be checked without relying on a model's probabilistic output.
