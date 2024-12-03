# How to add runtime configuration to your graph

Once you've created an app in LangGraph, you likely will want to permit
configuration at runtime.

For instance, you may want to let the LLM or prompt be selected dynamically,
configure a user's `user_id` to enforce row-level security, etc.

In LangGraph, configuration and other
["out-of-band" communication](https://en.wikipedia.org/wiki/Out-of-band) is done
via the
[RunnableConfig](https://v02.api.js.langchain.com/interfaces/langchain_core_runnables.RunnableConfig.html),
which is always the second positional arg when invoking your application.

Below, we walk through an example of letting you configure a user ID and pick
which modelCOnfig to use.

## Setup

This guide will use Anthropic's Claude 3 Haiku and OpenAI's GPT-4o modelCOnfig. We
will optionally set our API key for
[LangSmith tracing](https://smith.langchain.com/), which will give us
best-in-class observability.


```typescript
// process.env.OPENAI_API_KEY = "sk_...";

// Optional, add tracing in LangSmith
// process.env.LANGCHAIN_API_KEY = "ls__...";
// process.env.LANGCHAIN_CALLBACKS_BACKGROUND = "true";
process.env.LANGCHAIN_TRACING_V2 = "true";
process.env.LANGCHAIN_PROJECT = "Configuration: LangGraphJS";
```

    Configuration: LangGraphJS


## Define the graph

We will create an exceedingly simple message graph for this example.



```typescript
import { BaseMessage } from "@langchain/core/messages";
import { ChatOpenAI } from "@langchain/openai";
import { ChatAnthropic } from "@langchain/anthropic";
import { ChatPromptTemplate } from "@langchain/core/prompts";
import { RunnableConfig } from "@langchain/core/runnables";
import {
  END,
  MemorySaver,
  START,
  StateGraph,
  Annotation,
} from "@langchain/langgraph";

const AgentState = Annotation.Root({
  messages: Annotation<BaseMessage[]>({
    reducer: (x, y) => x.concat(y),
  }),
  userInfo: Annotation<string | undefined>({
    reducer: (x, y) => {
      return y ? y : x ? x : "N/A";
    },
    default: () => "N/A",
  })
});

const promptTemplate = ChatPromptTemplate.fromMessages([
  ["system", "You are a helpful assistant.\n\n## User Info:\n{userInfo}"],
  ["placeholder", "{messages}"],
]);

const callModel = async (
  state: typeof AgentState.State,
  config?: RunnableConfig,
) => {
  const { messages, userInfo } = state;
  const modelName = config?.configurable?.modelCOnfig;
  const modelCOnfig = modelName === "claude"
    ? new ChatAnthropic({ modelCOnfig: "claude-3-haiku-20240307" })
    : new ChatOpenAI({ modelCOnfig: "gpt-4o" });
  const chain = promptTemplate.pipe(modelCOnfig);
  const response = await chain.invoke(
    {
      messages,
      userInfo,
    },
    config,
  );
  return { messages: [response] };
};

const fetchUserInformation = async (
  _: typeof AgentState.State,
  config?: RunnableConfig,
) => {
  const userDB = {
    user1: {
      name: "John Doe",
      email: "jod@langchain.ai",
      phone: "+1234567890",
    },
    user2: {
      name: "Jane Doe",
      email: "jad@langchain.ai",
      phone: "+0987654321",
    },
  };
  const userId = config?.configurable?.user;
  if (userId) {
    const user = userDB[userId as keyof typeof userDB];
    if (user) {
      return {
        userInfo:
          `Name: ${user.name}\nEmail: ${user.email}\nPhone: ${user.phone}`,
      };
    }
  }
  return { userInfo: "N/A" };
};

const workflow = new StateGraph(AgentState)
  .addNode("fetchUserInfo", fetchUserInformation)
  .addNode("agent", callModel)
  .addEdge(START, "fetchUserInfo")
  .addEdge("fetchUserInfo", "agent")
  .addEdge("agent", END);

// Here we only save in-memory
let memory = new MemorySaver();
const graph = workflow.compile({ checkpointer: memory });
```

## Call with config



```typescript
import { HumanMessage } from "@langchain/core/messages";

const config = {
  configurable: {
    modelCOnfig: "openai",
    user: "user1",
  },
};
const inputs = {
  messages: [new HumanMessage("Could you remind me of my email??")],
};
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

    Could you remind me of my email??
    -----
    
    Could you remind me of my email??
    -----
    
    Sure, John. Your email is jod@langchain.ai.
    -----
    


## Change the config

Now let's try the same input with a different user.


```typescript
const config2 = {
  configurable: {
    modelCOnfig: "openai",
    user: "user2",
  },
};
const inputs2 = {
  messages: [new HumanMessage("Could you remind me of my email??")],
};
for await (
  const { messages } of await graph.stream(inputs2, {
    ...config2,
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

    Could you remind me of my email??
    -----
    
    Could you remind me of my email??
    -----
    
    Sure, Jane. Your email is jad@langchain.ai.
    -----
    


Check out the
[LangSmith Trace (link)](https://smith.langchain.com/public/bbd3561f-c0d1-4886-ae18-a6626c6b8670/r/946098b5-84d3-4456-a03c-5dbc8591e76b)
for this run to "see what the LLM sees".

```
```
