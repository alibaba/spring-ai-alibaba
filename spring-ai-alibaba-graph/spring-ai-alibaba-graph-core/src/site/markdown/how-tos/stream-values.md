# How to stream full state of your graph

LangGraph supports multiple streaming modes. The main ones are:

- `values`: This streaming mode streams back values of the graph. This is the
  **full state of the graph** after each node is called.
- `updates`: This streaming mode streams back updates to the graph. This is the
  **update to the state of the graph** after each node is called.

This guide covers `streamMode="values"`.


```typescript
// process.env.OPENAI_API_KEY = "sk-...";
```

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
  func: async ({ query: _query }: { query: string }) => {
    // This is a placeholder for the actual implementation
    return "Cold, with a low of 3℃";
  },
});

await searchTool.invoke({ query: "What's the weather like?" });

const tools = [searchTool];
```

We can now wrap these tools in a simple
[ToolNode](/langgraphjs/reference/classes/langgraph_prebuilt.ToolNode.html).
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

We can now put it all together.


```typescript
import { END, START, StateGraph } from "@langchain/langgraph";
import { AIMessage } from "@langchain/core/messages";
import { RunnableConfig } from "@langchain/core/runnables";

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
  // For versions of @langchain/core < 0.2.3, you must call `.stream()`
  // and aggregate the message from chunks instead of calling `.invoke()`.
  const { messages } = state;
  const responseMessage = await boundModel.invoke(messages, config);
  return { messages: [responseMessage] };
};

const workflow = new StateGraph(GraphState)
  .addNode("agent", callModel)
  .addNode("tools", toolNode)
  .addEdge(START, "agent")
  .addConditionalEdges("agent", routeMessage)
  .addEdge("tools", "agent");

const graph = workflow.compile();
```

## Stream values

We can now interact with the agent. Between interactions you can get and update
state.



```typescript
let inputs = { messages: [["user", "what's the weather in sf"]] };

for await (
  const chunk of await graph.stream(inputs, {
    streamMode: "values",
  })
) {
  console.log(chunk["messages"]);
  console.log("\n====\n");
}
```

    [ [ 'user', "what's the weather in sf" ] ]
    
    ====
    
    [
      [ 'user', "what's the weather in sf" ],
      AIMessage {
        "id": "chatcmpl-9y660d49eLzT7DZeBk2ZmX8C5f0LU",
        "content": "",
        "additional_kwargs": {
          "tool_calls": [
            {
              "id": "call_iD5Wk4vPsTckffDKJpEQaMkg",
              "type": "function",
              "function": "[Object]"
            }
          ]
        },
        "response_metadata": {
          "tokenUsage": {
            "completionTokens": 17,
            "promptTokens": 70,
            "totalTokens": 87
          },
          "finish_reason": "tool_calls",
          "system_fingerprint": "fp_3aa7262c27"
        },
        "tool_calls": [
          {
            "name": "search",
            "args": {
              "query": "current weather in San Francisco"
            },
            "type": "tool_call",
            "id": "call_iD5Wk4vPsTckffDKJpEQaMkg"
          }
        ],
        "invalid_tool_calls": [],
        "usage_metadata": {
          "input_tokens": 70,
          "output_tokens": 17,
          "total_tokens": 87
        }
      }
    ]
    
    ====
    
    [
      [ 'user', "what's the weather in sf" ],
      AIMessage {
        "id": "chatcmpl-9y660d49eLzT7DZeBk2ZmX8C5f0LU",
        "content": "",
        "additional_kwargs": {
          "tool_calls": [
            {
              "id": "call_iD5Wk4vPsTckffDKJpEQaMkg",
              "type": "function",
              "function": "[Object]"
            }
          ]
        },
        "response_metadata": {
          "tokenUsage": {
            "completionTokens": 17,
            "promptTokens": 70,
            "totalTokens": 87
          },
          "finish_reason": "tool_calls",
          "system_fingerprint": "fp_3aa7262c27"
        },
        "tool_calls": [
          {
            "name": "search",
            "args": {
              "query": "current weather in San Francisco"
            },
            "type": "tool_call",
            "id": "call_iD5Wk4vPsTckffDKJpEQaMkg"
          }
        ],
        "invalid_tool_calls": [],
        "usage_metadata": {
          "input_tokens": 70,
          "output_tokens": 17,
          "total_tokens": 87
        }
      },
      ToolMessage {
        "content": "Cold, with a low of 3℃",
        "name": "search",
        "additional_kwargs": {},
        "response_metadata": {},
        "tool_call_id": "call_iD5Wk4vPsTckffDKJpEQaMkg"
      }
    ]
    
    ====
    
    [
      [ 'user', "what's the weather in sf" ],
      AIMessage {
        "id": "chatcmpl-9y660d49eLzT7DZeBk2ZmX8C5f0LU",
        "content": "",
        "additional_kwargs": {
          "tool_calls": [
            {
              "id": "call_iD5Wk4vPsTckffDKJpEQaMkg",
              "type": "function",
              "function": "[Object]"
            }
          ]
        },
        "response_metadata": {
          "tokenUsage": {
            "completionTokens": 17,
            "promptTokens": 70,
            "totalTokens": 87
          },
          "finish_reason": "tool_calls",
          "system_fingerprint": "fp_3aa7262c27"
        },
        "tool_calls": [
          {
            "name": "search",
            "args": {
              "query": "current weather in San Francisco"
            },
            "type": "tool_call",
            "id": "call_iD5Wk4vPsTckffDKJpEQaMkg"
          }
        ],
        "invalid_tool_calls": [],
        "usage_metadata": {
          "input_tokens": 70,
          "output_tokens": 17,
          "total_tokens": 87
        }
      },
      ToolMessage {
        "content": "Cold, with a low of 3℃",
        "name": "search",
        "additional_kwargs": {},
        "response_metadata": {},
        "tool_call_id": "call_iD5Wk4vPsTckffDKJpEQaMkg"
      },
      AIMessage {
        "id": "chatcmpl-9y660ZKNXvziVJze0X5aTlZ5IoN35",
        "content": "Currently, in San Francisco, it's cold with a temperature of around 3℃ (37.4°F).",
        "additional_kwargs": {},
        "response_metadata": {
          "tokenUsage": {
            "completionTokens": 23,
            "promptTokens": 103,
            "totalTokens": 126
          },
          "finish_reason": "stop",
          "system_fingerprint": "fp_3aa7262c27"
        },
        "tool_calls": [],
        "invalid_tool_calls": [],
        "usage_metadata": {
          "input_tokens": 103,
          "output_tokens": 23,
          "total_tokens": 126
        }
      }
    ]
    
    ====
    

