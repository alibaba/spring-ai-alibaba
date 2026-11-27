This directory contains the full code examples for documentations published on https://java2ai.com

## Spring AI 2.1 Agent Framework tutorials

The examples under `framework/tutorials` use the Spring AI 2.1.0 APIs for request-scoped tools and approval, Message Parts, typed decisions, and Session Memory.

## Streaming events

`GraphEventStreamingExample` shows progress from a node, live model deltas from
a dynamically invoked child graph, a mapped child that pauses and resumes, and
parallel branch events. `AgentEventStreamingExample` shows an agent used as a
node and as a delegated tool, a reactive tool's output and final result, and
several projections from one execution.

Both examples use scripted model responses. From the repository root, install
the streaming APIs from the current checkout, then run the example entry points.

```bash
mvn -B -pl :spring-ai-alibaba-bom,:spring-ai-alibaba-studio -am -Dspringdoc.skip=true -DskipTests install
mvn -B -f examples/documentation/pom.xml compile org.codehaus.mojo:exec-maven-plugin:3.6.3:java \
  -Dexec.mainClass=com.alibaba.cloud.ai.examples.documentation.graph.core.GraphEventStreamingExample
mvn -B -f examples/documentation/pom.xml compile org.codehaus.mojo:exec-maven-plugin:3.6.3:java \
  -Dexec.mainClass=com.alibaba.cloud.ai.examples.documentation.framework.tutorials.AgentEventStreamingExample
```

## Context management recipes

`ContextManagementRecipes` runs three independent recipes using local model
responses. It traces budget-based fallback, reopens a file checkpoint and
reads a scoped archive, then delegates an inventory case with an explicit
state projection, deferred tools, Skills, and tenant-scoped memory.

Run the focused tests from the repository root. The first command installs
the APIs from the current checkout.

```bash
mvn -B -pl :spring-ai-alibaba-bom,:spring-ai-alibaba-agent-framework -am -DskipTests install
mvn -B -f examples/documentation/pom.xml -Dtest=ContextManagementRecipesTest test
```

To run the recipes and print their results, use the application entry point.
The application creates a temporary directory for checkpoint and Store data.
Set `-Dexec.args=/path/to/context-recipes` to use a specific directory.

```bash
mvn -B -f examples/documentation/pom.xml compile org.codehaus.mojo:exec-maven-plugin:3.6.3:java \
  -Dexec.cleanupDaemonThreads=false \
  -Dexec.mainClass=com.alibaba.cloud.ai.examples.documentation.framework.advanced.ContextManagementRecipes
```

## Running the documentation tests

Run the examples module tests from the repository root. They use local responses and require no model credentials.

```bash
mvn -B -f examples/documentation/pom.xml test
```

The live DashScope examples read `AI_DASHSCOPE_API_KEY` from the environment.

## Context management with native OpenAI

`OpenAiContextManagementExample` runs a report audit with the Spring AI
`OpenAiChatModel` Chat Completions adapter. The coordinator summarizes older
turns into a durable checkpoint, then routes the audit to a primary model. The
fallback assembles the same request with its own model options and input budget.
[Spring AI OpenAI Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)

The coordinator finds and loads an inventory tool on demand. Its oversized
result becomes an archive reference. The delegated analyst receives the task
and case ID, plus an explicitly forwarded tenant key for memory access. It
searches for a Skill and reads its instructions before asking for approval.
After approval, a new agent instance resumes from the same file checkpoints.
The analyst reads the scoped memory and returns its selected output. The
coordinator then reads the first two archived pages.

### Run the repeatable acceptance scenario

From the repository root, install the current framework from source and run the
example test.

```bash
mvn -B -pl :spring-ai-alibaba-bom,:spring-ai-alibaba-agent-framework -am -DskipTests install
mvn -B -f examples/documentation/pom.xml -Dtest=OpenAiContextManagementExampleTest test
```

The test starts a loopback HTTP server with scripted Chat Completions responses.
Both models use the native adapter. The primary receives
`context_length_exceeded`, the fallback streams tool calls, and the analyst
uses regular JSON completions. The test checks the outgoing model options and
tool definitions, the restored summary and approval, and that completed tools
do not run again. It needs no credentials or remote model calls.

### Run the example against a configured endpoint

Set `OPENAI_API_KEY`, `OPENAI_PRIMARY_MODEL`, and `OPENAI_FALLBACK_MODEL`.
Choose models that support Chat Completions and tool calling. Optional
`OPENAI_BASE_URL` defaults to `https://api.openai.com/v1`. The example uses a
deliberately small primary input cap of 128 tokens to exercise fallback. Set
`OPENAI_PRIMARY_INPUT_LIMIT` to change that cap.

```bash
mvn -B -f examples/documentation/pom.xml compile org.codehaus.mojo:exec-maven-plugin:3.6.3:java \
  -Dexec.cleanupDaemonThreads=false \
  -Dexec.mainClass=com.alibaba.cloud.ai.examples.documentation.framework.advanced.OpenAiContextManagementExample
```

The program uses a temporary directory for the coordinator and analyst
checkpoints, archive, and memory. Use `-Dexec.args=/path/to/demo-workspace` to
choose a directory. The example approves its synthetic audit in code and
reports fetch and recording counts before and after recreating the agents.
For an application, persist the thread ID and keep its checkpoint and archive
locations available. Resume with the recorded interruption and the user's
approval, using the same thread ID and empty input to continue the pending task.
