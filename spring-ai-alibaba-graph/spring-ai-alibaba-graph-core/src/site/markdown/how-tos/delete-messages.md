# How to delete messages

One of the common states for a graph is a list of messages. Usually you only add messages to that state. However, sometimes you may want to remove messages (either by directly modifying the state or as part of the graph). To do that, you can use the `RemoveMessage` modifier. In this guide, we will cover how to do that.

The key idea is that each state key has a `reducer` key. This key specifies how to combine updates to the state. The default `MessagesState` has a messages key, and the reducer for that key accepts these `RemoveMessage` modifiers. That reducer then uses these `RemoveMessage` to delete messages from the key.

So note that just because your graph state has a key that is a list of messages, it doesn't mean that that this `RemoveMessage` modifier will work. You also have to have a `reducer` defined that knows how to work with this.

**NOTE**: Many models expect certain rules around lists of messages. For example, some expect them to start with a `user` message, others expect all messages with tool calls to be followed by a tool message. **When deleting messages, you will want to make sure you don't violate these rules.**

## Setup


First, install the required dependencies for this example:

```bash
npm install @langchain/langgraph @langchain/openai @langchain/core zod uuid
```

Next, we need to set API keys for OpenAI (the LLM we will use):

```typescript
process.env.OPENAI_API_KEY = 'YOUR_API_KEY';
```

Optionally, we can set API key for [LangSmith tracing](https://smith.langchain.com/), which will give us best-in-class observability.

```typescript
process.env.LANGCHAIN_TRACING_V2 = "true";
process.env.LANGCHAIN_API_KEY = "YOUR_API_KEY";
```

Now, let's build a simple graph that uses messages. Note that it's using the `MessagesState` which has the required `reducer`.

## Build the agent
Let's now build a simple ReAct style agent.


```typescript
import { ChatOpenAI } from "@langchain/openai";
import { tool } from "@langchain/core/tools";
import { MemorySaver } from "@langchain/langgraph-checkpoint";
import { MessagesAnnotation, StateGraph, START, END } from "@langchain/langgraph";
import { ToolNode } from "@langchain/langgraph/prebuilt";
import { z } from "zod";

const memory = new MemorySaver();

const search = tool((_) => {
  // This is a placeholder for the actual implementation
  // Don't let the LLM know this though 😊
  return [
    "It's sunny in San Francisco, but you better look out if you're a Gemini 😈.",
  ];
}, {
  name: "search",
  description: "Call to surf the web.",
  schema: z.object({
    query: z.string(),
  })
});

const tools = [search];
const toolNode = new ToolNode<typeof MessagesAnnotation.State>(tools);
const model = new ChatOpenAI({ model: "gpt-4o" });
const boundModel = model.bindTools(tools);

function shouldContinue(state: typeof MessagesAnnotation.State): "action" | typeof END {
  const lastMessage = state.messages[state.messages.length - 1];
  if (
    "tool_calls" in lastMessage &&
    Array.isArray(lastMessage.tool_calls) &&
    lastMessage.tool_calls.length
  ) {
    return "action";
  }
  // If there is no tool call, then we finish
  return END;
}

// Define the function that calls the model
async function callModel(state: typeof MessagesAnnotation.State) {
  const response = await boundModel.invoke(state.messages);
  return { messages: [response] };
}

// Define a new graph
const workflow = new StateGraph(MessagesAnnotation)
  // Define the two nodes we will cycle between
  .addNode("agent", callModel)
  .addNode("action", toolNode)
  // Set the entrypoint as `agent`
  // This means that this node is the first one called
  .addEdge(START, "agent")
  // We now add a conditional edge
  .addConditionalEdges(
    // First, we define the start node. We use `agent`.
    // This means these are the edges taken after the `agent` node is called.
    "agent",
    // Next, we pass in the function that will determine which node is called next.
    shouldContinue
  )
  // We now add a normal edge from `tools` to `agent`.
  // This means that after `tools` is called, `agent` node is called next.
  .addEdge("action", "agent");

// Finally, we compile it!
// This compiles it into a LangChain Runnable,
// meaning you can use it as you would any other runnable
const app = workflow.compile({ checkpointer: memory });
```


```typescript
import { HumanMessage } from "@langchain/core/messages";
import { v4 as uuidv4 } from "uuid";

const config = { configurable: { thread_id: "2" }, streamMode: "values" as const };
const inputMessage = new HumanMessage({
  id: uuidv4(),
  content: "hi! I'm bob",
});

for await (const event of await app.stream(
  { messages: [inputMessage] },
  config,
)) {
  const lastMsg = event.messages[event.messages.length - 1];
  console.dir(
    {
      type: lastMsg._getType(),
      content: lastMsg.content,
      tool_calls: lastMsg.tool_calls,
    },
    { depth: null }
  )
}

const inputMessage2 = new HumanMessage({
  id: uuidv4(),
  content: "What's my name?",
});
for await (const event of await app.stream(
  { messages: [inputMessage2] },
  config,
)) {
  const lastMsg = event.messages[event.messages.length - 1];
  console.dir(
    {
      type: lastMsg._getType(),
      content: lastMsg.content,
      tool_calls: lastMsg.tool_calls,
    },
    { depth: null }
  )
}
```

    { type: 'human', content: "hi! I'm bob", tool_calls: undefined }
    {
      type: 'ai',
      content: 'Hi Bob! How can I assist you today?',
      tool_calls: []
    }
    { type: 'human', content: "What's my name?", tool_calls: undefined }
    { type: 'ai', content: 'Your name is Bob.', tool_calls: [] }


## Manually deleting messages

First, we will cover how to manually delete messages. Let's take a look at the current state of the thread:


```typescript
const messages = (await app.getState(config)).values.messages;
console.dir(
  messages.map((msg) => ({
    id: msg.id,
    type: msg._getType(),
    content: msg.content,
    tool_calls:
    msg.tool_calls,
  })),
  { depth: null }
);
```

    [
      {
        id: '24187daa-00dd-40d8-bc30-f4e24ff78165',
        type: 'human',
        content: "hi! I'm bob",
        tool_calls: undefined
      },
      {
        id: 'chatcmpl-9zYV9yHLiZmR2ZVHEhHcbVEshr3qG',
        type: 'ai',
        content: 'Hi Bob! How can I assist you today?',
        tool_calls: []
      },
      {
        id: 'a67e53c3-5dcf-4ddc-83f5-309b72ac61f4',
        type: 'human',
        content: "What's my name?",
        tool_calls: undefined
      },
      {
        id: 'chatcmpl-9zYV9mmpJrm3SQ7ngMJZ1XBHzHfL6',
        type: 'ai',
        content: 'Your name is Bob.',
        tool_calls: []
      }
    ]


We can call `updateState` and pass in the id of the first message. This will delete that message.


```typescript
import { RemoveMessage } from "@langchain/core/messages";

await app.updateState(config, { messages: new RemoveMessage({ id: messages[0].id }) })
```

    {
      configurable: {
        thread_id: '2',
        checkpoint_ns: '',
        checkpoint_id: '1ef61abf-1fc2-6431-8005-92730e9d667c'
      }
    }


If we now look at the messages, we can verify that the first one was deleted.


```typescript
const updatedMessages = (await app.getState(config)).values.messages;
console.dir(
  updatedMessages.map((msg) => ({
    id: msg.id,
    type: msg._getType(),
    content: msg.content,
    tool_calls:
    msg.tool_calls,
  })),
  { depth: null }
);
```

    [
      {
        id: 'chatcmpl-9zYV9yHLiZmR2ZVHEhHcbVEshr3qG',
        type: 'ai',
        content: 'Hi Bob! How can I assist you today?',
        tool_calls: []
      },
      {
        id: 'a67e53c3-5dcf-4ddc-83f5-309b72ac61f4',
        type: 'human',
        content: "What's my name?",
        tool_calls: undefined
      },
      {
        id: 'chatcmpl-9zYV9mmpJrm3SQ7ngMJZ1XBHzHfL6',
        type: 'ai',
        content: 'Your name is Bob.',
        tool_calls: []
      }
    ]


## Programmatically deleting messages

We can also delete messages programmatically from inside the graph. Here we'll modify the graph to delete any old messages (longer than 3 messages ago) at the end of a graph run.


```typescript
import { RemoveMessage } from "@langchain/core/messages";
import { StateGraph, START, END } from "@langchain/langgraph";
import { MessagesAnnotation } from "@langchain/langgraph";

function deleteMessages(state: typeof MessagesAnnotation.State) {
  const messages = state.messages;
  if (messages.length > 3) {
    return { messages: messages.slice(0, -3).map(m => new RemoveMessage({ id: m.id })) };
  }
  return {};
}

// We need to modify the logic to call deleteMessages rather than end right away
function shouldContinue2(state: typeof MessagesAnnotation.State): "action" | "delete_messages" {
  const lastMessage = state.messages[state.messages.length - 1];
  if (
    "tool_calls" in lastMessage &&
    Array.isArray(lastMessage.tool_calls) &&
    lastMessage.tool_calls.length
  ) {
    return "action";
  }
  // Otherwise if there aren't, we finish
  return "delete_messages";
}

// Define a new graph
const workflow2 = new StateGraph(MessagesAnnotation)
  .addNode("agent", callModel)
  .addNode("action", toolNode)
  // This is our new node we're defining
  .addNode("delete_messages", deleteMessages)
  .addEdge(START, "agent")
  .addConditionalEdges(
    "agent",
    shouldContinue2
  )
  .addEdge("action", "agent")
  // This is the new edge we're adding: after we delete messages, we finish
  .addEdge("delete_messages", END);

const app2 = workflow2.compile({ checkpointer: memory });
```

We can now try this out. We can call the graph twice and then check the state


```typescript
import { HumanMessage } from "@langchain/core/messages";
import { v4 as uuidv4 } from "uuid";

const config2 = { configurable: { thread_id: "3" }, streamMode: "values" as const };

const inputMessage3 = new HumanMessage({
  id: uuidv4(),
  content: "hi! I'm bob",
});

console.log("--- FIRST ITERATION ---\n");
for await (const event of await app2.stream(
  { messages: [inputMessage3] },
  config2
)) {
  console.log(event.messages.map((message) => [message._getType(), message.content]));
}

const inputMessage4 = new HumanMessage({
  id: uuidv4(),
  content: "what's my name?",
});

console.log("\n\n--- SECOND ITERATION ---\n");
for await (const event of await app2.stream(
  { messages: [inputMessage4] },
  config2
)) {
  console.log(event.messages.map((message) => [message._getType(), message.content]), "\n");
}
```

    --- FIRST ITERATION ---
    
    [ [ 'human', "hi! I'm bob" ] ]


    [
      [ 'human', "hi! I'm bob" ],
      [ 'ai', 'Hi Bob! How can I assist you today?' ]
    ]
    
    
    --- SECOND ITERATION ---
    
    [
      [ 'human', "hi! I'm bob" ],
      [ 'ai', 'Hi Bob! How can I assist you today?' ],
      [ 'human', "what's my name?" ]
    ] 
    
    [
      [ 'human', "hi! I'm bob" ],
      [ 'ai', 'Hi Bob! How can I assist you today?' ],
      [ 'human', "what's my name?" ],
      [ 'ai', "Based on what you've told me, your name is Bob." ]
    ] 
    
    [
      [ 'ai', 'Hi Bob! How can I assist you today?' ],
      [ 'human', "what's my name?" ],
      [ 'ai', "Based on what you've told me, your name is Bob." ]
    ] 
    


If we now check the state, we should see that it is only three messages long. This is because we just deleted the earlier messages - otherwise it would be four!


```typescript
const messages3 = (await app.getState(config2)).values["messages"]
console.dir(
  messages3.map((msg) => ({
    id: msg.id,
    type: msg._getType(),
    content: msg.content,
    tool_calls:
    msg.tool_calls,
  })),
  { depth: null }
);
```

    [
      {
        id: 'chatcmpl-9zYVAEiiC9D7bb0wF4KLXgY0OAG8O',
        type: 'ai',
        content: 'Hi Bob! How can I assist you today?',
        tool_calls: []
      },
      {
        id: 'b93e5f35-cfa3-4ca6-9b59-154ce2bd476b',
        type: 'human',
        content: "what's my name?",
        tool_calls: undefined
      },
      {
        id: 'chatcmpl-9zYVBHJWtEM6pw2koE8dykzSA0XSO',
        type: 'ai',
        content: "Based on what you've told me, your name is Bob.",
        tool_calls: []
      }
    ]


Remember, when deleting messages you will want to make sure that the remaining message list is still valid. This message list **may actually not be** - this is because it currently starts with an AI message, which some models do not allow.
