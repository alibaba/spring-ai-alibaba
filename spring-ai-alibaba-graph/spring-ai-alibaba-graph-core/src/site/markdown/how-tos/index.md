---
hide:
  - toc
---

# How-to guides

Welcome to the LangGraph4j how-to Guides! These guides provide practical, step-by-step instructions for accomplishing key tasks in LangGraph4j.

## Installation

**Maven**
```xml
<dependency>
    <groupId>org.bsc.langgraph4j</groupId>
    <artifactId>langgraph4j-core-jdk8</artifactId>
    <version>1.0-rc2</version>
</dependency>
```

## Controllability

LangGraph4j is known for being a highly controllable agent framework.
These how-to guides show how to achieve that controllability.
> _TO DO_
<!-- 
- [How to define graph state](define-state.html)
- [How to create subgraphs](subgraph.html)
- [How to create branches for parallel execution](branching.html)
- [How to create map-reduce branches for parallel execution](map-reduce.html) 
-->

## Persistence

LangGraph4j makes it easy to persist state across graph runs. The guides below shows how to add persistence to your graph.
> _TO DO_
<!-- 
- [How to add persistence ("memory") to your graph](persistence.html)
- [How to manage conversation history](manage-conversation-history.html)
- [How to view and update past graph state](time-travel.html)
- [How to create a custom checkpointer using Postgres](persistence-postgres.html)
- [How to delete messages](delete-messages.html)
- [How to add summary of the conversation history](add-summary-conversation-history.html)
 -->

## Human-in-the-loop

One of LangGraph4j's main benefits is that it makes human-in-the-loop workflows easy.
These guides cover common examples of that.
> _TO DO_
<!-- 
- [How to add breakpoints](breakpoints.html)
- [How to add dynamic breakpoints](dynamic_breakpoints.html)
- [How to wait for user input](wait-user-input.html)
- [How to edit graph state](edit-graph-state.html)
 -->
## Streaming

LangGraph4j is built to be streaming first.
These guides show how to use different streaming modes.
> _TO DO_
<!-- 
- [How to stream full state of your graph](stream-values.html)
- [How to stream state updates of your graph](stream-updates.html)
- [How to stream LLM tokens](stream-tokens.html)
- [How to stream LLM tokens without LangChain models](streaming-tokens-without-langchain.html)
- [How to stream events from within a tool](streaming-events-from-within-tools.html)
- [How to stream from the final node](streaming-from-final-node.html)
 -->

<!-- 
## Tool calling

- [How to call tools using ToolNode](tool-calling.html)
- [How to force an agent to call a tool](force-calling-a-tool-first.html)
- [How to handle tool calling errors](tool-calling-errors.html)
 -->

<!-- 
## Other

- [How to add runtime configuration to your graph](configuration.html)
- [How to let agent return tool results directly](dynamically-returning-directly.html)
- [How to have agent respond in structured format](respond-in-format.html)
- [How to manage agent steps](managing-agent-steps.html)
 -->

***

## References

[Langgraph.js How-to Guides](https://langchain-ai.github.io/langgraphjs/how-tos/)
