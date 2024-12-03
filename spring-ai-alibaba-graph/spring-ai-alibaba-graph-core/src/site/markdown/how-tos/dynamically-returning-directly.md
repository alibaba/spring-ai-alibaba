# How to let agent return tool results directly

A typical ReAct loop follows user -> assistant -> tool -> assistant ..., ->
user. In some cases, you don't need to call the LLM after the tool completes,
the user can view the results directly themselves.

In this example we will build a conversational ReAct agent where the LLM can
optionally decide to return the result of a tool call as the final answer. This
is useful in cases where you have tools that can sometimes generate responses
that are acceptable as final answers, and you want to use the LLM to determine
when that is the case

## Setup

First we need to install the required packages:

```bash
yarn add @langchain/langgraph @langchain/openai
```

Next, we need to set API keys for OpenAI (the LLM we will use). Optionally, we
can set API key for [LangSmith tracing](https://smith.langchain.com/), which
will give us best-in-class observability.



```typescript
// process.env.OPENAI_API_KEY = "sk_...";

// Optional, add tracing in LangSmith
// process.env.LANGCHAIN_API_KEY = "ls__..."
process.env.LANGCHAIN_CALLBACKS_BACKGROUND = "true";
process.env.LANGCHAIN_TRACING_V2 = "true";
process.env.LANGCHAIN_PROJECT = "Direct Return: LangGraphJS";
```

    Direct Return: LangGraphJS


## Set up the tools

We will first define the tools we want to use. For this simple example, we will
use a simple placeholder "search engine". However, it is really easy to create
your own tools - see documentation
[here](https://js.langchain.com/docs/modules/agents/tools/dynamic) on how to do
that.

To add a 'return_direct' option, we will create a custom zod schema to use
**instead of** the schema that would be automatically inferred by the tool.



```typescript
import { DynamicStructuredTool } from "@langchain/core/tools";
import { z } from "zod";

const SearchTool = z.object({
  query: z.string().describe("query to look up online"),
  // **IMPORTANT** We are adding an **extra** field here
  // that isn't used directly by the tool - it's used by our
  // graph instead to determine whether or not to return the
  // result directly to the user
  return_direct: z.boolean()
    .describe(
      "Whether or not the result of this should be returned directly to the user without you seeing what it is",
    )
    .default(false),
});

const searchTool = new DynamicStructuredTool({
  name: "search",
  description: "Call to surf the web.",
  // We are overriding the default schema here to
  // add an extra field
  schema: SearchTool,
  func: async ({}: { query: string }) => {
    // This is a placeholder for the actual implementation
    // Don't let the LLM know this though 😊
    return "It's sunny in San Francisco, but you better look out if you're a Gemini 😈.";
  },
});

const tools = [searchTool];
```

We can now wrap these tools in a simple ToolExecutor.\
This is a real simple class that takes in a ToolInvocation and calls that tool,
returning the output. A ToolInvocation is any type with `tool` and `toolInput`
attribute.



```typescript
import { ToolNode } from "@langchain/langgraph/prebuilt";
import { BaseMessage } from "@langchain/core/messages";

const toolNode = new ToolNode<{ messages: BaseMessage[] }>(tools);
```

## Set up the modelCOnfig

Now we need to load the chat modelCOnfig we want to use.\
Importantly, this should satisfy two criteria:

1. It should work with messages. We will represent all agent state in the form
   of messages, so it needs to be able to work well with them.
2. It should support
   [tool calling](https://js.langchain.com/v0.2/docs/concepts/#functiontool-calling).

Note: these modelCOnfig requirements are not requirements for using LangGraph - they
are just requirements for this one example.



```typescript
import { ChatOpenAI } from "@langchain/openai";

const modelCOnfig = new ChatOpenAI({
  temperature: 0,
  modelCOnfig: "gpt-3.5-turbo",
});
// This formats the tools as json schema for the modelCOnfig API.
// The modelCOnfig then uses this like a system prompt.
const boundModel = modelCOnfig.bindTools(tools);
```

## Define the agent state

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
   called b. If the agent said that it was finished, then it should finish
2. Normal Edge: after the tools are invoked, it should always go back to the
   agent to decide what to do next

Let's define the nodes, as well as a function to decide how what conditional
edge to take.



```typescript
import { RunnableConfig } from "@langchain/core/runnables";
import { END } from "@langchain/langgraph";
import { AIMessage } from "@langchain/core/messages";

// Define the function that determines whether to continue or not
const shouldContinue = (state: typeof AgentState.State) => {
  const { messages } = state;
  const lastMessage = messages[messages.length - 1] as AIMessage;
  // If there is no function call, then we finish
  if (!lastMessage?.tool_calls?.length) {
    return END;
  } // Otherwise if there is, we check if it's suppose to return direct
  else {
    const args = lastMessage.tool_calls[0].args;
    if (args?.return_direct) {
      return "final";
    } else {
      return "tools";
    }
  }
};

// Define the function that calls the modelCOnfig
const callModel = async (state: typeof AgentState.State, config?: RunnableConfig) => {
  const messages = state.messages;
  const response = await boundModel.invoke(messages, config);
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
  // Define the two nodes we will cycle between
  .addNode("agent", callModel)
  // Note the "action" and "final" nodes are identical!
  .addNode("tools", toolNode)
  .addNode("final", toolNode)
  // Set the entrypoint as `agent`
  .addEdge(START, "agent")
  // We now add a conditional edge
  .addConditionalEdges(
    // First, we define the start node. We use `agent`.
    "agent",
    // Next, we pass in the function that will determine which node is called next.
    shouldContinue,
  )
  // We now add a normal edge from `tools` to `agent`.
  .addEdge("tools", "agent")
  .addEdge("final", END);

// Finally, we compile it!
const app = workflow.compile();
```

## Use it!

We can now use it! This now exposes the
[same interface](https://js.langchain.com/docs/expression_language/) as all
other LangChain runnables.



```typescript
import { HumanMessage, isAIMessage } from "@langchain/core/messages";

const prettyPrint = (message: BaseMessage) => {
  let txt = `[${message._getType()}]: ${message.content}`;
  if (
    isAIMessage(message) && (message as AIMessage)?.tool_calls?.length || 0 > 0
  ) {
    const tool_calls = (message as AIMessage)?.tool_calls
      ?.map((tc) => `- ${tc.name}(${JSON.stringify(tc.args)})`)
      .join("\n");
    txt += ` \nTools: \n${tool_calls}`;
  }
  console.log(txt);
};

const inputs = { messages: [new HumanMessage("what is the weather in sf")] };
for await (const output of await app.stream(inputs, { streamMode: "values" })) {
  const lastMessage = output.messages[output.messages.length - 1];
  prettyPrint(lastMessage);
  console.log("-----\n");
}
```

    [human]: what is the weather in sf
    -----
    
    [ai]:  
    Tools: 
    - search({"query":"weather in San Francisco"})
    -----
    
    [tool]: It's sunny in San Francisco, but you better look out if you're a Gemini 😈.
    -----
    
    [ai]: The weather in San Francisco is sunny.
    -----
    



```typescript
const inputs2 = {
  messages: [
    new HumanMessage(
      "what is the weather in sf? return this result directly by setting return_direct = True",
    ),
  ],
};
for await (
  const output of await app.stream(inputs2, { streamMode: "values" })
) {
  const lastMessage = output.messages[output.messages.length - 1];
  prettyPrint(lastMessage);
  console.log("-----\n");
}
```

    [human]: what is the weather in sf? return this result directly by setting return_direct = True
    -----
    
    [ai]:  
    Tools: 
    - search({"query":"weather in San Francisco","return_direct":true})
    -----
    
    [tool]: It's sunny in San Francisco, but you better look out if you're a Gemini 😈.
    -----
    


Done! The graph **stopped** after running the `tools` node!

```
```
