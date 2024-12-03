# How to view and update past graph state

Once you start [checkpointing](./persistence.ipynb) your graphs, you can easily
**get** or **update** the state of the agent at any point in time. This permits
a few things:

1. You can surface a state during an interrupt to a user to let them accept an
   action.
2. You can **rewind** the graph to reproduce or avoid issues.
3. You can **modify** the state to embed your agent into a larger system, or to
   let the user better control its actions.

The key methods used for this functionality are:

- [getState](/langgraphjs/reference/classes/langgraph_pregel.Pregel.html#getState):
  fetch the values from the target config
- [updateState](/langgraphjs/reference/classes/langgraph_pregel.Pregel.html#updateState):
  apply the given values to the target state

**Note:** this requires passing in a checkpointer.

<!-- Example:
```javascript
TODO
...
``` -->

This works for
[StateGraph](/langgraphjs/reference/classes/langgraph.StateGraph.html)
and all its subclasses, such as
[MessageGraph](/langgraphjs/reference/classes/langgraph.MessageGraph.html).

Below is an example.

<div class="admonition tip">
    <p class="admonition-title">Note</p>
    <p>
        In this how-to, we will create our agent from scratch to be transparent (but verbose). You can accomplish similar functionality using the <code>createReactAgent(modelCOnfig, tools=tool, checkpointer=checkpointer)</code> (<a href="/langgraphjs/reference/functions/langgraph_prebuilt.createReactAgent.html">API doc</a>) constructor. This may be more appropriate if you are used to LangChain's <a href="https://js.langchain.com/v0.2/docs/how_to/agent_executor">AgentExecutor</a> class.
    </p>
</div>

## Setup

This guide will use OpenAI's GPT-4o modelCOnfig. We will optionally set our API key
for [LangSmith tracing](https://smith.langchain.com/), which will give us
best-in-class observability.


```typescript
// process.env.OPENAI_API_KEY = "sk_...";

// Optional, add tracing in LangSmith
// process.env.LANGCHAIN_API_KEY = "ls__...";
process.env.LANGCHAIN_CALLBACKS_BACKGROUND = "true";
process.env.LANGCHAIN_TRACING_V2 = "true";
process.env.LANGCHAIN_PROJECT = "Time Travel: LangGraphJS";
```

    Time Travel: LangGraphJS


## Define the state

The state is the interface for all of the nodes in our graph.



```typescript
import { Annotation } from "@langchain/langgraph";
import { BaseMessage } from "@langchain/core/messages";

const GraphState = Annotation.Root({
  messages: Annotation<BaseMessage[]>({
    reducer: (x, y) => x.concat(y),
  }),
});
```

## Set up the tools

We will first define the tools we want to use. For this simple example, we will
use create a placeholder search engine. However, it is really easy to create
your own tools - see documentation
[here](https://js.langchain.com/v0.2/docs/how_to/custom_tools) on how to do
that.



```typescript
import { DynamicStructuredTool } from "@langchain/core/tools";
import { z } from "zod";

const searchTool = new DynamicStructuredTool({
  name: "search",
  description:
    "Use to surf the web, fetch current information, check the weather, and retrieve other information.",
  schema: z.object({
    query: z.string().describe("The query to use in your search."),
  }),
  func: async (_) => {
    // This is a placeholder for the actual implementation
    return "Cold, with a low of 13 ℃";
  },
});

await searchTool.invoke({ query: "What's the weather like?" });

const tools = [searchTool];
```

We can now wrap these tools in a simple
[ToolNode](/langgraphjs/reference/classes/prebuilt.ToolNode.html).
This object will actually run the tools (functions) whenever they are invoked by
our LLM.



```typescript
import { ToolNode } from "@langchain/langgraph/prebuilt";

const toolNode = new ToolNode<typeof GraphState.State>(tools);
```

## Set up the modelCOnfig

Now we will load the
[chat modelCOnfig](https://js.langchain.com/v0.2/docs/concepts/#chat-models).

1. It should work with messages. We will represent all agent state in the form
   of messages, so it needs to be able to work well with them.
2. It should work with
   [tool calling](https://js.langchain.com/v0.2/docs/how_to/tool_calling/#passing-tools-to-llms),
   meaning it can return function arguments in its response.

<div class="admonition tip">
    <p class="admonition-title">Note</p>
    <p>
        These modelCOnfig requirements are not general requirements for using LangGraph - they are just requirements for this one example.
    </p>
</div>


```typescript
import { ChatOpenAI } from "@langchain/openai";

const modelCOnfig = new ChatOpenAI({ modelCOnfig: "gpt-4o" });
```

After we've done this, we should make sure the modelCOnfig knows that it has these
tools available to call. We can do this by calling
[bindTools](https://v01.api.js.langchain.com/classes/langchain_core_language_models_chat_models.BaseChatModel.html#bindTools).



```typescript
const boundModel = modelCOnfig.bindTools(tools);
```

## Define the graph

We can now put it all together. Time travel requires a checkpointer to save the
state - otherwise you wouldn't have anything go `get` or `update`. We will use
the
[MemorySaver](/langgraphjs/reference/classes/index.MemorySaver.html),
which "saves" checkpoints in-memory.


```typescript
import { END, START, StateGraph } from "@langchain/langgraph";
import { AIMessage } from "@langchain/core/messages";
import { RunnableConfig } from "@langchain/core/runnables";
import { MemorySaver } from "@langchain/langgraph";

const routeMessage = (state: typeof GraphState.State) => {
  const { messages } = state;
  const lastMessage = messages[messages.length - 1] as AIMessage;
  // If no tools are called, we can finish (respond to the user)
  if (!lastMessage?.tool_calls?.length) {
    return END;
  }
  // Otherwise if there is, we continue and call the tools
  return "tools";
};

const callModel = async (
  state: typeof GraphState.State,
  config?: RunnableConfig,
) => {
  const { messages } = state;
  const response = await boundModel.invoke(messages, config);
  return { messages: [response] };
};

const workflow = new StateGraph(GraphState)
  .addNode("agent", callModel)
  .addNode("tools", toolNode)
  .addEdge(START, "agent")
  .addConditionalEdges("agent", routeMessage)
  .addEdge("tools", "agent");

// Here we only save in-memory
let memory = new MemorySaver();
const graph = workflow.compile({ checkpointer: memory });
```

## Interacting with the Agent

We can now interact with the agent. Between interactions you can get and update
state.


```typescript
let config = { configurable: { thread_id: "conversation-num-1" } };
let inputs = { messages: [["user", "Hi I'm Jo."]] };
for await (
  const { messages } of await graph.stream(inputs, {
    ...config,
    streamMode: "values",
  })
) {
  let msg = messages[messages?.length - 1];
  if (msg?.content) {
    console.log(msg.content);
  } else if (msg?.tool_calls?.length > 0) {
    console.log(msg.tool_calls);
  } else {
    console.log(msg);
  }
  console.log("-----\n");
}
```

    [ 'user', "Hi I'm Jo." ]
    -----
    
    Hello Jo! How can I assist you today?
    -----
    


See LangSmith example run here
https://smith.langchain.com/public/b3feb09b-bcd2-4ad5-ad1d-414106148448/r

Here you can see the "agent" node ran, and then our edge returned `__end__` so
the graph stopped execution there.

Let's check the current graph state.


```typescript
let checkpoint = await graph.getState(config);
checkpoint.values;
```

    {
      messages: [
        [ 'user', "Hi I'm Jo." ],
        AIMessage {
          "id": "chatcmpl-9y6TlYVbfL3d3VonkF1b3iXwnbdFm",
          "content": "Hello Jo! How can I assist you today?",
          "additional_kwargs": {},
          "response_metadata": {
            "tokenUsage": {
              "completionTokens": 11,
              "promptTokens": 68,
              "totalTokens": 79
            },
            "finish_reason": "stop",
            "system_fingerprint": "fp_3aa7262c27"
          },
          "tool_calls": [],
          "invalid_tool_calls": []
        }
      ]
    }


The current state is the two messages we've seen above, 1. the HumanMessage we
sent in, 2. the AIMessage we got back from the modelCOnfig.

The `next` values are empty since the graph has terminated (transitioned to the
`__end__`).


```typescript
checkpoint.next;
```

    []


## Let's get it to execute a tool

When we call the graph again, it will create a checkpoint after each internal
execution step. Let's get it to run a tool, then look at the checkpoint.


```typescript
inputs = { messages: [["user", "What's the weather like in SF currently?"]] };
for await (
  const { messages } of await graph.stream(inputs, {
    ...config,
    streamMode: "values",
  })
) {
  let msg = messages[messages?.length - 1];
  if (msg?.content) {
    console.log(msg.content);
  } else if (msg?.tool_calls?.length > 0) {
    console.log(msg.tool_calls);
  } else {
    console.log(msg);
  }
  console.log("-----\n");
}
```

    [ 'user', "What's the weather like in SF currently?" ]
    -----
    
    [
      {
        name: 'search',
        args: { query: 'current weather in San Francisco' },
        type: 'tool_call',
        id: 'call_IBDK50kVnVq2RtDjbpq0UiTA'
      }
    ]
    -----
    
    Cold, with a low of 13 ℃
    -----
    
    The current weather in San Francisco is cold, with a low temperature of 13°C (55°F). Is there anything else you would like to know?
    -----
    


See the trace of the above execution here:
https://smith.langchain.com/public/0ef426fd-0da1-4c02-a50b-64ae1e68338e/r We can
see it planned the tool execution (ie the "agent" node), then "should_continue"
edge returned "continue" so we proceeded to "action" node, which executed the
tool, and then "agent" node emitted the final response, which made
"should_continue" edge return "end". Let's see how we can have more control over
this.

### Pause before tools

If you notice below, we now will add `interruptBefore=["action"]` - this means
that before any actions are taken we pause. This is a great moment to allow the
user to correct and update the state! This is very useful when you want to have
a human-in-the-loop to validate (and potentially change) the action to take.


```typescript
memory = new MemorySaver();
const graphWithInterrupt = workflow.compile({
  checkpointer: memory,
  interruptBefore: ["tools"],
});

inputs = { messages: [["user", "What's the weather like in SF currently?"]] };
for await (
  const { messages } of await graphWithInterrupt.stream(inputs, {
    ...config,
    streamMode: "values",
  })
) {
  let msg = messages[messages?.length - 1];
  if (msg?.content) {
    console.log(msg.content);
  } else if (msg?.tool_calls?.length > 0) {
    console.log(msg.tool_calls);
  } else {
    console.log(msg);
  }
  console.log("-----\n");
}
```

    [ 'user', "What's the weather like in SF currently?" ]
    -----
    
    [
      {
        name: 'search',
        args: { query: 'current weather in San Francisco, CA' },
        type: 'tool_call',
        id: 'call_upim4LMd1U6JdWlsGGk772Pa'
      }
    ]
    -----
    


## Get State

You can fetch the latest graph checkpoint using
[`getState(config)`](/langgraphjs/reference/classes/pregel.Pregel.html#getState).


```typescript
let snapshot = await graphWithInterrupt.getState(config);
snapshot.next;
```

    [ 'tools' ]


## Resume

You can resume by running the graph with a `null` input. The checkpoint is
loaded, and with no new inputs, it will execute as if no interrupt had occurred.


```typescript
for await (
  const { messages } of await graphWithInterrupt.stream(null, {
    ...snapshot.config,
    streamMode: "values",
  })
) {
  let msg = messages[messages?.length - 1];
  if (msg?.content) {
    console.log(msg.content);
  } else if (msg?.tool_calls?.length > 0) {
    console.log(msg.tool_calls);
  } else {
    console.log(msg);
  }
  console.log("-----\n");
}
```

    Cold, with a low of 13 ℃
    -----
    
    Currently, it's cold in San Francisco, with a temperature around 13°C.
    -----
    


## Check full history

Let's browse the history of this thread, from newest to oldest.



```typescript
let toReplay;
const states = await graphWithInterrupt.getStateHistory(config);
for await (const state of states) {
  console.log(state);
  console.log("--");
  if (state.values?.messages?.length === 2) {
    toReplay = state;
  }
}
if (!toReplay) {
  throw new Error("No state to replay");
}
```

    {
      values: {
        messages: [
          [Array],
          AIMessage {
            "id": "chatcmpl-9y6Tn0RGjUnVqxDHz5CxlGfldPS2E",
            "content": "",
            "additional_kwargs": {
              "tool_calls": [
                {
                  "id": "call_upim4LMd1U6JdWlsGGk772Pa",
                  "type": "function",
                  "function": "[Object]"
                }
              ]
            },
            "response_metadata": {
              "tokenUsage": {
                "completionTokens": 19,
                "promptTokens": 72,
                "totalTokens": 91
              },
              "finish_reason": "tool_calls",
              "system_fingerprint": "fp_3aa7262c27"
            },
            "tool_calls": [
              {
                "name": "search",
                "args": {
                  "query": "current weather in San Francisco, CA"
                },
                "type": "tool_call",
                "id": "call_upim4LMd1U6JdWlsGGk772Pa"
              }
            ],
            "invalid_tool_calls": []
          },
          ToolMessage {
            "content": "Cold, with a low of 13 ℃",
            "name": "search",
            "additional_kwargs": {},
            "response_metadata": {},
            "tool_call_id": "call_upim4LMd1U6JdWlsGGk772Pa"
          },
          AIMessage {
            "id": "chatcmpl-9y6ToC6yczhz1hzn5XMPt6Fha4CLJ",
            "content": "Currently, it's cold in San Francisco, with a temperature around 13°C.",
            "additional_kwargs": {},
            "response_metadata": {
              "tokenUsage": {
                "completionTokens": 17,
                "promptTokens": 107,
                "totalTokens": 124
              },
              "finish_reason": "stop",
              "system_fingerprint": "fp_3aa7262c27"
            },
            "tool_calls": [],
            "invalid_tool_calls": []
          }
        ]
      },
      next: [],
      metadata: { source: 'loop', step: 3, writes: { agent: [Object] } },
      config: {
        configurable: {
          thread_id: 'conversation-num-1',
          checkpoint_id: '1ef5e864-0045-68b1-8003-3da747a708d6'
        }
      },
      createdAt: '2024-08-19T23:53:36.443Z',
      parentConfig: undefined
    }
    --
    {
      values: {
        messages: [
          [Array],
          AIMessage {
            "id": "chatcmpl-9y6Tn0RGjUnVqxDHz5CxlGfldPS2E",
            "content": "",
            "additional_kwargs": {
              "tool_calls": [
                {
                  "id": "call_upim4LMd1U6JdWlsGGk772Pa",
                  "type": "function",
                  "function": "[Object]"
                }
              ]
            },
            "response_metadata": {
              "tokenUsage": {
                "completionTokens": 19,
                "promptTokens": 72,
                "totalTokens": 91
              },
              "finish_reason": "tool_calls",
              "system_fingerprint": "fp_3aa7262c27"
            },
            "tool_calls": [
              {
                "name": "search",
                "args": {
                  "query": "current weather in San Francisco, CA"
                },
                "type": "tool_call",
                "id": "call_upim4LMd1U6JdWlsGGk772Pa"
              }
            ],
            "invalid_tool_calls": []
          },
          ToolMessage {
            "content": "Cold, with a low of 13 ℃",
            "name": "search",
            "additional_kwargs": {},
            "response_metadata": {},
            "tool_call_id": "call_upim4LMd1U6JdWlsGGk772Pa"
          }
        ]
      },
      next: [ 'agent' ],
      metadata: { source: 'loop', step: 2, writes: { tools: [Object] } },
      config: {
        configurable: {
          thread_id: 'conversation-num-1',
          checkpoint_id: '1ef5e863-fa1c-6650-8002-bf4528305aac'
        }
      },
      createdAt: '2024-08-19T23:53:35.797Z',
      parentConfig: undefined
    }
    --
    {
      values: {
        messages: [
          [Array],
          AIMessage {
            "id": "chatcmpl-9y6Tn0RGjUnVqxDHz5CxlGfldPS2E",
            "content": "",
            "additional_kwargs": {
              "tool_calls": [
                {
                  "id": "call_upim4LMd1U6JdWlsGGk772Pa",
                  "type": "function",
                  "function": "[Object]"
                }
              ]
            },
            "response_metadata": {
              "tokenUsage": {
                "completionTokens": 19,
                "promptTokens": 72,
                "totalTokens": 91
              },
              "finish_reason": "tool_calls",
              "system_fingerprint": "fp_3aa7262c27"
            },
            "tool_calls": [
              {
                "name": "search",
                "args": {
                  "query": "current weather in San Francisco, CA"
                },
                "type": "tool_call",
                "id": "call_upim4LMd1U6JdWlsGGk772Pa"
              }
            ],
            "invalid_tool_calls": []
          }
        ]
      },
      next: [ 'tools' ],
      metadata: { source: 'loop', step: 1, writes: { agent: [Object] } },
      config: {
        configurable: {
          thread_id: 'conversation-num-1',
          checkpoint_id: '1ef5e863-f976-6611-8001-af242a92fef8'
        }
      },
      createdAt: '2024-08-19T23:53:35.729Z',
      parentConfig: undefined
    }
    --
    {
      values: { messages: [ [Array] ] },
      next: [ 'agent' ],
      metadata: { source: 'loop', step: 0, writes: null },
      config: {
        configurable: {
          thread_id: 'conversation-num-1',
          checkpoint_id: '1ef5e863-f365-6a51-8000-6443aafd5477'
        }
      },
      createdAt: '2024-08-19T23:53:35.093Z',
      parentConfig: undefined
    }
    --
    {
      values: {},
      next: [ '__start__' ],
      metadata: { source: 'input', step: -1, writes: { __start__: [Object] } },
      config: {
        configurable: {
          thread_id: 'conversation-num-1',
          checkpoint_id: '1ef5e863-f365-6a50-ffff-0ae60570513f'
        }
      },
      createdAt: '2024-08-19T23:53:35.093Z',
      parentConfig: undefined
    }
    --


## Replay a past state

To replay from this place we just need to pass its config back to the agent.



```typescript
for await (
  const { messages } of await graphWithInterrupt.stream(null, {
    ...toReplay.config,
    streamMode: "values",
  })
) {
  let msg = messages[messages?.length - 1];
  if (msg?.content) {
    console.log(msg.content);
  } else if (msg?.tool_calls?.length > 0) {
    console.log(msg.tool_calls);
  } else {
    console.log(msg);
  }
  console.log("-----\n");
}
```

    Cold, with a low of 13 ℃
    -----
    
    The current weather in San Francisco, CA is cold, with a temperature of 13°C (approximately 55°F).
    -----
    


## Branch off a past state

Using LangGraph's checkpointing, you can do more than just replay past states.
You can branch off previous locations to let the agent explore alternate
trajectories or to let a user "version control" changes in a workflow.

#### First, update a previous checkpoint

Updating the state will create a **new** snapshot by applying the update to the
previous checkpoint. Let's **add a tool message** to simulate calling the tool.


```typescript
import { ToolMessage } from "@langchain/core/messages";

const tool_calls =
  toReplay.values.messages[toReplay.values.messages.length - 1].tool_calls;
const branchConfig = await graphWithInterrupt.updateState(
  toReplay.config,
  {
    messages: [
      new ToolMessage("It's sunny out, with a high of 38 ℃.", tool_calls[0].id),
    ],
  },
  // Updates are applied "as if" they were coming from a node. By default,
  // the updates will come from the last node to run. In our case, we want to treat
  // this update as if it came from the tools node, so that the next node to run will be
  // the agent.
  "tools",
);

const branchState = await graphWithInterrupt.getState(branchConfig);
console.log(branchState.values);
console.log(branchState.next);
```

    {
      messages: [
        [ 'user', "What's the weather like in SF currently?" ],
        AIMessage {
          "id": "chatcmpl-9y6Tn0RGjUnVqxDHz5CxlGfldPS2E",
          "content": "",
          "additional_kwargs": {
            "tool_calls": [
              {
                "id": "call_upim4LMd1U6JdWlsGGk772Pa",
                "type": "function",
                "function": "[Object]"
              }
            ]
          },
          "response_metadata": {
            "tokenUsage": {
              "completionTokens": 19,
              "promptTokens": 72,
              "totalTokens": 91
            },
            "finish_reason": "tool_calls",
            "system_fingerprint": "fp_3aa7262c27"
          },
          "tool_calls": [
            {
              "name": "search",
              "args": {
                "query": "current weather in San Francisco, CA"
              },
              "type": "tool_call",
              "id": "call_upim4LMd1U6JdWlsGGk772Pa"
            }
          ],
          "invalid_tool_calls": []
        },
        ToolMessage {
          "content": "It's sunny out, with a high of 38 ℃.",
          "additional_kwargs": {},
          "response_metadata": {},
          "tool_call_id": "call_upim4LMd1U6JdWlsGGk772Pa"
        }
      ]
    }
    [ 'agent' ]


#### Now you can run from this branch

Just use the updated config (containing the new checkpoint ID). The trajectory
will follow the new branch.


```typescript
for await (
  const { messages } of await graphWithInterrupt.stream(null, {
    ...branchConfig,
    streamMode: "values",
  })
) {
  let msg = messages[messages?.length - 1];
  if (msg?.content) {
    console.log(msg.content);
  } else if (msg?.tool_calls?.length > 0) {
    console.log(msg.tool_calls);
  } else {
    console.log(msg);
  }
  console.log("-----\n");
}
```

    The current weather in San Francisco is sunny with a high of 38°C (100.4°F).
    -----
    

