# 🦜🕸️ SpringAiGraph Studio

An **embed playground webapp** that runs a SpringAiGraph workflow in visual way.

![result](studio-demo.gif)

## Features

- [x] Show graph diagram
- [x] Run a workflow
- [x] show which step is currently running
- [x] Show state data for each executed step
- [x] Allow edit state data and resume execution
- [ ] Manage Interruptions

## Demo Code
```java
StateGraph<AgentState> workflow = new StateGraph<>( AgentState::new );

// define your workflow   

...

var saver = new MemorySaver();
// connect playgroud webapp to workflow
var server = LangGraphStreamingServerJetty.builder()
                                      .port(8080)
                                      .title("SpringAiGraph - TEST")
                                      .stateGraph( workflow )
                                      .checkpointSaver(saver)
                                      .addInputStringArg("input")
                                      .build();
// start playground
server.start().join();

```
