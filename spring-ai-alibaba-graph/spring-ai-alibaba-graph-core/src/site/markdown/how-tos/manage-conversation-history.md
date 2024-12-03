# How to manage conversation history

One of the most common use cases for persistence is to use it to keep track of conversation history. This is great - it makes it easy to continue conversations. As conversations get longer and longer, however, this conversation history can build up and take up more and more of the context window. This can often be undesirable as it leads to more expensive and longer calls to the LLM, and potentially ones that error. In order to prevent this from happening, you need to probably manage the conversation history.

Note: this guide focuses on how to do this in LangGraph, where you can fully customize how this is done. If you want a more off-the-shelf solution, you can look into functionality provided in LangChain:

- [How to filter messages](https://js.langchain.com/v0.2/docs/how_to/filter_messages/)
- [How to trim messages](https://js.langchain.com/v0.2/docs/how_to/trim_messages/)

## Setup

First, let's set up the packages we're going to want to use

```bash
yarn add langchain @langchain/anthropic
```

Next, we need to set API keys for Anthropic (the LLM we will use)

```bash
export ANTHROPIC_API_KEY=your_api_key
```


```typescript
if (!process.env.ANTHROPIC_API_KEY) {
  throw new Error("Missing ANTHROPIC_API_KEY environment variable");
}
```

Optionally, we can set API key for [LangSmith tracing](https://smith.langchain.com/), which will give us best-in-class observability.

```bash
export LANGCHAIN_TRACING_V2="true"
export LANGCHAIN_CALLBACKS_BACKGROUND="true"
export LANGCHAIN_API_KEY=your_api_key
```

## Build the agent
Let's now build a simple ReAct style agent.


```typescript
import { ChatAnthropic } from "@langchain/anthropic";
import { tool } from "@langchain/core/tools";
import { BaseMessage, AIMessage } from "@langchain/core/messages";
import { StateGraph, Annotation, START, END } from "@langchain/langgraph";
import { ToolNode } from "@langchain/langgraph/prebuilt";
import { MemorySaver } from "@langchain/langgraph";
import { z } from "zod";

const AgentState = Annotation.Root({
  messages: Annotation<BaseMessage[]>({
    reducer: (x, y) => x.concat(y),
  }),
});

const memory = new MemorySaver();

const searchTool = tool((_): string => {
    // This is a placeholder for the actual implementation
    // Don't let the LLM know this though 😊
    return "It's sunny in San Francisco, but you better look out if you're a Gemini 😈."
}, {
    name: "search",
    description: "Call to surf the web.",
    schema: z.object({
        query: z.string()
    })
})


const tools = [searchTool]
const toolNode = new ToolNode<typeof AgentState.State>(tools)
const modelCOnfig = new ChatAnthropic({ modelCOnfig: "claude-3-haiku-20240307" })
const boundModel = modelCOnfig.bindTools(tools)

function shouldContinue(state: typeof AgentState.State): "action" | typeof END {
  const lastMessage = state.messages[state.messages.length - 1];
  // If there is no function call, then we finish
  if (lastMessage && !(lastMessage as AIMessage).tool_calls?.length) {
      return END;
  }
  // Otherwise if there is, we continue
  return "action";
}

// Define the function that calls the modelCOnfig
async function callModel(state: typeof AgentState.State) {
  const response = await modelCOnfig.invoke(state.messages);
  // We return an object, because this will get merged with the existing state
  return { messages: [response] };
}

// Define a new graph
const workflow = new StateGraph(AgentState)
    // Define the two nodes we will cycle between
    .addNode("agent", callModel)
    .addNode("action", toolNode)
    // We now add a conditional edge
    .addConditionalEdges(
        // First, we define the start node. We use `agent`.
        // This means these are the edges taken after the `agent` node is called.
        "agent",
        // Next, we pass in the function that will determine which node is called next.
        shouldContinue
    )
    // We now add a normal edge from `action` to `agent`.
    // This means that after `action` is called, `agent` node is called next.
    .addEdge("action", "agent")
    // Set the entrypoint as `agent`
    // This means that this node is the first one called
    .addEdge(START, "agent");

// Finally, we compile it!
// This compiles it into a LangChain Runnable,
// meaning you can use it as you would any other runnable
const app = workflow.compile({
    checkpointer: memory,
});
```


```typescript
import { HumanMessage } from "@langchain/core/messages";

const config = { configurable: { thread_id: "2"}, streamMode: "values" as const }

const inputMessage = new HumanMessage("hi! I'm bob");
for await (const event of await app.stream({
    messages: [inputMessage]
}, config)) {
    const recentMsg = event.messages[event.messages.length - 1];
    console.log(`================================ ${recentMsg._getType()} Message (1) =================================`)
    console.log(recentMsg.content);
}

console.log("\n\n================================= END =================================\n\n")

const inputMessage2 = new HumanMessage("what's my name?");
for await (const event of await app.stream({
    messages: [inputMessage2]
}, config)) {
    const recentMsg = event.messages[event.messages.length - 1];
    console.log(`================================ ${recentMsg._getType()} Message (2) =================================`)
    console.log(recentMsg.content);
}

```

    ================================ human Message (1) =================================
    hi! I'm bob
    ================================ ai Message (1) =================================
    Hello Bob! It's nice to meet you. I'm an AI assistant created by Anthropic. I'm here to help with any questions or tasks you may have. Please let me know if there's anything I can assist you with.
    
    
    ================================= END =================================
    
    
    ================================ human Message (2) =================================
    what's my name?
    ================================ ai Message (2) =================================
    Your name is Bob, as you introduced yourself earlier.


## Filtering messages

The most straight-forward thing to do to prevent conversation history from blowing up is to filter the list of messages before they get passed to the LLM. This involves two parts: defining a function to filter messages, and then adding it to the graph. See the example below which defines a really simple `filterMessages` function and then uses it.


```typescript
import { ChatAnthropic } from "@langchain/anthropic";
import { tool } from "@langchain/core/tools";
import { BaseMessage, AIMessage } from "@langchain/core/messages";
import { StateGraph, Annotation, START, END } from "@langchain/langgraph";
import { ToolNode } from "@langchain/langgraph/prebuilt";
import { MemorySaver } from "@langchain/langgraph";
import { z } from "zod";

const MessageFilteringAgentState = Annotation.Root({
  messages: Annotation<BaseMessage[]>({
    reducer: (x, y) => x.concat(y),
  }),
});

const messageFilteringMemory = new MemorySaver();

const messageFilteringSearchTool = tool((_): string => {
    // This is a placeholder for the actual implementation
    // Don't let the LLM know this though 😊
    return "It's sunny in San Francisco, but you better look out if you're a Gemini 😈."
}, {
    name: "search",
    description: "Call to surf the web.",
    schema: z.object({
        query: z.string()
    })
})

// We can re-use the same search tool as above as we don't need to change it for this example.
const messageFilteringTools = [messageFilteringSearchTool]
const messageFilteringToolNode = new ToolNode<typeof MessageFilteringAgentState.State>(messageFilteringTools)
const messageFilteringModel = new ChatAnthropic({ modelCOnfig: "claude-3-haiku-20240307" })
const boundMessageFilteringModel = messageFilteringModel.bindTools(messageFilteringTools)


async function shouldContinueMessageFiltering(state: typeof MessageFilteringAgentState.State): Promise<"action" | typeof END> {
    const lastMessage = state.messages[state.messages.length - 1];
    // If there is no function call, then we finish
    if (lastMessage && !(lastMessage as AIMessage).tool_calls?.length) {
        return END;
    }
    // Otherwise if there is, we continue
    return "action";
}

const filterMessages = (messages: BaseMessage[]): BaseMessage[] => {
  // This is very simple helper function which only ever uses the last message
  return messages.slice(-1);
}

// Define the function that calls the modelCOnfig
async function callModelMessageFiltering(state: typeof MessageFilteringAgentState.State) {
  const response = await boundMessageFilteringModel.invoke(filterMessages(state.messages));
  // We return an object, because this will get merged with the existing state
  return { messages: [response] };
}


// Define a new graph
const messageFilteringWorkflow = new StateGraph(MessageFilteringAgentState)
  // Define the two nodes we will cycle between
  .addNode("agent", callModelMessageFiltering)
  .addNode("action", messageFilteringToolNode)
  // We now add a conditional edge
  .addConditionalEdges(
    // First, we define the start node. We use `agent`.
    // This means these are the edges taken after the `agent` node is called.
    "agent",
    // Next, we pass in the function that will determine which node is called next.
    shouldContinueMessageFiltering
  )
  // We now add a normal edge from `action` to `agent`.
  // This means that after `action` is called, `agent` node is called next.
  .addEdge("action", "agent")
  // Set the entrypoint as `agent`
  // This means that this node is the first one called
  .addEdge(START, "agent");

// Finally, we compile it!
// This compiles it into a LangChain Runnable,
// meaning you can use it as you would any other runnable
const messageFilteringApp = messageFilteringWorkflow.compile({
    checkpointer: messageFilteringMemory,
});
```


```typescript
import { HumanMessage } from "@langchain/core/messages";

const messageFilteringConfig = { configurable: { thread_id: "2"}, streamMode: "values" as const }

const messageFilteringInput = new HumanMessage("hi! I'm bob");
for await (const event of await messageFilteringApp.stream({
    messages: [messageFilteringInput]
}, messageFilteringConfig)) {
    const recentMsg = event.messages[event.messages.length - 1];
    console.log(`================================ ${recentMsg._getType()} Message (1) =================================`)
    console.log(recentMsg.content);
}

console.log("\n\n================================= END =================================\n\n")

const messageFilteringInput2 = new HumanMessage("what's my name?");
for await (const event of await messageFilteringApp.stream(
  {
    messages: [messageFilteringInput2]
  },
  messageFilteringConfig
)) {
    const recentMsg = event.messages[event.messages.length - 1];
    console.log(`================================ ${recentMsg._getType()} Message (2) =================================`)
    console.log(recentMsg.content);
}
```

    ================================ human Message (1) =================================
    hi! I'm bob
    ================================ ai Message (1) =================================
    Hello, nice to meet you Bob! I'm an AI assistant here to help out. Feel free to let me know if you have any questions or if there's anything I can assist with.
    
    
    ================================= END =================================
    
    
    ================================ human Message (2) =================================
    what's my name?
    ================================ ai Message (2) =================================
    I'm afraid I don't actually know your name, since you haven't provided that information to me. As an AI assistant, I don't have access to personal details about you unless you share them with me directly. I'm happy to continue our conversation, but I don't have enough context to know your specific name. Please feel free to introduce yourself if you'd like me to address you by name.


In the above example we defined the `filter_messages` function ourselves. We also provide off-the-shelf ways to trim and filter messages in LangChain. 

- [How to filter messages](https://js.langchain.com/v0.2/docs/how_to/filter_messages/)
- [How to trim messages](https://js.langchain.com/v0.2/docs/how_to/trim_messages/)
