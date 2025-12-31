# Spring AI Alibaba Graph

## What's Spring AI Alibaba Graph

Spring AI Alibaba Graph is a **workflow and multi-agent framework** for Java developers to build complex applications composed of multiple AI models or steps.

Spring AI Alibaba Graph serves as the underlying core engine of the Agent Framework. It provides atomic components for building intelligent agents with interruptible and orchestratable capabilities, offering high flexibility but also relatively high learning costs. In contrast, the Agent Framework is built atop Graph, abstracting away the underlying complexities through concepts like ReactAgent and SequentialAgent.

Please check [the documentation](https://java2ai.com/docs/frameworks/graph-core/quick-start) on official website for mote details

## Core Concepts & Classes

Graph is deeply integrated with the Spring Boot ecosystem, providing a declarative API to orchestrate workflows. This allows developers to abstract each step of an AI application as a node (Node) and connect these nodes in the form of a directed graph (Graph) to create a customizable execution flow. Compared to traditional single-agent (one-turn Q&A) solutions, Spring AI Alibaba Graph supports more complex multi-step task flows, helping to address the issue of a **single large model being insufficient for complex tasks**.

The core of the framework includes: **StateGraph** (the state graph for defining nodes and edges), **Node** (node, encapsulating a specific operation or model call), **Edge** (edge, representing transitions between nodes), and **OverAllState** (global state, carrying shared data throughout the flow). These designs make it convenient to manage state and control flow in the workflow.

1. StateGraph
   The main class for defining a workflow.
   Lets you add nodes (addNode) and edges (addEdge, addConditionalEdges).
   Supports conditional routing, subgraphs, and validation.
   Can be compiled into a CompiledGraph for execution.
2. Node
   Represents a single step in the workflow (e.g., a model call, a data transformation).
   Nodes can be asynchronous and can encapsulate LLM calls or custom logic.
3. Edge
   Represents transitions between nodes.
   Can be conditional, with logic to determine the next node based on the current state.
4. OverAllState
   A serializable, central state object that holds all workflow data.
   Supports key-based strategies for merging/updating state.
   Used for checkpointing, resuming, and passing data between nodes.
5. CompiledGraph
   The executable form of a StateGraph.
   Handles the actual execution, state transitions, and streaming of results.
   Supports interruption, parallel nodes, and checkpointing.

## How It's Used (Typical Flow)
- Define StateGraph: In a Spring configuration, you define a StateGraph bean, add nodes (each encapsulating a model call or logic), and connect them with edges.
- Configure State: Use an OverAllStateFactory to define the initial state and key strategies.
- Execution: The graph is compiled and executed, with state flowing through nodes and edges, and conditional logic determining the path.
- Integration: Typically exposed via a REST controller or service in a Spring Boot app.
