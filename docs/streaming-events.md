# Streaming graph and agent events

`CompiledGraph.streamEvents` and `Agent.streamEvents` return a Reactor `Flux<RunEvent>`. Each subscription starts a new invocation. The default options deliver root lifecycle and model text events. Select the channels and nested scopes that the consumer needs.

```java
var run = RunnableConfig.builder().threadId("order-42").build();
Flux<RunEvent> events = agent.streamEvents("Check this order", run, StreamOptions.all());
```

Each event carries a run ID, a delivery sequence and a structured scope. Use the scope segment IDs to distinguish repeated or parallel calls to the same graph or agent. Use `modelCallId` to combine model deltas. A model completion carries the final assistant message and token usage. The root completion carries the final graph state. Failure and pause are separate root outcomes.

## Several views of one execution

Create an `eventRun` to consume several projections without executing the graph again. Register the live views before calling `start()`. The root outcome can be read repeatedly through `output()`. A subgraph or subagent view provides events and an outcome for that invocation, with access to the recent bounded event window. A projection that falls behind reports an error. Closing the handle cancels the execution.

```java
try (RunStream execution = agent.eventRun("Check this order", run, StreamOptions.all())) {
    execution.messages().subscribe(this::renderMessage, this::reportError);
    execution.subagents().subscribe(this::showSubagent, this::reportError);
    execution.start();
    RunEvent outcome = execution.output().block();
    showOutcome(outcome);
}
```

## Nested graphs and progress

Use the compiled-graph node overload taking input and output mapping functions for a resumable child. Its output function also receives individual committed child writes, so it must accept partial updates. Configure a saver for the parent and child when the child must resume from a checkpoint.

A node or tool can use `RunnableConfig.runChild` or `ToolContextHelper.runChild` to invoke a dynamically selected graph and receive its final `NodeOutput`. Each call gets its own child checkpoint identity. If a dynamic wrapper needs checkpoint-backed continuation, model that boundary as a resumable compiled-graph node.

Use `config.streamWriter()` in nodes and `ToolContextHelper.streamWriter(context)` in tools to publish custom progress. The writer reports when progress is disabled, the run has closed, or the buffer is full.

## Streaming tools

A `StreamingToolCallback` can emit `ToolStreamPart.Delta` values followed by one `ToolStreamPart.Completed` and successful publisher completion. The final value is returned to the model as the tool result. Input argument chunks in model events describe the model request. Output chunks in tool events describe the running tool.

Parallel tool deltas can interleave. Tool results are assembled in the model's call order. A tool that is cancelled or fails before the final result completes has no successful final value. For a parallel graph using `ANY_OF`, the live stream publishes the winning branch when it is selected and reports truncated candidate output if the configured buffer limit was reached.

## Studio Server-Sent Events

The typed event endpoints are `/run_sse/v2`, `/resume_sse/v2` and `/graph_run_sse/v2`. They accept the same request bodies as the corresponding original endpoints. The SSE event ID combines the run ID and delivery sequence. Select optional payloads with `reasoning=true`, `toolPayload=true` or repeated `stateKey` query parameters.

To resume an approval, submit its interruption ID to the resume endpoint with the same thread ID. Resuming starts a new run with a new run ID. Checkpoint recovery resumes graph execution. SSE event IDs identify delivered events within an invocation.
