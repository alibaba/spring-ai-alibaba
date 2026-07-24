# Spring AI Alibaba Sandbox

This module exposes AgentScope Runtime sandbox capabilities as Spring AI
`ToolCallback` instances. A regular Spring AI Alibaba `ReactAgent` can therefore
run Python, shell, filesystem, and browser operations in an isolated Docker
environment instead of on the application host.

## Requirements

- Java 17 or later
- A running Docker daemon accessible to the application process
- A supported chat model

Add the sandbox and Agent Framework dependencies:

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-agent-framework</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-sandbox</artifactId>
</dependency>
```

Manage their versions with the Spring AI Alibaba BOM used by the rest of the
application.

## Use Python and shell tools with ReactAgent

Create one `SandboxService` for the application, start it before creating
sandboxes, and clean up its containers during application shutdown:

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.sandbox.ToolkitInit;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

ManagerConfig managerConfig = ManagerConfig.builder().build();
SandboxService sandboxService = new SandboxService(managerConfig);
sandboxService.start();

BaseSandbox sandbox = new BaseSandbox(
        sandboxService,
        "user-123",
        "session-456");

// Configure or inject the ChatModel used by the application.
ReactAgent agent = ReactAgent.builder()
        .name("sandbox_agent")
        .description("An agent that executes code in an isolated sandbox.")
        .instruction("Use the Python or shell tool when code execution is required.")
        .model(chatModel)
        .tools(List.of(
                ToolkitInit.RunPythonCodeTool(sandbox),
                ToolkitInit.RunShellCommandTool(sandbox)))
        .build();

try {
    agent.invoke("Use Python to calculate the first 20 Fibonacci numbers.");
}
finally {
    sandboxService.cleanupAllSandboxes();
}
```

In a Spring Boot application, prefer declaring `SandboxService` as a singleton
bean with explicit lifecycle methods:

```java
@Bean(initMethod = "start", destroyMethod = "cleanupAllSandboxes")
SandboxService sandboxService() {
    return new SandboxService(ManagerConfig.builder().build());
}
```

Create a separate `BaseSandbox` per user/session isolation boundary. Do not
reuse the same user and session identifiers for mutually untrusted workloads.

## Other tool groups

`ToolkitInit` also provides callbacks for:

- `FilesystemSandbox`: reading, writing, editing, moving, and searching files
- `BrowserSandbox`: navigation, interaction, screenshots, and page inspection
- `BaseSandbox`: Python and shell execution

Only expose the minimum tool set required by the agent.

## Deployment boundary

The current implementation uses AgentScope Runtime's Docker-backed
`SandboxService`. The application process must be able to reach the Docker
daemon and create the required sandbox containers.

This module is not a generic client for an arbitrary remote Kubernetes sandbox
service. Integrating a separate remote sandbox control plane requires a custom
tool callback or a compatible AgentScope Runtime manager implementation.

For production deployments, also apply container resource limits, network
policies, image allowlists, read-only mounts, and cleanup monitoring appropriate
to the trust level of executed code.
