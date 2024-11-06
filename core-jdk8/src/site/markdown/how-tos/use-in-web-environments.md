# How to use LangGraph.js in web environments

LangGraph.js uses the [`async_hooks`](https://nodejs.org/api/async_hooks.html)
API to more conveniently allow for tracing and callback propagation within
nodes. This API is supported in many environments, such as
[Node.js](https://nodejs.org/api/async_hooks.html),
[Deno](https://deno.land/std@0.177.0/node/internal/async_hooks.ts),
[Cloudflare Workers](https://developers.cloudflare.com/workers/runtime-apis/nodejs/asynclocalstorage/),
and the
[Edge runtime](https://vercel.com/docs/functions/runtimes/edge-runtime#compatible-node.js-modules),
but not all, such as within web browsers.

To allow usage of LangGraph.js in environments that do not have the
`async_hooks` API available, we've added a separate `@langchain/langgraph/web`
entrypoint. This entrypoint exports everything that the primary
`@langchain/langgraph` exports, but will not initialize or even import
`async_hooks`. Here's a simple example:


```typescript
// Import from "@langchain/langgraph/web"
import {
  END,
  START,
  StateGraph,
  Annotation,
} from "@langchain/langgraph/web";
import { BaseMessage, HumanMessage } from "@langchain/core/messages";

const GraphState = Annotation.Root({
  messages: Annotation<BaseMessage[]>({
    reducer: (x, y) => x.concat(y),
  }),
});

const nodeFn = async (_state: typeof GraphState.State) => {
  return { messages: [new HumanMessage("Hello from the browser!")] };
};

// Define a new graph
const workflow = new StateGraph(GraphState)
  .addNode("node", nodeFn)
  .addEdge(START, "node")
  .addEdge("node", END);

const app = workflow.compile({});

// Use the Runnable
const finalState = await app.invoke(
  { messages: [] },
);

console.log(finalState.messages[finalState.messages.length - 1].content);
```

    Hello from the browser!


Other entrypoints, such as `@langchain/langgraph/prebuilt`, can be used in
either environment.

<div class="admonition warning">
  <p class="admonition-title">Caution</p>
  <p>
    If you are using LangGraph.js on the frontend, make sure you are not exposing any private keys!
    For chat models, this means you need to use something like <a href="https://js.langchain.com/v0.2/docs/integrations/chat/web_llm">WebLLM</a>
    that can run client-side without authentication.
  </p>
</div>

## Passing config

The lack of `async_hooks` support in web browsers means that if you are calling
a [`Runnable`](https://js.langchain.com/v0.2/docs/concepts#interface) within a
node (for example, when calling a chat model), you need to manually pass a
`config` object through to properly support tracing,
[`.streamEvents()`](https://js.langchain.com/v0.2/docs/how_to/streaming#using-stream-events)
to stream intermediate steps, and other callback related functionality. This
config object will passed in as the second argument of each node, and should be
used as the second parameter of any `Runnable` method.

To illustrate this, let's set up our graph again as before, but with a
`Runnable` within our node. First, we'll avoid passing `config` through into the
nested function, then try to use `.streamEvents()` to see the intermediate
results of the nested function:


```typescript
// Import from "@langchain/langgraph/web"
import {
  END,
  START,
  StateGraph,
  Annotation,
} from "@langchain/langgraph/web";
import { BaseMessage } from "@langchain/core/messages";
import { RunnableLambda } from "@langchain/core/runnables";
import { type StreamEvent } from "@langchain/core/tracers/log_stream";

const GraphState2 = Annotation.Root({
  messages: Annotation<BaseMessage[]>({
    reducer: (x, y) => x.concat(y),
  }),
});

const nodeFn2 = async (_state: typeof GraphState2.State) => {
  // Note that we do not pass any `config` through here
  const nestedFn = RunnableLambda.from(async (input: string) => {
    return new HumanMessage(`Hello from ${input}!`);
  }).withConfig({ runName: "nested" });
  const responseMessage = await nestedFn.invoke("a nested function");
  return { messages: [responseMessage] };
};

// Define a new graph
const workflow2 = new StateGraph(GraphState2)
  .addNode("node", nodeFn2)
  .addEdge(START, "node")
  .addEdge("node", END);

const app2 = workflow2.compile({});

// Stream intermediate steps from the graph
const eventStream2 = app2.streamEvents(
  { messages: [] },
  { version: "v2" },
  { includeNames: ["nested"] },
);

const events2: StreamEvent[] = [];
for await (const event of eventStream2) {
  console.log(event);
  events2.push(event);
}

console.log(`Received ${events2.length} events from the nested function`);
```

    Received 0 events from the nested function


We can see that we get no events.

Next, let's try redeclaring the graph with a node that passes config through
correctly:


```typescript
// Import from "@langchain/langgraph/web"
import {
  END,
  START,
  StateGraph,
  Annotation,
} from "@langchain/langgraph/web";
import { BaseMessage } from "@langchain/core/messages";
import { type RunnableConfig, RunnableLambda } from "@langchain/core/runnables";
import { type StreamEvent } from "@langchain/core/tracers/log_stream";

const GraphState3 = Annotation.Root({
  messages: Annotation<BaseMessage[]>({
    reducer: (x, y) => x.concat(y),
  }),
});

// Note the second argument here.
const nodeFn3 = async (_state: typeof GraphState3.State, config?: RunnableConfig) => {
  // If you need to nest deeper, remember to pass `_config` when invoking
  const nestedFn = RunnableLambda.from(
    async (input: string, _config?: RunnableConfig) => {
      return new HumanMessage(`Hello from ${input}!`);
    },
  ).withConfig({ runName: "nested" });
  const responseMessage = await nestedFn.invoke("a nested function", config);
  return { messages: [responseMessage] };
};

// Define a new graph
const workflow3 = new StateGraph(GraphState3)
  .addNode("node", nodeFn3)
  .addEdge(START, "node")
  .addEdge("node", END);

const app3 = workflow3.compile({});

// Stream intermediate steps from the graph
const eventStream3 = app3.streamEvents(
  { messages: [] },
  { version: "v2" },
  { includeNames: ["nested"] },
);

const events3: StreamEvent[] = [];
for await (const event of eventStream3) {
  console.log(event);
  events3.push(event);
}

console.log(`Received ${events3.length} events from the nested function`);
```

    {
      event: "on_chain_start",
      data: { input: { messages: [] } },
      name: "nested",
      tags: [],
      run_id: "22747451-a2fa-447b-b62f-9da19a539b2f",
      metadata: {
        langgraph_step: 1,
        langgraph_node: "node",
        langgraph_triggers: [ "start:node" ],
        langgraph_task_idx: 0,
        __pregel_resuming: false,
        checkpoint_id: "1ef62793-f065-6840-fffe-cdfb4cbb1248",
        checkpoint_ns: "node"
      }
    }
    {
      event: "on_chain_end",
      data: {
        output: HumanMessage {
          "content": "Hello from a nested function!",
          "additional_kwargs": {},
          "response_metadata": {}
        }
      },
      run_id: "22747451-a2fa-447b-b62f-9da19a539b2f",
      name: "nested",
      tags: [],
      metadata: {
        langgraph_step: 1,
        langgraph_node: "node",
        langgraph_triggers: [ "start:node" ],
        langgraph_task_idx: 0,
        __pregel_resuming: false,
        checkpoint_id: "1ef62793-f065-6840-fffe-cdfb4cbb1248",
        checkpoint_ns: "node"
      }
    }
    Received 2 events from the nested function


You can see that we get events from the nested function as expected.

## Next steps

You've now learned about some special considerations around using LangGraph.js
in web environments.

Next, check out
[some how-to guides on core functionality](/langgraphjs/how-tos/#core).
