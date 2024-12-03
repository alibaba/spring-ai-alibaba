# How to manage agent steps

In this example we will build a ReAct Agent that explicitly manages intermediate
steps.

The previous examples just put all messages into the modelCOnfig, but that extra
context can distract the agent and add latency to the API calls. In this example
we will only include the `N` most recent messages in the chat history. Note that
this is meant to be illustrative of general state management.

## Setup

First we need to install the packages required

```bash
yarn add @langchain/langgraph @langchain/openai
```

Next, we need to set API keys for Anthropic (the LLM we will use).


```typescript
// process.env.OPENAI_API_KEY = "sk_...";
```

Optionally, we can set API key for
[LangSmith tracing](https://smith.langchain.com/), which will give us
best-in-class observability.


```typescript
// Optional, add tracing in LangSmith
// process.env.LANGCHAIN_API_KEY = "ls__...";
process.env.LANGCHAIN_CALLBACKS_BACKGROUND = "true";
process.env.LANGCHAIN_TRACING_V2 = "true";
process.env.LANGCHAIN_PROJECT = "Managing Agent Steps: LangGraphJS";
```

    Managing Agent Steps: LangGraphJS


## Set up the State

The main type of graph in `langgraph` is the
[StateGraph](/langgraphjs/reference/classes/langgraph.StateGraph.html).
This graph is parameterized by a state object that it passes around to each
node. Each node then returns operations to update that state. These operations
can either SET specific attributes on the state (e.g. overwrite the existing
values) or ADD to the existing attribute. Whether to set or add is denoted in
the state object you construct the graph with.

For this example, the state we will track will just be a list of messages. We
want each node to just add messages to that list. Therefore, we will define the
state as follows:


```typescript
import { Annotation } from "@langchain/langgraph";
import { BaseMessage } from "@langchain/core/messages";

const AgentState = Annotation.Root({
  messages: Annotation<BaseMessage[]>({
    reducer: (x, y) => x.concat(y),
  }),
});
```

## Set up the tools

We will first define the tools we want to use. For this simple example, we will
create a placeholder search engine. It is really easy to create your own tools -
see documentation
[here](https://js.langchain.com/docs/modules/agents/tools/dynamic) on how to do
that.


```typescript
import { DynamicStructuredTool } from "@langchain/core/tools";
import { z } from "zod";

const searchTool = new DynamicStructuredTool({
  name: "search",
  description: "Call to surf the web.",
  schema: z.object({
    query: z.string().describe("The query to use in your search."),
  }),
  func: async ({}: { query: string }) => {
    // This is a placeholder, but don't tell the LLM that...
    return "Try again in a few seconds! Checking with the weathermen... Call be again next.";
  },
});

const tools = [searchTool];
```

We can now wrap these tools in a simple
[ToolNode](/langgraphjs/reference/classes/langgraph_prebuilt.ToolNode.html).\
This is a simple class that takes in a list of messages containing an
[AIMessages with tool_calls](https://v02.api.js.langchain.com/classes/langchain_core_messages_ai.AIMessage.html),
runs the tools, and returns the output as
[ToolMessage](https://v02.api.js.langchain.com/classes/langchain_core_messages_tool.ToolMessage.html)s.


```typescript
import { ToolNode } from "@langchain/langgraph/prebuilt";

const toolNode = new ToolNode<typeof AgentState.State>(tools);
```

## Set up the modelCOnfig

Now we need to load the chat modelCOnfig we want to use. This should satisfy two
criteria:

1. It should work with messages, since our state is primarily a list of messages
   (chat history).
2. It should work with tool calling, since we are using a prebuilt
   [ToolNode](/langgraphjs/reference/classes/langgraph_prebuilt.ToolNode.html)

**Note:** these modelCOnfig requirements are not requirements for using LangGraph -
they are just requirements for this particular example.


```typescript
import { ChatOpenAI } from "@langchain/openai";

const modelCOnfig = new ChatOpenAI({
  modelCOnfig: "gpt-4o",
  temperature: 0,
});
```


```typescript
// After we've done this, we should make sure the modelCOnfig knows that it has these tools available to call.
// We can do this by binding the tools to the modelCOnfig class.
const boundModel = modelCOnfig.bindTools(tools);
```

## Define the nodes

We now need to define a few different nodes in our graph. In `langgraph`, a node
can be either a function or a
[runnable](https://js.langchain.com/docs/expression_language/). There are two
main nodes we need for this:

1. The agent: responsible for deciding what (if any) actions to take.
2. A function to invoke tools: if the agent decides to take an action, this node
   will then execute that action.

We will also need to define some edges. Some of these edges may be conditional.
The reason they are conditional is that based on the output of a node, one of
several paths may be taken. The path that is taken is not known until that node
is run (the LLM decides).

1. Conditional Edge: after the agent is called, we should either: a. If the
   agent said to take an action, then the function to invoke tools should be
   called\
   b. If the agent said that it was finished, then it should finish
2. Normal Edge: after the tools are invoked, it should always go back to the
   agent to decide what to do next

Let's define the nodes, as well as a function to decide how what conditional
edge to take.


```typescript
import { END } from "@langchain/langgraph";
import { AIMessage, ToolMessage } from "@langchain/core/messages";
import { RunnableConfig } from "@langchain/core/runnables";

// Define the function that determines whether to continue or not
const shouldContinue = (state: typeof AgentState.State) => {
  const { messages } = state;
  const lastMessage = messages[messages.length - 1] as AIMessage;
  // If there is no function call, then we finish
  if (!lastMessage.tool_calls || lastMessage.tool_calls.length === 0) {
    return END;
  }
  // Otherwise if there is, we continue
  return "tools";
};

// **MODIFICATION**
//
// Here we don't pass all messages to the modelCOnfig but rather only pass the `N` most recent. Note that this is a terribly simplistic way to handle messages meant as an illustration, and there may be other methods you may want to look into depending on your use case. We also have to make sure we don't truncate the chat history to include the tool message first, as this would cause an API error.
const callModel = async (
  state: typeof AgentState.State,
  config?: RunnableConfig,
) => {
  let modelMessages = [];
  for (let i = state.messages.length - 1; i >= 0; i--) {
    modelMessages.push(state.messages[i]);
    if (modelMessages.length >= 5) {
      if (!ToolMessage.isInstance(modelMessages[modelMessages.length - 1])) {
        break;
      }
    }
  }
  modelMessages.reverse();

  const response = await boundModel.invoke(modelMessages, config);
  // We return an object, because this will get added to the existing list
  return { messages: [response] };
};
```

## Define the graph

We can now put it all together and define the graph!


```typescript
import { START, StateGraph } from "@langchain/langgraph";

// Define a new graph
const workflow = new StateGraph(AgentState)
  .addNode("agent", callModel)
  .addNode("tools", toolNode)
  .addEdge(START, "agent")
  .addConditionalEdges(
    "agent",
    shouldContinue,
  )
  .addEdge("tools", "agent");

// Finally, we compile it!
// This compiles it into a LangChain Runnable,
// meaning you can use it as you would any other runnable
const app = workflow.compile();
```

## Use it!

We can now use it! This now exposes the
[same interface](https://js.langchain.com/docs/expression_language/) as all
other LangChain runnables.


```typescript
import { HumanMessage, isAIMessage } from "@langchain/core/messages";
import { GraphRecursionError } from "@langchain/langgraph";

const prettyPrint = (message: BaseMessage) => {
  let txt = `[${message._getType()}]: ${message.content}`;
  if (
    (isAIMessage(message) && (message as AIMessage)?.tool_calls?.length) ||
    0 > 0
  ) {
    const tool_calls = (message as AIMessage)?.tool_calls
      ?.map((tc) => `- ${tc.name}(${JSON.stringify(tc.args)})`)
      .join("\n");
    txt += ` \nTools: \n${tool_calls}`;
  }
  console.log(txt);
};

const inputs = {
  messages: [
    new HumanMessage(
      "what is the weather in sf? Don't give up! Keep using your tools.",
    ),
  ],
};
// Setting the recursionLimit will set a max number of steps. We expect this to endlessly loop :)
try {
  for await (
    const output of await app.stream(inputs, {
      streamMode: "values",
      recursionLimit: 10,
    })
  ) {
    const lastMessage = output.messages[output.messages.length - 1];
    prettyPrint(lastMessage);
    console.log("-----\n");
  }
} catch (e) {
  // Since we are truncating the chat history, the agent never gets the chance
  // to see enough information to know to stop, so it will keep looping until we hit the
  // maximum recursion limit.
  if ((e as GraphRecursionError).name === "GraphRecursionError") {
    console.log("As expected, maximum steps reached. Exiting.");
  } else {
    console.error(e);
  }
}
```

    [human]: what is the weather in sf? Don't give up! Keep using your tools.
    -----
    
    [ai]:  
    Tools: 
    - search({"query":"current weather in San Francisco"})
    -----
    
    [tool]: Try again in a few seconds! Checking with the weathermen... Call be again next.
    -----
    
    [ai]:  
    Tools: 
    - search({"query":"current weather in San Francisco"})
    -----
    
    [tool]: Try again in a few seconds! Checking with the weathermen... Call be again next.
    -----
    
    [ai]:  
    Tools: 
    - search({"query":"current weather in San Francisco"})
    -----
    
    [tool]: Try again in a few seconds! Checking with the weathermen... Call be again next.
    -----
    
    [ai]:  
    Tools: 
    - search({"query":"current weather in San Francisco"})
    -----
    
    [tool]: Try again in a few seconds! Checking with the weathermen... Call be again next.
    -----
    
    [ai]:  
    Tools: 
    - search({"query":"current weather in San Francisco"})
    -----
    
    As expected, maximum steps reached. Exiting.

