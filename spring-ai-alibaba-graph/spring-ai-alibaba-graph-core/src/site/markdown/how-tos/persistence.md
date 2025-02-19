# Persistence 

## How to add persistence ("memory") to your graph

Many AI applications need memory to share context across multiple interactions. In LangGraph4j, memory is provided for any [`StateGraph`] through [`Checkpointers`].

When creating any LangGraph workflow, you can set them up to persist their state by doing using the following:

1. A [`Checkpointer`], such as the [`MemorySaver`]
1. Pass your [`Checkpointers`] in configuration when compiling the graph.

### Example

```java

AgentStateFactory<AgentState> factory = (initData) -> (new AgentState(initData));

var workflow = new StateGraph( factory );

// ... Add nodes and edges

// Initialize any compatible CheckPointSaver
var memory = new MemorySaver();

var compileConfig = CompileConfig.builder()
                        .checkpointSaver(memory)
                        .build();

var persistentGraph = workflow.compile( compileConfig );
```

[`StateGraph`]: https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/StateGraph.html
[`Checkpointers`]: https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/checkpoint/BaseCheckpointSaver.html
[`Checkpointer`]: https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/checkpoint/Checkpoint.html
[`MemorySaver`]: https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/checkpoint/MemorySaver.html

## Define the state

State is an (immutable) data class, inheriting from [`AgentState`], shared with all nodes in our graph. A state is basically a wrapper of a `Map<String,Object>` that provides some enhancers:

1. Schema (optional), that is a `Map<String,Channel>` where each [`Channel`] describe behaviour of the related property
1. `value()` accessors that inspect Map an return an Optional of value contained and cast to the required type

[`Channel`]: https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/state/Channel.html
[`AgentState`]: https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/state/AgentState.html

```java
import state.com.alibaba.ai.graph.AgentState;
import state.com.alibaba.ai.graph.Channel;
import state.com.alibaba.ai.graph.AppenderChannel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;

public class MessageState extends AgentState {

    static Map<String, Channel<?>> SCHEMA = Map.of(
            "messages", AppenderChannel.<AiMessage>of(ArrayList::new)
    );

    public MessageState(Map<String, Object> initData) {
        super(initData);
    }

    List<? extends ChatMessage> messages() {
        return this.<List<? extends ChatMessage>>value("messages")
                .orElseThrow(() -> new RuntimeException("messages not found"));
    }

    // utility method to quick access to last message
    Optional<? extends ChatMessage> lastMessage() {
        List<? extends ChatMessage> messages = messages();
        return (messages.isEmpty()) ?
                Optional.empty() :
                Optional.of(messages.get(messages.size() - 1));
    }
}
```

## Create Serializer

Every object that should be stored into State **MUST BE SERIALIZABLE**. If the object is not `Serializable` by default, Langgraph4j provides a way to build and associate a custom [`Serializer`] to it. 

In the example, since [`AiMessage`] from Langchain4j is not Serialzable we have to create an register a new custom [`Serializer`].

[`Serializer`]: https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/serializer/Serializer.html
[`AiMessage`]: https://docs.langchain4j.dev/apidocs/dev/langchain4j/data/message/AiMessage.html

```java

import com.alibaba.cloud.ai.graph.serializer.NullableObjectSerializer;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

// Setup custom serializer for Langchain4j ToolExecutionRequest
StateSerializer.register(ToolExecutionRequest.class,new Serializer<ToolExecutionRequest>(){

@Override
public void write(ToolExecutionRequest object,ObjectOutput out)throws IOException{
        out.writeUTF(object.id());
        out.writeUTF(object.name());
        out.writeUTF(object.arguments());


        }

@Override
public ToolExecutionRequest read(ObjectInput in)throws IOException,ClassNotFoundException{
        return ToolExecutionRequest.builder()
        .id(in.readUTF())
        .name(in.readUTF())
        .arguments(in.readUTF())
        .build();
        }
        });

// Setup custom serializer for Langchain4j AiMessage
        StateSerializer.register(AiMessage.class,new NullableObjectSerializer<AiMessage>(){

@Override
public void write(AiMessage object,ObjectOutput out)throws IOException{
        var hasToolExecutionRequests=object.hasToolExecutionRequests();

        out.writeBoolean(hasToolExecutionRequests);

        if(hasToolExecutionRequests){
        writeNullableObject(object.toolExecutionRequests(),out);

        }
        else{
        out.writeUTF(object.text());
        }

        }

@Override
public AiMessage read(ObjectInput in)throws IOException,ClassNotFoundException{
        var hasToolExecutionRequests=in.readBoolean();
        if(hasToolExecutionRequests){
        List<ToolExecutionRequest> toolExecutionRequests=readNullableObject(in);
        return AiMessage.aiMessage(toolExecutionRequests);
        }
        return AiMessage.aiMessage(in.readUTF());
        }
        });

```

## Set up the tools

Using [langchain4j], We will first define the tools we want to use. For this simple example, we will
use create a placeholder search engine. However, it is really easy to create
your own tools - see documentation
[here][tools] on how to do
that.

[langchain4j]: https://docs.langchain4j.dev
[tools]: https://docs.langchain4j.dev/tutorials/tools


```java
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Optional;

import static java.lang.String.format;

public class SearchTool {

    @Tool("Use to surf the web, fetch current information, check the weather, and retrieve other information.")
    String execQuery(@P("The query to use in your search.") String query) {

        // This is a placeholder for the actual implementation
        return "Cold, with a low of 13 degrees";
    }
}
```

## Set up the model

Now we will load the
[chat model].

1. It should work with messages. We will represent all agent state in the form of messages, so it needs to be able to work well with them.
2. It should work with [tool calling],meaning it can return function arguments in its response.

Note:
   >
   > These model requirements are not general requirements for using LangGraph4j - they are just requirements for this one example.
   >

[chat model]: https://docs.langchain4j.dev/tutorials/chat-and-language-models
[tool calling]: https://docs.langchain4j.dev/tutorials/tools   



```java
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;

public record LLM( OpenAiChatModel model ) {
    public LLM() {
        this( 
            OpenAiChatModel.builder()
                .apiKey( System.getenv("OPENAI_API_KEY") )
                .modelName( "gpt-4o" )
                .logResponses(true)
                .maxRetries(2)
                .temperature(0.0)
                .maxTokens(2000)
                .build()   
            );
    }
}

```


## Define the graph

We can now put it all together. We will run it first without a checkpointer:

```java


import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.tool.DefaultToolExecutor;

// Route Message 
EdgeAction<MessageState> routeMessage=state->{

        var lastMessage=state.lastMessage();

        if(!lastMessage.isPresent()){
        return"exit";
        }

        var message=(AiMessage)lastMessage.get();

        // If no tools are called, we can finish (respond to the user)
        if(!message.hasToolExecutionRequests()){
        return"exit";
        }

        // Otherwise if there is, we continue and call the tools
        return"next";
        };

        var llm=new LLM();

// Call Model
        NodeAction<MessageState> callModel=state->{

        var response=llm.model().generate((List<ChatMessage>)state.messages());

        return Map.of("messages",response.content());
        };

        var searchTool=new SearchTool();


// Invoke Tool 
        NodeAction<MessageState> invokeTool=state->{

        var lastMessage=(AiMessage)state.lastMessage()
        .orElseThrow(()->(new IllegalStateException("last message not found!")));

        var executionRequest=lastMessage.toolExecutionRequests().get(0);

        var executor=new DefaultToolExecutor(searchTool,executionRequest);

        var result=executor.execute(executionRequest,null);

        return Map.of("messages",AiMessage.from(result));
        };

// Define Graph

        var workflow=new StateGraph<MessageState> (MessageState.SCHEMA,MessageState::new)
        .addNode("agent",node_async(callModel))
        .addNode("tools",node_async(invokeTool))
        .addEdge(START,"agent")
        .addConditionalEdges("agent",edge_async(routeMessage),Map.of("next","tools","exit",END))
        .addEdge("tools","agent");

        var graph=workflow.compile();
```


```java

Map<String,Object> inputs = Map.of( "messages", AiMessage.from("Hi I'm Bartolo, niced to meet you.") );

var result = graph.stream( inputs );

for( var r : result ) {
  System.out.println( r.node() );
  if( r.node().equals("agent")) {
    System.out.println( r.state() );
  }
}
```

    __START__
    agent
    {messages=[AiMessage { text = "Hi I'm Bartolo, niced to meet you." toolExecutionRequests = null }, AiMessage { text = "Hello Bartolo! Nice to meet you too. How can I assist you today?" toolExecutionRequests = null }]}
    __END__



```java

Map<String,Object> inputs = Map.of( "messages", AiMessage.from("Remember my name?") );

var result = graph.stream( inputs );

for( var r : result ) {
  System.out.println( r.node() );
  if( r.node().equals("agent")) {
    System.out.println( r.state() );
  }
}
```

    __START__
    agent
    {messages=[AiMessage { text = "Remember my name?" toolExecutionRequests = null }, AiMessage { text = "I'm sorry, but I don't have the ability to remember personal details or previous interactions. How can I assist you today?" toolExecutionRequests = null }]}
    __END__


## Add Memory

Let's try it again with a checkpointer. We will use the
[`MemorySaver`],
which will "save" checkpoints in-memory.

[`MemorySaver`]: https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/checkpoint/MemorySaver.html

```java


// Here we only save in-memory
var memory=new MemorySaver();

        var compileConfig=CompileConfig.builder()
        .checkpointSaver(memory)
        .build();

        var persistentGraph=workflow.compile(compileConfig);
```

```java


var runnableConfig=RunnableConfig.builder()
        .threadId("conversation-num-1")
        .build();

        Map<String, Object> inputs=Map.of("messages",AiMessage.from("Hi I'm Bartolo, niced to meet you."));

        var result=persistentGraph.stream(inputs,runnableConfig);

        for(var r:result){
        System.out.println(r.node());
        if(r.node().equals("agent")){
        System.out.println(r.state().lastMessage().orElse(null));
        }
        }
```

    __START__
    agent
    AiMessage { text = "Hello Bartolo! Nice to meet you too. How can I assist you today?" toolExecutionRequests = null }
    __END__



```java

Map<String,Object> inputs = Map.of( "messages", AiMessage.from("Remember my name?") );

var result = persistentGraph.stream( inputs, runnableConfig );

for( var r : result ) {
  System.out.println( r.node() );
  if( r.node().equals("agent")) {
    System.out.println( r.state().lastMessage().orElse(null) );
  }
}
```

    __START__
    agent
    AiMessage { text = "Of course, Bartolo! How can I help you today?" toolExecutionRequests = null }
    __END__


## New Conversational Thread

If we want to start a new conversation, we can pass in a different
**`thread_id`**. Poof! All the memories are gone (just kidding, they'll always
live in that other thread)!



```java
runnableConfig =  RunnableConfig.builder()
                .threadId("conversation-2" )
                .build();
```


```java
inputs = Map.of( "messages", AiMessage.from("you forgot?") );

var result = persistentGraph.stream( inputs, runnableConfig );

for( var r : result ) {
  System.out.println( r.node() );
  if( r.node().equals("agent")) {
    System.out.println( r.state().lastMessage().orElse(null) );
  }
}
```

    __START__
    agent
    AiMessage { text = "I don't have personal experiences or memories, so I don't forget things in the way humans do. However, I can help you with information or answer questions based on the data I was trained on up until October 2023. How can I assist you today?" toolExecutionRequests = null }
    __END__

