# How to view and update past graph state

Once you start [checkpointing](./persistence.ipynb) your graphs, you can easily
**get** or **update** the state of the agent at any point in time. This permits
a few things:

1. You can surface a state during an interrupt to a user to let them accept an
   action.
2. You can **rewind** the graph to reproduce or avoid issues.
3. You can **modify** the state to embed your agent into a larger system, or to
   let the user better control its actions.

The key methods used for this functionality are:

- [getState](/langgraphjs/reference/classes/langgraph_pregel.Pregel.html#getState):
  fetch the values from the target config
- [updateState](/langgraphjs/reference/classes/langgraph_pregel.Pregel.html#updateState):
  apply the given values to the target state

**Note:** this requires passing in a checkpointer.

<!-- Example:
```javascript
TODO
...
``` -->

This works for [StateGraph](https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/StateGraph.html)

Below is an example.

## Define the state

State is an (immutable) data class, inheriting from [AgentState], shared with all nodes in our graph. A state is basically a wrapper of a `Map<String,Object>` that provides some enhancers:

1. Schema (optional), that is a `Map<String,Channel>` where each [Channel] describe behaviour of the related property
1. `value()` accessors that inspect Map an return an Optional of value contained and cast to the required type

[Channel]: https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/state/Channel.html
[AgentState]: https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/state/AgentState.html

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

## Create Serializers

Every object that should be stored into State **MUST BE SERIALIZABLE**. If the object is not `Serializable` by default, Langgraph4j provides a way to build and associate a custom [Serializer] to it. 

In the example, since [AiMessage] and [UserMessage] from Langchain4j are not Serialzable we have to create an register a new custom [`Serializer`].

[Serializer]: https://bsorrentino.github.io/langgraph4j/apidocs/org/bsc/langgraph4j/serializer/Serializer.html
[AiMessage]: https://docs.langchain4j.dev/apidocs/dev/langchain4j/data/message/AiMessage.html
[UserMessage]: https://docs.langchain4j.dev/apidocs/dev/langchain4j/data/message/UserMessage.html

```java

import com.alibaba.cloud.ai.graph.serializer.NullableObjectSerializer;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

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
        StateSerializer.register(ChatMessage.class,new NullableObjectSerializer<ChatMessage>(){

        void writeAI(AiMessage msg,ObjectOutput out)throws IOException{
        var hasToolExecutionRequests=msg.hasToolExecutionRequests();

        out.writeBoolean(hasToolExecutionRequests);

        if(hasToolExecutionRequests){
        writeNullableObject(msg.toolExecutionRequests(),out);
        }
        else{
        out.writeUTF(msg.text());
        }
        }

        AiMessage readAI(ObjectInput in)throws IOException,ClassNotFoundException{
        var hasToolExecutionRequests=in.readBoolean();
        if(hasToolExecutionRequests){
        List<ToolExecutionRequest> toolExecutionRequests=readNullableObject(in);
        return AiMessage.aiMessage(toolExecutionRequests);
        }
        return AiMessage.aiMessage(in.readUTF());
        }

        void writeUSER(UserMessage msg,ObjectOutput out)throws IOException{
        out.writeUTF(msg.text());
        }

        UserMessage readUSER(ObjectInput in)throws IOException,ClassNotFoundException{
        return UserMessage.from(in.readUTF());
        }

        void writeEXREQ(ToolExecutionResultMessage msg,ObjectOutput out)throws IOException{
        out.writeUTF(msg.id());
        out.writeUTF(msg.toolName());
        out.writeUTF(msg.text());
        }

        ToolExecutionResultMessage readEXREG(ObjectInput in)throws IOException,ClassNotFoundException{
        return new ToolExecutionResultMessage(in.readUTF(),in.readUTF(),in.readUTF());
        }


@Override
public void write(ChatMessage object,ObjectOutput out)throws IOException{
        out.writeObject(object.type());
        switch(object.type()){
        case AI->writeAI((AiMessage)object,out);
        case USER->writeUSER((UserMessage)object,out);
        case TOOL_EXECUTION_RESULT->writeEXREQ((ToolExecutionResultMessage)object,out);
        case SYSTEM->{
        // Nothing
        }
        };
        }

@Override
public ChatMessage read(ObjectInput in)throws IOException,ClassNotFoundException{

        ChatMessageType type=(ChatMessageType)in.readObject();

        return switch(type){
        case AI->{yield readAI(in);}
        case USER->{yield readUSER(in);}
        case TOOL_EXECUTION_RESULT->{yield readEXREG(in);}
        case SYSTEM->{
        yield null;
        }
        };
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

OpenAiChatModel llm = OpenAiChatModel.builder()
    .apiKey( System.getenv("OPENAI_API_KEY") )
    .modelName( "gpt-4o" )
    .logResponses(true)
    .maxRetries(2)
    .temperature(0.0)
    .maxTokens(2000)
    .build()  

```

## Test function calling


```java
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.tool.DefaultToolExecutor;

var tools = ToolSpecifications.toolSpecificationsFrom( SearchTool.class );

UserMessage userMessage = UserMessage.from("What will the weather be like in London tomorrow?");
Response<AiMessage> response = llm.generate(Collections.singletonList(userMessage), tools );
AiMessage aiMessage = response.content();

System.out.println( aiMessage );

var executionRequest = aiMessage.toolExecutionRequests().get(0);

var executor = new DefaultToolExecutor( new SearchTool(), executionRequest );

var result = executor.execute( executionRequest, null );

result;

```

    AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_3YrelSH5At2zVambJLLf8gYo", name = "execQuery", arguments = "{"query":"London weather forecast for tomorrow"}" }] }

    Cold, with a low of 13 degrees



## Define the graph

We can now put it all together. We will run it first without a checkpointer:

```java


import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.tool.DefaultToolExecutor;
// Route Message 
EdgeAction<MessageState> routeMessage=state->{

        var lastMessage=state.lastMessage();

        if(!lastMessage.isPresent())return"exit";

        if(lastMessage.get()instanceof AiMessage message){

        // If tools should be called
        if(message.hasToolExecutionRequests())return"next";

        }

        // If no tools are called, we can finish (respond to the user)
        return"exit";
        };

// Call Model
        NodeAction<MessageState> callModel=state->{
        var tools=ToolSpecifications.toolSpecificationsFrom(SearchTool.class );

        var response=llm.generate((List<ChatMessage>)state.messages(),tools);

        return Map.of("messages",response.content());
        };

// Invoke Tool 
        NodeAction<MessageState> invokeTool=state->{
        var lastMessage=(AiMessage)state.lastMessage()
        .orElseThrow(()->(new IllegalStateException("last message not found!")));

        var executionRequest=lastMessage.toolExecutionRequests().get(0);

        var executor=new DefaultToolExecutor(new SearchTool(),executionRequest);

        var result=executor.execute(executionRequest,null);

        return Map.of("messages",new ToolExecutionResultMessage(executionRequest.id(),executionRequest.name(),result));
        };

// Define Graph
        var workflow=new StateGraph<MessageState> (MessageState.SCHEMA,MessageState::new)
        .addNode("agent",node_async(callModel))
        .addNode("tools",node_async(invokeTool))
        .addEdge(START,"agent")
        .addConditionalEdges("agent",edge_async(routeMessage),Map.of("next","tools","exit",END))
        .addEdge("tools","agent");

// Here we only save in-memory
        var memory=new MemorySaver();

        var compileConfig=CompileConfig.builder()
        .checkpointSaver(memory)
        .build();

        var graph=workflow.compile(compileConfig);
```

## Interacting with the Agent

We can now interact with the agent. Between interactions you can get and update state.

```java


var runnableConfig=RunnableConfig.builder()
        .threadId("conversation-num-1")
        .build();

        Map<String, Object> inputs=Map.of("messages",UserMessage.from("Hi I'm Bartolo."));

        var result=graph.stream(inputs,runnableConfig);

        for(var r:result){
        System.out.println(r.node());
        System.out.println(r.state());

        }
```

    __START__
    {messages=[UserMessage { name = null contents = [TextContent { text = "Hi I'm Bartolo." }] }]}
    agent
    {messages=[UserMessage { name = null contents = [TextContent { text = "Hi I'm Bartolo." }] }, AiMessage { text = "Hello Bartolo! How can I assist you today?" toolExecutionRequests = null }]}
    __END__
    {messages=[UserMessage { name = null contents = [TextContent { text = "Hi I'm Bartolo." }] }, AiMessage { text = "Hello Bartolo! How can I assist you today?" toolExecutionRequests = null }]}


Here you can see the "`agent`" node ran, and then our edge returned `__END__` so the graph stopped execution there.

Let's check the current graph state.

```java


var checkpoint=graph.getState(runnableConfig);

        System.out.println(checkpoint);

```

    StateSnapshot(state={messages=[UserMessage { name = null contents = [TextContent { text = "Hi I'm Bartolo." }] }, AiMessage { text = "Hello Bartolo! How can I assist you today?" toolExecutionRequests = null }]}, config=RunnableConfig(threadId=conversation-num-1, checkPointId=93c2b047-7927-4062-89e0-4498f63813f6, nextNode=__END__))


The current state is the two messages we've seen above, 1. the Human Message we sent in, 2. the AIMessage we got back from the model.

The next value is `__END__`  since the graph has terminated.


```java
checkpoint.getNext()
```

    __END__



## Let's get it to execute a tool

When we call the graph again, it will create a checkpoint after each internal execution step. Let's get it to run a tool, then look at the checkpoint.


```java

Map<String,Object> inputs = Map.of( "messages", UserMessage.from("What's the weather like in SF currently?") );

var state = graph.invoke( inputs, runnableConfig ).orElseThrow( () ->(new IllegalStateException()) ) ;

System.out.println( state.lastMessage().orElse(null) );
  
```

    AiMessage { text = "The current weather in San Francisco is cold, with a low of 13 degrees Celsius. Is there anything else you need help with?" toolExecutionRequests = null }


## Pause before tools

If you notice below, we now will add interruptBefore=["action"] - this means that before any actions are taken we pause. This is a great moment to allow the user to correct and update the state! This is very useful when you want to have a human-in-the-loop to validate (and potentially change) the action to take.



```java
var memory = new MemorySaver();

var compileConfig = CompileConfig.builder()
                    .checkpointSaver(memory)
                    .interruptBefore( "tools")
                    .build();

var graphWithInterrupt = workflow.compile(compileConfig);

var runnableConfig =  RunnableConfig.builder()
                .threadId("conversation-2" )
                .build();

Map<String,Object> inputs = Map.of( "messages", UserMessage.from("What's the weather like in SF currently?") );

var result = graphWithInterrupt.stream( inputs, runnableConfig );

for( var r : result ) {
  System.out.println( r.node() );
  System.out.println( r.state() );
  
}

```

    __START__
    {messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }]}
    agent
    {messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }]}


## Get State

You can fetch the latest graph checkpoint using `getState(config)`.


```java
var snapshot = graphWithInterrupt.getState(runnableConfig);
snapshot.getNext();

```




    tools



## Resume

You can resume by running the graph with a null input. The checkpoint is loaded, and with no new inputs, it will execute as if no interrupt had occurred.


```java
var result = graphWithInterrupt.stream( null, snapshot.getConfig() );

for( var r : result ) {
  System.out.println( r.node() );
  System.out.println( r.state() );
  
}
```

    tools
    {messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }, ToolExecutionResultMessage { id = "call_M60jubFO7QzVUGZsutRsvdjw" toolName = "execQuery" text = "Cold, with a low of 13 degrees" }]}
    agent
    {messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }, ToolExecutionResultMessage { id = "call_M60jubFO7QzVUGZsutRsvdjw" toolName = "execQuery" text = "Cold, with a low of 13 degrees" }, AiMessage { text = "The current weather in San Francisco is cold, with a low of 13 degrees Celsius." toolExecutionRequests = null }]}
    __END__
    {messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }, ToolExecutionResultMessage { id = "call_M60jubFO7QzVUGZsutRsvdjw" toolName = "execQuery" text = "Cold, with a low of 13 degrees" }, AiMessage { text = "The current weather in San Francisco is cold, with a low of 13 degrees Celsius." toolExecutionRequests = null }]}


## Check full history

Let's browse the history of this thread, from newest to oldest.




```java
RunnableConfig toReplay = null;
var states = graphWithInterrupt.getStateHistory(runnableConfig);
for( var state: states ) {
  
  System.out.println(state);
  System.out.println("--");

  if (state.getState().messages().size() == 3) {
     toReplay = state.getConfig();
  }
}
if (toReplay==null) {
  throw new IllegalStateException("No state to replay");
}
```

    StateSnapshot(state={messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }, ToolExecutionResultMessage { id = "call_M60jubFO7QzVUGZsutRsvdjw" toolName = "execQuery" text = "Cold, with a low of 13 degrees" }, AiMessage { text = "The current weather in San Francisco is cold, with a low of 13 degrees Celsius." toolExecutionRequests = null }]}, config=RunnableConfig(threadId=conversation-2, checkPointId=5bb993e3-8703-48c9-8284-1ee801c265b7, nextNode=__END__))
    --
    StateSnapshot(state={messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }, ToolExecutionResultMessage { id = "call_M60jubFO7QzVUGZsutRsvdjw" toolName = "execQuery" text = "Cold, with a low of 13 degrees" }]}, config=RunnableConfig(threadId=conversation-2, checkPointId=11fc5618-c5b2-4b15-843c-8a51267c76a1, nextNode=agent))
    --
    StateSnapshot(state={messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }, ToolExecutionResultMessage { id = "call_M60jubFO7QzVUGZsutRsvdjw" toolName = "execQuery" text = "Cold, with a low of 13 degrees" }, AiMessage { text = "The current weather in San Francisco is cold, with a low of 13 degrees Celsius." toolExecutionRequests = null }]}, config=RunnableConfig(threadId=conversation-2, checkPointId=ab776d0f-a051-424c-9a70-6017f3bfdbf8, nextNode=__END__))
    --
    StateSnapshot(state={messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }, ToolExecutionResultMessage { id = "call_M60jubFO7QzVUGZsutRsvdjw" toolName = "execQuery" text = "Cold, with a low of 13 degrees" }]}, config=RunnableConfig(threadId=conversation-2, checkPointId=d6909dae-6f94-4afe-a8b5-7b39da95cc22, nextNode=agent))
    --
    StateSnapshot(state={messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }, ToolExecutionResultMessage { id = "call_M60jubFO7QzVUGZsutRsvdjw" toolName = "execQuery" text = "Cold, with a low of 13 degrees" }, AiMessage { text = "The current weather in San Francisco is cold, with a low of 13 degrees Celsius." toolExecutionRequests = null }]}, config=RunnableConfig(threadId=conversation-2, checkPointId=0bb4e2ce-67fe-4585-94c7-035602f51f16, nextNode=tools))
    --
    StateSnapshot(state={messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }, ToolExecutionResultMessage { id = "call_M60jubFO7QzVUGZsutRsvdjw" toolName = "execQuery" text = "Cold, with a low of 13 degrees" }, AiMessage { text = "The current weather in San Francisco is cold, with a low of 13 degrees Celsius." toolExecutionRequests = null }]}, config=RunnableConfig(threadId=conversation-2, checkPointId=0167fa6f-e3b1-4b9e-aeac-b8ff15f191ba, nextNode=tools))
    --
    StateSnapshot(state={messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }]}, config=RunnableConfig(threadId=conversation-2, checkPointId=3a4cc3c3-e863-4799-b33c-8ca295000552, nextNode=agent))
    --


## Replay a past state

To replay from this place we just need to pass its config back to the agent.


```java
var results = graphWithInterrupt.stream(null, toReplay ); 

for( var r : results ) {
  System.out.println( r.node() );
  System.out.println( r.state() );
  
}

```

    agent
    {messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }, ToolExecutionResultMessage { id = "call_M60jubFO7QzVUGZsutRsvdjw" toolName = "execQuery" text = "Cold, with a low of 13 degrees" }, AiMessage { text = "The current weather in San Francisco is cold, with a low of 13 degrees Celsius." toolExecutionRequests = null }]}
    __END__
    {messages=[UserMessage { name = null contents = [TextContent { text = "What's the weather like in SF currently?" }] }, AiMessage { text = null toolExecutionRequests = [ToolExecutionRequest { id = "call_M60jubFO7QzVUGZsutRsvdjw", name = "execQuery", arguments = "{"query":"current weather in San Francisco"}" }] }, ToolExecutionResultMessage { id = "call_M60jubFO7QzVUGZsutRsvdjw" toolName = "execQuery" text = "Cold, with a low of 13 degrees" }, AiMessage { text = "The current weather in San Francisco is cold, with a low of 13 degrees Celsius." toolExecutionRequests = null }]}

