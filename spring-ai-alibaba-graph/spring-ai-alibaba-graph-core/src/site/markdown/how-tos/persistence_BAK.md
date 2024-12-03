# How to add persistence ("memory") to your graph

Many AI applications need memory to share context across multiple interactions.
In LangGraph, memory is provided for any
[StateGraph](/langgraphjs/reference/classes/langgraph.StateGraph.html)
through
[Checkpointers](/langgraphjs/reference/modules/checkpoint.html).

When creating any LangGraph workflow, you can set them up to persist their state
by doing using the following:

1. A
   [Checkpointer](/langgraphjs/reference/classes/checkpoint.BaseCheckpointSaver.html),
   such as the
   [MemorySaver](/langgraphjs/reference/classes/checkpoint.MemorySaver.html)
2. Call `compile(checkpointer=myCheckpointer)` when compiling the graph.

Example:

```javascript
import { MemorySaver, Annotation } from "@langchain/langgraph";

const GraphState = Annotation.Root({ ... });

const workflow = new StateGraph(GraphState);

/// ... Add nodes and edges
// Initialize any compatible CheckPointSaver
const memory = new MemorySaver();
const persistentGraph = workflow.compile({ checkpointer: memory });
```

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
process.env.LANGCHAIN_PROJECT = "Persistence: LangGraphJS";
```

    Persistence: LangGraphJS


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
  func: async ({}: { query: string }) => {
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

We can now put it all together. We will run it first without a checkpointer:



```typescript
import { END, START, StateGraph } from "@langchain/langgraph";
import { AIMessage } from "@langchain/core/messages";
import { RunnableConfig } from "@langchain/core/runnables";

const routeMessage = (state: typeof GraphState.State) => {
  const { messages } = state;
  const lastMessage = messages[messages.length - 1] as AIMessage;
  // If no tools are called, we can finish (respond to the user)
  if (!lastMessage.tool_calls?.length) {
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

const graph = workflow.compile();
```


```typescript
let inputs = { messages: [["user", "Hi I'm Yu, niced to meet you."]] };
for await (
  const { messages } of await graph.stream(inputs, {
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

    [ 'user', "Hi I'm Yu, niced to meet you." ]
    -----
    
    Hi Yu, nice to meet you too! How can I assist you today?
    -----
    



```typescript
inputs = { messages: [["user", "Remember my name?"]] };
for await (
  const { messages } of await graph.stream(inputs, {
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

    [ 'user', 'Remember my name?' ]
    -----
    
    I don't have memory of previous interactions, so I don't remember your name. Can you please tell me again?
    -----
    


## Add Memory

Let's try it again with a checkpointer. We will use the
[MemorySaver](/langgraphjs/reference/classes/index.MemorySaver.html),
which will "save" checkpoints in-memory.


```typescript
import { MemorySaver } from "@langchain/langgraph";

// Here we only save in-memory
const memory = new MemorySaver();
const persistentGraph = workflow.compile({ checkpointer: memory });
```


```typescript
let config = { configurable: { thread_id: "conversation-num-1" } };
inputs = { messages: [["user", "Hi I'm Jo, niced to meet you."]] };
for await (
  const { messages } of await persistentGraph.stream(inputs, {
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

    [ 'user', "Hi I'm Jo, niced to meet you." ]
    -----
    
    Hi Jo, nice to meet you too! How can I assist you today?
    -----
    



```typescript
inputs = { messages: [["user", "Remember my name?"]] };
for await (
  const { messages } of await persistentGraph.stream(inputs, {
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

    [ 'user', 'Remember my name?' ]
    -----
    
    Of course, Jo! How can I help you today?
    -----
    


## New Conversational Thread

If we want to start a new conversation, we can pass in a different
**`thread_id`**. Poof! All the memories are gone (just kidding, they'll always
live in that other thread)!



```typescript
config = { configurable: { thread_id: "conversation-2" } };
```

    { configurable: { thread_id: 'conversation-2' } }



```typescript
inputs = { messages: [["user", "you forgot?"]] };
for await (
  const { messages } of await persistentGraph.stream(inputs, {
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

    [ 'user', 'you forgot?' ]
    -----
    


    I'm sorry, it seems like I missed something. Could you remind me what you're referring to?
    -----
    

