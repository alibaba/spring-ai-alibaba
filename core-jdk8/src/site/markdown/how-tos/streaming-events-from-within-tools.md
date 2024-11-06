# How to stream events from within a tool

If your LangGraph graph needs to use tools that call LLMs (or any other LangChain `Runnable` objects -- other graphs, LCEL chains, retrievers, etc.), you might want to stream events from the underlying `Runnable`. This guide shows how you can do that.

## Setup

```bash
npm install @langchain/langgraph @langchain/anthropic zod
```

```typescript
process.env.ANTHROPIC_API_KEY = 'YOUR_API_KEY'
```

## Define graph and tools

We'll use a prebuilt ReAct agent for this guide


```typescript
import { z } from "zod";
import { tool } from "@langchain/core/tools";
import { ChatPromptTemplate } from "@langchain/core/prompts";
import { ChatAnthropic } from "@langchain/anthropic";

const model = new ChatAnthropic({
  model: "claude-3-5-sonnet-20240620",
  temperature: 0,
});

const getItems = tool(
  async (input, config) => {
    const template = ChatPromptTemplate.fromMessages([
      [
        "human",
        "Can you tell me what kind of items i might find in the following place: '{place}'. " +
          "List at least 3 such items separating them by a comma. And include a brief description of each item..",
      ],
    ]);

    const modelWithConfig = model.withConfig({
      runName: "Get Items LLM",
      tags: ["tool_llm"],
    });

    const chain = template.pipe(modelWithConfig);
    const result = await chain.invoke(input, config);
    return result.content;
  },
  {
    name: "get_items",
    description: "Use this tool to look up which items are in the given place.",
    schema: z.object({
      place: z.string().describe("The place to look up items for. E.g 'shelf'"),
    }),
  }
);
```

We're adding a custom tag (`tool_llm`) to our LLM runnable within the tool. This will allow us to filter events that we'll stream from the compiled graph (`agent`) Runnable below


```typescript
import { createReactAgent } from "@langchain/langgraph/prebuilt";

const agent = createReactAgent({
  llm: model,
  tools: [getItems],
});
```

## Stream events from the graph


```typescript
let finalEvent;

for await (const event of agent.streamEvents(
  {
    messages: [
      [
        "human",
        "what items are on the shelf? You should call the get_items tool.",
      ],
    ],
  },
  {
    version: "v2",
  },
  {
    includeTags: ["tool_llm"],
  }
)) {
  if ("chunk" in event.data) {
    console.dir({
      type: event.data.chunk._getType(),
      content: event.data.chunk.content,
    })
  }
  finalEvent = event;
}
```

    { type: 'ai', content: 'Here' }
    { type: 'ai', content: ' are three items you might' }
    { type: 'ai', content: ' find on a shelf,' }
    { type: 'ai', content: ' along with brief' }
    { type: 'ai', content: ' descriptions:\n\n1.' }
    { type: 'ai', content: ' Books' }
    { type: 'ai', content: ': Boun' }
    { type: 'ai', content: 'd collections of printe' }
    { type: 'ai', content: 'd pages' }
    { type: 'ai', content: ' containing' }
    { type: 'ai', content: ' various' }
    { type: 'ai', content: ' forms' }
    { type: 'ai', content: ' of literature, information' }
    { type: 'ai', content: ', or reference' }
    { type: 'ai', content: ' material.\n\n2.' }
    { type: 'ai', content: ' Picture' }
    { type: 'ai', content: ' frames: Decorative' }
    { type: 'ai', content: ' borders' }
    { type: 'ai', content: ' used to display an' }
    { type: 'ai', content: 'd protect photographs, artwork' }
    { type: 'ai', content: ', or other visual memor' }
    { type: 'ai', content: 'abilia.\n\n3' }
    { type: 'ai', content: '. Pot' }
    { type: 'ai', content: 'ted plants: Small' }
    { type: 'ai', content: ' indoor' }
    { type: 'ai', content: ' plants in' }
    { type: 'ai', content: ' containers, often used for' }
    { type: 'ai', content: ' decoration or to add a' }
    { type: 'ai', content: ' touch of nature to indoor' }
    { type: 'ai', content: ' spaces.' }


Let's inspect the last event to get the final list of messages from the agent


```typescript
const finalMessage = finalEvent?.data.output;
console.dir(
  {
    type: finalMessage._getType(),
    content: finalMessage.content,
    tool_calls: finalMessage.tool_calls,
  },
  { depth: null }
);

```

    {
      type: 'ai',
      content: 'Here are three items you might find on a shelf, along with brief descriptions:\n' +
        '\n' +
        '1. Books: Bound collections of printed pages containing various forms of literature, information, or reference material.\n' +
        '\n' +
        '2. Picture frames: Decorative borders used to display and protect photographs, artwork, or other visual memorabilia.\n' +
        '\n' +
        '3. Potted plants: Small indoor plants in containers, often used for decoration or to add a touch of nature to indoor spaces.',
      tool_calls: []
    }


You can see that the content of the `ToolMessage` is the same as the output we streamed above
