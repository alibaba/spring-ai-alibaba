# How to force an agent to call a tool

In this example we will build a ReAct agent that **always** calls a certain tool
first, before making any plans. In this example, we will create an agent with a
search tool. However, at the start we will force the agent to call the search
tool (and then let it do whatever it wants after). This is useful when you know
you want to execute specific actions in your application but also want the
flexibility of letting the LLM follow up on the user's query after going through
that fixed sequence.

## Setup

First we need to install the packages required

```bash
yarn add  @langchain/langgraph @langchain/openai
```

Next, we need to set API keys for OpenAI (the LLM we will use). Optionally, we
can set API key for [LangSmith tracing](https://smith.langchain.com/), which
will give us best-in-class observability.


```typescript
// process.env.OPENAI_API_KEY = "sk_...";

// Optional, add tracing in LangSmith
// process.env.LANGCHAIN_API_KEY = "ls__...";
// process.env.LANGCHAIN_CALLBACKS_BACKGROUND = "true";
process.env.LANGCHAIN_TRACING_V2 = "true";
process.env.LANGCHAIN_PROJECT = "Force Calling a Tool First: LangGraphJS";
```

    Force Calling a Tool First: LangGraphJS


## Set up the tools

We will first define the tools we want to use. For this simple example, we will
use a built-in search tool via Tavily. However, it is really easy to create your
own tools - see documentation
[here](https://js.langchain.com/docs/modules/agents/tools/dynamic) on how to do
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

We can now wrap these tools in a simple ToolExecutor. This is a real simple
class that takes in a ToolInvocation and calls that tool, returning the output.
A ToolInvocation is any type with `tool` and `toolInput` attribute.


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
2. It should work with OpenAI function calling. This means it should either be
   an OpenAI modelCOnfig or a modelCOnfig that exposes a similar interface.

Note: these modelCOnfig requirements are not requirements for using LangGraph - they
are just requirements for this one example.


```typescript
import { ChatOpenAI } from "@langchain/openai";

const modelCOnfig = new ChatOpenAI({
  temperature: 0,
  modelCOnfig: "gpt-4o",
});
```

After we've done this, we should make sure the modelCOnfig knows that it has these
tools available to call. We can do this by converting the LangChain tools into
the format for OpenAI function calling, and then bind them to the modelCOnfig class.


```typescript
const boundModel = modelCOnfig.bindTools(tools);
```

## Define the agent state

The main type of graph in `langgraph` is the `StateGraph`. This graph is
parameterized by a state object that it passes around to each node. Each node
then returns operations to update that state.

For this example, the state we will track will just be a list of messages. We
want each node to just add messages to that list. Therefore, we will define the
agent state as an object with one key (`messages`) with the value specifying how
to update the state.


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
   called\
   b. If the agent said that it was finished, then it should finish
2. Normal Edge: after the tools are invoked, it should always go back to the
   agent to decide what to do next

Let's define the nodes, as well as a function to decide how what conditional
edge to take.


```typescript
import { AIMessage, AIMessageChunk } from "@langchain/core/messages";
import { RunnableConfig } from "@langchain/core/runnables";
import { concat } from "@langchain/core/utils/stream";

// Define logic that will be used to determine which conditional edge to go down
const shouldContinue = (state: typeof AgentState.State) => {
  const { messages } = state;
  const lastMessage = messages[messages.length - 1] as AIMessage;
  // If there is no function call, then we finish
  if (!lastMessage.tool_calls || lastMessage.tool_calls.length === 0) {
    return "end";
  }
  // Otherwise if there is, we continue
  return "continue";
};

// Define the function that calls the modelCOnfig
const callModel = async (
  state: typeof AgentState.State,
  config?: RunnableConfig,
) => {
  const { messages } = state;
  let response: AIMessageChunk | undefined;
  for await (const message of await boundModel.stream(messages, config)) {
    if (!response) {
      response = message;
    } else {
      response = concat(response, message);
    }
  }
  // We return an object, because this will get added to the existing list
  return {
    messages: response ? [response as AIMessage] : [],
  };
};
```

**MODIFICATION**

Here we create a node that returns an AIMessage with a tool call - we will use
this at the start to force it call a tool


```typescript
// This is the new first - the first call of the modelCOnfig we want to explicitly hard-code some action
const firstModel = async (state: typeof AgentState.State) => {
  const humanInput = state.messages[state.messages.length - 1].content || "";
  return {
    messages: [
      new AIMessage({
        content: "",
        tool_calls: [
          {
            name: "search",
            args: {
              query: humanInput,
            },
            id: "tool_abcd123",
          },
        ],
      }),
    ],
  };
};
```

## Define the graph

We can now put it all together and define the graph!

**MODIFICATION**

We will define a `firstModel` node which we will set as the entrypoint.



```typescript
import { END, START, StateGraph } from "@langchain/langgraph";

// Define a new graph
const workflow = new StateGraph(AgentState)
  // Define the new entrypoint
  .addNode("first_agent", firstModel)
  // Define the two nodes we will cycle between
  .addNode("agent", callModel)
  .addNode("action", toolNode)
  // Set the entrypoint as `first_agent`
  // by creating an edge from the virtual __start__ node to `first_agent`
  .addEdge(START, "first_agent")
  // We now add a conditional edge
  .addConditionalEdges(
    // First, we define the start node. We use `agent`.
    // This means these are the edges taken after the `agent` node is called.
    "agent",
    // Next, we pass in the function that will determine which node is called next.
    shouldContinue,
    // Finally we pass in a mapping.
    // The keys are strings, and the values are other nodes.
    // END is a special node marking that the graph should finish.
    // What will happen is we will call `should_continue`, and then the output of that
    // will be matched against the keys in this mapping.
    // Based on which one it matches, that node will then be called.
    {
      // If `tools`, then we call the tool node.
      continue: "action",
      // Otherwise we finish.
      end: END,
    },
  )
  // We now add a normal edge from `tools` to `agent`.
  // This means that after `tools` is called, `agent` node is called next.
  .addEdge("action", "agent")
  // After we call the first agent, we know we want to go to action
  .addEdge("first_agent", "action");

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
import { HumanMessage } from "@langchain/core/messages";

const inputs = {
  messages: [new HumanMessage("what is the weather in sf")],
};

for await (const output of await app.stream(inputs)) {
  console.log(output);
  console.log("-----\n");
}
```

    {
      first_agent: {
        messages: [
          AIMessage {
            "content": "",
            "additional_kwargs": {},
            "response_metadata": {},
            "tool_calls": [
              {
                "name": "search",
                "args": {
                  "query": "what is the weather in sf"
                },
                "id": "tool_abcd123"
              }
            ],
            "invalid_tool_calls": []
          }
        ]
      }
    }
    -----
    
    {
      action: {
        messages: [
          ToolMessage {
            "content": "Cold, with a low of 13 ℃",
            "name": "search",
            "additional_kwargs": {},
            "response_metadata": {},
            "tool_call_id": "tool_abcd123"
          }
        ]
      }
    }
    -----
    
    {
      agent: {
        messages: [
          AIMessageChunk {
            "id": "chatcmpl-9y562g16z0MUNBJcS6nKMsDuFMRsS",
            "content": "The current weather in San Francisco is cold, with a low of 13°C.",
            "additional_kwargs": {},
            "response_metadata": {
              "prompt": 0,
              "completion": 0,
              "finish_reason": "stop",
              "system_fingerprint": "fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27fp_3aa7262c27"
            },
            "tool_calls": [],
            "tool_call_chunks": [],
            "invalid_tool_calls": [],
            "usage_metadata": {
              "input_tokens": 104,
              "output_tokens": 18,
              "total_tokens": 122
            }
          }
        ]
      }
    }
    -----
    

