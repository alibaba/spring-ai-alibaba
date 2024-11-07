# How to define graph state

This how to guide will cover how to define the state of your graph. This implementation has changed, and there is a new recommended method of defining the state of your graph. This new method is through the [`Annotation`](/langgraphjs/reference/functions/langgraph.Annotation-1.html) function.

## Prerequisites

- [State conceptual guide](/langgraphjs/concepts/low_level/#state) - Conceptual guide on defining the state of your graph.
- [Building graphs](/langgraphjs/tutorials/quickstart/) - This how to assumes you have a basic understanding of how to build graphs.

## Setup

This guide requires installing the `@langchain/langgraph`, and `@langchain/core` packages:

```bash
npm install @langchain/langgraph @langchain/core
```

## Getting started

The `Annotation` function is the recommended way to define your graph state for new `StateGraph` graphs. The `Annotation.Root` function is used to create the top-level state object, where each field represents a channel in the graph.

Here's an example of how to define a simple graph state with one channel called `messages`:


```typescript
import { BaseMessage } from "@langchain/core/messages";
import { Annotation } from "@langchain/langgraph";

const GraphAnnotation = Annotation.Root({
  // Define a 'messages' channel to store an array of BaseMessage objects
  messages: Annotation<BaseMessage[]>({
    // Reducer function: Combines the current state with new messages
    reducer: (currentState, updateValue) => currentState.concat(updateValue),
    // Default function: Initialize the channel with an empty array
    default: () => [],
  })
});
```

Each channel can optionally have `reducer` and `default` functions:
- The `reducer` function defines how new values are combined with the existing state.
- The `default` function provides an initial value for the channel.

For more information on reducers, see the [reducers conceptual guide](/langgraphjs/concepts/low_level/#reducers)


```typescript
const QuestionAnswerAnnotation = Annotation.Root({
  question: Annotation<string>,
  answer: Annotation<string>,
});
```

Above, all we're doing is defining the channels, and then passing the un-instantiated `Annotation` function as the value. It is important to note we always pass in the TypeScript type of each channel as the first generics argument to `Annotation`. Doing this ensures our graph state is type safe, and we can get the proper types when defining our nodes. Below shows how you can extract the typings from the `Annotation` function:


```typescript
type QuestionAnswerAnnotationType = typeof QuestionAnswerAnnotation.State;
```

This is equivalent to the following type:

```typescript
type QuestionAnswerAnnotationType = {
  question: string;
  answer: string;
}
```

## Merging states

If you have two graph state annotations, you can merge the two into a single annotation by using the `spec` value:


```typescript
const MergedAnnotation = Annotation.Root({
  ...QuestionAnswerAnnotation.spec,
  ...GraphAnnotation.spec,
})
```

The type of the merged annotation is the intersection of the two annotations:

```typescript
type MergedAnnotation = {
  messages: BaseMessage[];
  question: string;
  answer: string;
}
```

Finally, instantiating your graph using the annotations is as simple as passing the annotation to the `StateGraph` constructor:



```typescript
import { StateGraph } from "@langchain/langgraph";

const workflow = new StateGraph(MergedAnnotation);
```

## State channels

The `Annotation` function is a convince wrapper around the low level implementation of how states are defined in LangGraph. Defining state using the `channels` object (which is what `Annotation` is a wrapper of) is still possible, although not recommended for most cases. The below example shows how to implement a graph using this pattern:


```typescript
import { StateGraph } from "@langchain/langgraph";

interface WorkflowChannelsState {
  messages: BaseMessage[];
  question: string;
  answer: string;
}

const workflowWithChannels = new StateGraph<WorkflowChannelsState>({
  channels: {
    messages: {
      reducer: (currentState, updateValue) => currentState.concat(updateValue),
      default: () => [],
    },
    question: null,
    answer: null,
  }
});
```

Above, we set the value of `question` and `answer` to `null`, as it does not contain a default value. To set a default value, the channel should be implemented how the `messages` key is, with the `default` factory returing the default value. The `reducer` function is optional, and can be added to the channel object if needed.
