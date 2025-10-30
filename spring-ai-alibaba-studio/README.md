![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg) [![Static Badge](https://img.shields.io/badge/maven--snapshots-0.0.2--SNAPSHOT-blue)][snapshots] [![Maven Central](https://img.shields.io/maven-central/v/org.bsc.langgraph4j/langgraph4j-copilotkit.svg)][releases][![discord](https://img.shields.io/discord/1364514593765986365?logo=discord&style=flat)](https://discord.gg/szVVztSYKh)

# Visualized agent UI based on CopilotKit

Compatible with [AG-UI protocol][AG-UI] with [CopilotKit] integration

## Architecture

```mermaid
flowchart LR
    User((User))
    CopilotKit(Copilot Kit)
    SAA Adaptor (SAA AGUI Adaptor
    Typescript)
    SAAServer(SAA AGUI Adaptor
    Java)
    Agent(Agent)
    subgraph "AG-UI-APP"
        CopilotKit --> SAAAdaptor
    end
    subgraph "SAA Server"
        SAAServer --> Agent
    end
    User --> CopilotKit
    %%CopilotKit --> SAAAdaptor
    SAAAdaptor --> SAAServer
    %%SAAServer --> Agent
    Agent --> SAAServer
    SAAServer --> SAAAdaptor
    SAAAdaptor --> CopilotKit
    CopilotKit --> User
    %% Legend
    %% - The User sends a request to the Copilot Kit.
    %% - The Copilot Kit processes the request and passes it to the SAA Adaptor.
    %% - The SAA Adaptor forwards the request to the SAA Server.
    %% - The SAA Server processes the request using the Agent.
    %% - The Agent processes the data and sends back a response to the SAA Server.
    %% - The SAA Server sends the processed response back to the SAA Adaptor.
    %% - The SAA Adaptor then sends the response back to the Copilot Kit.
    %% - Finally, the Copilot Kit presents the results back to the User.

```
## Getting Started

### Start Spring AI Alibaba Agent

```bash
mvn package spring-boot:test-run
```

### Start CopilotKit App

```bash
cd webui
npm run dev
```

### Open web app

Open browser on [http://localhost:3000](http://localhost:3000) and play with chat


## References

[AG-UI]: https://docs.ag-ui.com/introduction
[CopilotKit]: https://www.copilotkit.ai
