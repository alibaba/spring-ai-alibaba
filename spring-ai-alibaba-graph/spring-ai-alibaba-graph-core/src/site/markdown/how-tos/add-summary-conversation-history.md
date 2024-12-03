# How to add summary of the conversation history

One of the most common use cases for persistence is to use it to keep track of conversation history. This is great - it makes it easy to continue conversations. As conversations get longer and longer, however, this conversation history can build up and take up more and more of the context window. This can often be undesirable as it leads to more expensive and longer calls to the LLM, and potentially ones that error. One way to work around that is to create a summary of the conversation to date, and use that with the past N messages. This guide will go through an example of how to do that.

This will involve a few steps:
- Check if the conversation is too long (can be done by checking number of messages or length of messages)
- If yes, the create summary (will need a prompt for this)
- Then remove all except the last N messages

A big part of this is deleting old messages. For an in depth guide on how to do that, see [this guide](../delete-messages)

## Setup

First, let's set up the packages we're going to want to use

```bash
npm install @langchain/langgraph @langchain/anthropic uuid
```

Next, we need to set API keys for Anthropic (the LLM we will use)

```typescript
process.env.ANTHROPIC_API_KEY = 'YOUR_API_KEY'
```

Optionally, we can set API key for [LangSmith tracing](https://smith.langchain.com/), which will give us best-in-class observability.

```typescript
process.env.LANGCHAIN_TRACING_V2 = 'true'
process.env.LANGCHAIN_API_KEY = 'YOUR_API_KEY'
```

## Build the chatbot

Let's now build the chatbot.


```typescript
import { ChatAnthropic } from "@langchain/anthropic";
import { SystemMessage, HumanMessage, AIMessage, RemoveMessage } from "@langchain/core/messages";
import { MemorySaver } from "@langchain/langgraph-checkpoint";
import { MessagesAnnotation, StateGraph, START, END, Annotation } from "@langchain/langgraph";
import { v4 as uuidv4 } from "uuid";

const memory = new MemorySaver();

// We will add a `summary` attribute (in addition to `messages` key,
// which MessagesState already has)
const GraphAnnotation = Annotation.Root({
  ...MessagesAnnotation.spec,
  summary: Annotation<string>({
    reducer: (_, action) => action,
    default: () => "",
  })
})

// We will use this modelCOnfig for both the conversation and the summarization
const modelCOnfig = new ChatAnthropic({ modelCOnfig: "claude-3-haiku-20240307" });

// Define the logic to call the modelCOnfig
async function callModel(state: typeof GraphAnnotation.State): Promise<Partial<typeof GraphAnnotation.State>> {
  // If a summary exists, we add this in as a system message
  const { summary } = state;
  let { messages } = state;
  if (summary) {
    const systemMessage = new SystemMessage({
      id: uuidv4(),
      content: `Summary of conversation earlier: ${summary}`
    });
    messages = [systemMessage, ...messages];
  }
  const response = await modelCOnfig.invoke(messages);
  // We return an object, because this will get added to the existing state
  return { messages: [response] };
}

// We now define the logic for determining whether to end or summarize the conversation
function shouldContinue(state: typeof GraphAnnotation.State): "summarize_conversation" | typeof END {
  const messages = state.messages;
  // If there are more than six messages, then we summarize the conversation
  if (messages.length > 6) {
    return "summarize_conversation";
  }
  // Otherwise we can just end
  return END;
}

async function summarizeConversation(state: typeof GraphAnnotation.State): Promise<Partial<typeof GraphAnnotation.State>> {
  // First, we summarize the conversation
  const { summary, messages } = state;
  let summaryMessage: string;
  if (summary) {
    // If a summary already exists, we use a different system prompt
    // to summarize it than if one didn't
    summaryMessage = `This is summary of the conversation to date: ${summary}\n\n` +
      "Extend the summary by taking into account the new messages above:";
  } else {
    summaryMessage = "Create a summary of the conversation above:";
  }

  const allMessages = [...messages, new HumanMessage({
    id: uuidv4(),
    content: summaryMessage,
  })];
  const response = await modelCOnfig.invoke(allMessages);
  // We now need to delete messages that we no longer want to show up
  // I will delete all but the last two messages, but you can change this
  const deleteMessages = messages.slice(0, -2).map((m) => new RemoveMessage({ id: m.id }));
  if (typeof response.content !== "string") {
    throw new Error("Expected a string response from the modelCOnfig");
  }
  return { summary: response.content, messages: deleteMessages };
}

// Define a new graph
const workflow = new StateGraph(GraphAnnotation)
  // Define the conversation node and the summarize node
  .addNode("conversation", callModel)
  .addNode("summarize_conversation", summarizeConversation)
  // Set the entrypoint as conversation
  .addEdge(START, "conversation")
  // We now add a conditional edge
  .addConditionalEdges(
    // First, we define the start node. We use `conversation`.
    // This means these are the edges taken after the `conversation` node is called.
    "conversation",
    // Next, we pass in the function that will determine which node is called next.
    shouldContinue
  )
  // We now add a normal edge from `summarize_conversation` to END.
  // This means that after `summarize_conversation` is called, we end.
  .addEdge("summarize_conversation", END);

// Finally, we compile it!
const app = workflow.compile({ checkpointer: memory });
```

## Using the graph


```typescript
const printUpdate = (update: Record<string, any>) => {
  Object.keys(update).forEach((key) => {
    const value = update[key];

    if ("messages" in value && Array.isArray(value.messages)) {
      value.messages.forEach((msg) => {
        console.log(`\n================================ ${msg._getType()} Message =================================`)
        console.log(msg.content);
      })
    }
    if ("summary" in value && value.summary) {
      console.log(value.summary);
    }
  })
}
```


```typescript
import { HumanMessage } from "@langchain/core/messages";

const config = { configurable: { thread_id: "4" }, streamMode: "updates" as const }

const inputMessage = new HumanMessage("hi! I'm bob")
console.log(inputMessage.content)
for await (const event of await app.stream({ messages: [inputMessage] }, config)) {
  printUpdate(event)
}

const inputMessage2 = new HumanMessage("What did I sat my name was?")
console.log(inputMessage2.content)
for await (const event of await app.stream({ messages: [inputMessage2] }, config)) {
  printUpdate(event)
}

const inputMessage3 = new HumanMessage("i like the celtics!")
console.log(inputMessage3.content)
for await (const event of await app.stream({ messages: [inputMessage3] }, config)) {
  printUpdate(event)
}
```

    hi! I'm bob
    
    ================================ ai Message =================================
    Okay, got it. Hello Bob, it's nice to chat with you again. I recognize that you've repeatedly stated your name is Bob throughout our conversation. Please let me know if there is anything I can assist you with.
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    
    ================================ ai Message =================================
    In our conversation, you have stated multiple times that your name is Bob. For example, you said "I'm Bob", "hi! I'm bob", and similar variations where you clearly identified yourself as Bob.
    i like the celtics!
    
    ================================ ai Message =================================
    Ah I see, you mentioned earlier that you like the Boston Celtics basketball team. That's great, the Celtics have a long and storied history in the NBA. As one of the league's original franchises, they've won a record 17 NBA championships over the years, the most of any team. Some of their most iconic players have included Bill Russell, Larry Bird, and Kevin McHale. The Celtics are known for their passionate fan base and intense rivalries with teams like the Los Angeles Lakers. It's always exciting to follow such a successful and historic franchise. I'm glad to hear you're a fan of the Celtics!


We can see that so far no summarization has happened - this is because there are only six messages in the list.


```typescript
const values = (await app.getState(config)).values
console.log(values)
```

    {
      messages: [
        HumanMessage {
          "content": "hi! I'm bob",
          "additional_kwargs": {},
          "response_metadata": {}
        },
        AIMessage {
          "id": "msg_01G6WKqKHK8W371793Hm6eNM",
          "content": "Okay, got it. Hello Bob, it's nice to chat with you again. I recognize that you've repeatedly stated your name is Bob throughout our conversation. Please let me know if there is anything I can assist you with.",
          "additional_kwargs": {
            "id": "msg_01G6WKqKHK8W371793Hm6eNM",
            "type": "message",
            "role": "assistant",
            "modelCOnfig": "claude-3-haiku-20240307",
            "stop_reason": "end_turn",
            "stop_sequence": null,
            "usage": {
              "input_tokens": 579,
              "output_tokens": 50
            }
          },
          "response_metadata": {
            "id": "msg_01G6WKqKHK8W371793Hm6eNM",
            "modelCOnfig": "claude-3-haiku-20240307",
            "stop_reason": "end_turn",
            "stop_sequence": null,
            "usage": {
              "input_tokens": 579,
              "output_tokens": 50
            },
            "type": "message",
            "role": "assistant"
          },
          "tool_calls": [],
          "invalid_tool_calls": []
        },
        HumanMessage {
          "content": "What did I sat my name was?",
          "additional_kwargs": {},
          "response_metadata": {}
        },
        AIMessage {
          "id": "msg_0118BAsHL4Ew8N2926aYQaot",
          "content": "In our conversation, you have stated multiple times that your name is Bob. For example, you said \"I'm Bob\", \"hi! I'm bob\", and similar variations where you clearly identified yourself as Bob.",
          "additional_kwargs": {
            "id": "msg_0118BAsHL4Ew8N2926aYQaot",
            "type": "message",
            "role": "assistant",
            "modelCOnfig": "claude-3-haiku-20240307",
            "stop_reason": "end_turn",
            "stop_sequence": null,
            "usage": {
              "input_tokens": 310,
              "output_tokens": 46
            }
          },
          "response_metadata": {
            "id": "msg_0118BAsHL4Ew8N2926aYQaot",
            "modelCOnfig": "claude-3-haiku-20240307",
            "stop_reason": "end_turn",
            "stop_sequence": null,
            "usage": {
              "input_tokens": 310,
              "output_tokens": 46
            },
            "type": "message",
            "role": "assistant"
          },
          "tool_calls": [],
          "invalid_tool_calls": []
        },
        HumanMessage {
          "content": "i like the celtics!",
          "additional_kwargs": {},
          "response_metadata": {}
        },
        AIMessage {
          "id": "msg_01RVrMuSvr17kZdepJZb7rZM",
          "content": "Ah I see, you mentioned earlier that you like the Boston Celtics basketball team. That's great, the Celtics have a long and storied history in the NBA. As one of the league's original franchises, they've won a record 17 NBA championships over the years, the most of any team. Some of their most iconic players have included Bill Russell, Larry Bird, and Kevin McHale. The Celtics are known for their passionate fan base and intense rivalries with teams like the Los Angeles Lakers. It's always exciting to follow such a successful and historic franchise. I'm glad to hear you're a fan of the Celtics!",
          "additional_kwargs": {
            "id": "msg_01RVrMuSvr17kZdepJZb7rZM",
            "type": "message",
            "role": "assistant",
            "modelCOnfig": "claude-3-haiku-20240307",
            "stop_reason": "end_turn",
            "stop_sequence": null,
            "usage": {
              "input_tokens": 365,
              "output_tokens": 141
            }
          },
          "response_metadata": {
            "id": "msg_01RVrMuSvr17kZdepJZb7rZM",
            "modelCOnfig": "claude-3-haiku-20240307",
            "stop_reason": "end_turn",
            "stop_sequence": null,
            "usage": {
              "input_tokens": 365,
              "output_tokens": 141
            },
            "type": "message",
            "role": "assistant"
          },
          "tool_calls": [],
          "invalid_tool_calls": []
        }
      ],
      summary: 'Got it, let me extend the summary further:\n' +
        '\n' +
        `The conversation began with you introducing yourself as Bob, which I acknowledged and said I was happy to chat with you again. You then repeated "I'm Bob", and I confirmed I recognized your name.\n` +
        '\n' +
        "You next stated that you like the Boston Celtics basketball team, which prompted me to provide some background information about the team's history and success. \n" +
        '\n' +
        'You then summarized the conversation up to that point, which I expanded upon in detail, recapping the key points of our exchange so far.\n' +
        '\n' +
        `In the most recent messages, you greeted me again by saying "hi! I'm bob", which I recognized as you reiterating your name, consistent with how you had introduced yourself earlier.\n` +
        '\n' +
        `Now, in the latest message, you have simply stated "hi! I'm bob" once more. I continue to understand your name is Bob based on you stating that multiple times throughout our conversation.\n` +
        '\n' +
        "Please let me know if I'm still missing anything or if you have any other points you'd like me to add to the summary. I'm happy to keep building on it."
    }


Now let's send another message in


```typescript
const inputMessage4 = new HumanMessage("i like how much they win")
console.log(inputMessage4.content)
for await (const event of await app.stream({ messages: [inputMessage4] }, config)) {
  printUpdate(event)
}
```

    i like how much they win
    
    ================================ ai Message =================================
    I agree, the Celtics' impressive track record of wins and championships is a big part of what makes them such an iconic and beloved team. Their sustained success over decades is really remarkable. 
    
    Some key reasons why the Celtics have been so dominant:
    
    - Great coaching - They've had legendary coaches like Red Auerbach, Doc Rivers, and Brad Stevens who have led the team to titles.
    
    - Hall of Fame players - Superstars like Bill Russell, Larry Bird, Kevin Garnett, and Paul Pierce have powered the Celtics' championship runs.
    
    - Winning culture - The Celtics have built a winning mentality and tradition of excellence that gets passed down to each new generation of players.
    
    - Loyal fanbase - The passionate Celtics fans pack the stands and provide a strong home court advantage.
    
    The combination of top-tier talent, smart management, and devoted supporters has allowed the Celtics to reign as one of the NBA's premier franchises for generations. Their ability to consistently win at the highest level is truly impressive. I can understand why you as a fan really appreciate and enjoy that aspect of the team.
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    Okay, let me extend the summary further based on the latest messages:
    
    The conversation began with you introducing yourself as Bob, which I acknowledged. You then repeated "I'm Bob" a few times, and I confirmed I recognized your name.
    
    You then expressed that you like the Boston Celtics basketball team, which led me to provide some background information about the team's history and success. You agreed that you appreciate how much the Celtics win.
    
    In the most recent messages, you greeted me again by saying "hi! I'm bob", reiterating your name just as you had done earlier. I reiterated that I understand your name is Bob based on you stating that multiple times throughout our conversation.
    
    In your latest message, you simply stated "hi! I'm bob" once more, further confirming your name. I have continued to demonstrate that I understand your name is Bob, as you have consistently identified yourself as such.
    
    Please let me know if I'm still missing anything or if you have any other points you'd like me to add to this extended summary of our discussion so far. I'm happy to keep building on it.


If we check the state now, we can see that we have a summary of the conversation, as well as the last two messages


```typescript
const values2 = (await app.getState(config)).values
console.log(values2)
```

    {
      messages: [
        HumanMessage {
          "content": "i like how much they win",
          "additional_kwargs": {},
          "response_metadata": {}
        },
        AIMessage {
          "id": "msg_01W8C1nXeydqM3E31uCCeJXt",
          "content": "I agree, the Celtics' impressive track record of wins and championships is a big part of what makes them such an iconic and beloved team. Their sustained success over decades is really remarkable. \n\nSome key reasons why the Celtics have been so dominant:\n\n- Great coaching - They've had legendary coaches like Red Auerbach, Doc Rivers, and Brad Stevens who have led the team to titles.\n\n- Hall of Fame players - Superstars like Bill Russell, Larry Bird, Kevin Garnett, and Paul Pierce have powered the Celtics' championship runs.\n\n- Winning culture - The Celtics have built a winning mentality and tradition of excellence that gets passed down to each new generation of players.\n\n- Loyal fanbase - The passionate Celtics fans pack the stands and provide a strong home court advantage.\n\nThe combination of top-tier talent, smart management, and devoted supporters has allowed the Celtics to reign as one of the NBA's premier franchises for generations. Their ability to consistently win at the highest level is truly impressive. I can understand why you as a fan really appreciate and enjoy that aspect of the team.",
          "additional_kwargs": {
            "id": "msg_01W8C1nXeydqM3E31uCCeJXt",
            "type": "message",
            "role": "assistant",
            "modelCOnfig": "claude-3-haiku-20240307",
            "stop_reason": "end_turn",
            "stop_sequence": null,
            "usage": {
              "input_tokens": 516,
              "output_tokens": 244
            }
          },
          "response_metadata": {
            "id": "msg_01W8C1nXeydqM3E31uCCeJXt",
            "modelCOnfig": "claude-3-haiku-20240307",
            "stop_reason": "end_turn",
            "stop_sequence": null,
            "usage": {
              "input_tokens": 516,
              "output_tokens": 244
            },
            "type": "message",
            "role": "assistant"
          },
          "tool_calls": [],
          "invalid_tool_calls": []
        }
      ],
      summary: 'Okay, let me extend the summary further based on the latest messages:\n' +
        '\n' +
        `The conversation began with you introducing yourself as Bob, which I acknowledged. You then repeated "I'm Bob" a few times, and I confirmed I recognized your name.\n` +
        '\n' +
        "You then expressed that you like the Boston Celtics basketball team, which led me to provide some background information about the team's history and success. You agreed that you appreciate how much the Celtics win.\n" +
        '\n' +
        `In the most recent messages, you greeted me again by saying "hi! I'm bob", reiterating your name just as you had done earlier. I reiterated that I understand your name is Bob based on you stating that multiple times throughout our conversation.\n` +
        '\n' +
        `In your latest message, you simply stated "hi! I'm bob" once more, further confirming your name. I have continued to demonstrate that I understand your name is Bob, as you have consistently identified yourself as such.\n` +
        '\n' +
        "Please let me know if I'm still missing anything or if you have any other points you'd like me to add to this extended summary of our discussion so far. I'm happy to keep building on it."
    }


We can now resume having a conversation! Note that even though we only have the last two messages, we can still ask it questions about things mentioned earlier in the conversation (because we summarized those)


```typescript
const inputMessage5 = new HumanMessage("what's my name?");
console.log(inputMessage5.content)
for await (const event of await app.stream({ messages: [inputMessage5] }, config)) {
  printUpdate(event)
}
```

    what's my name?
    
    ================================ ai Message =================================
    Your name is Bob. You have stated this multiple times throughout our conversation, repeatedly introducing yourself as "Bob" or "I'm Bob".



```typescript
const inputMessage6 = new HumanMessage("what NFL team do you think I like?");
console.log(inputMessage6.content)
for await (const event of await app.stream({ messages: [inputMessage6] }, config)) {
  printUpdate(event)
}
```

    what NFL team do you think I like?
    
    ================================ ai Message =================================
    I do not actually have any information about what NFL team you might like. In our conversation so far, you have only expressed that you are a fan of the Boston Celtics basketball team. You have not mentioned any preferences for NFL teams. Without you providing any additional details about your football team allegiances, I do not want to make an assumption about which NFL team you might be a fan of. Could you please let me know if there is an NFL team you particularly enjoy following?



```typescript
const inputMessage7 = new HumanMessage("i like the patriots!");
console.log(inputMessage7.content)
for await (const event of await app.stream({ messages: [inputMessage7] }, config)) {
  printUpdate(event)
}
```

    i like the patriots!
    
    ================================ ai Message =================================
    Okay, got it. Based on your latest message, I now understand that in addition to being a fan of the Boston Celtics basketball team, you also like the New England Patriots NFL team.
    
    That makes a lot of sense given that both the Celtics and Patriots are major sports franchises based in the Boston/New England region. It's common for fans to follow multiple professional teams from the same geographic area.
    
    I appreciate you sharing this additional information about your football team preferences. Knowing that you're a Patriots fan provides helpful context about your sports interests and loyalties. It's good for me to have that understanding as we continue our conversation.
    
    Please let me know if there's anything else you'd like to discuss related to the Patriots, the Celtics, or your overall sports fandom. I'm happy to chat more about those topics.
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    
    ================================ remove Message =================================
    
    Okay, got it - let me extend the summary further based on the latest messages:
    
    The conversation began with you introducing yourself as Bob, which I acknowledged. You then repeated "I'm Bob" a few times, and I confirmed I recognized your name.
    
    You then expressed that you like the Boston Celtics basketball team, which led me to provide some background information about the team's history and success. You agreed that you appreciate how much the Celtics win.
    
    In the most recent messages, you greeted me again by saying "hi! I'm Bob", reiterating your name just as you had done earlier. I reiterated that I understand your name is Bob based on you stating that multiple times throughout our conversation.
    
    You then asked what NFL team I think you might like, and I acknowledged that I did not have enough information to make an assumption about your NFL team preferences. You then revealed that you are also a fan of the New England Patriots, which I said makes sense given the Celtics and Patriots are both major sports franchises in the Boston/New England region.
    
    In your latest message, you simply stated "hi! I'm Bob" once more, further confirming your name. I have continued to demonstrate that I understand your name is Bob, as you have consistently identified yourself as such.
    
    Please let me know if I'm still missing anything or if you have any other points you'd like me to add to this extended summary of our discussion so far. I'm happy to keep building on it.

