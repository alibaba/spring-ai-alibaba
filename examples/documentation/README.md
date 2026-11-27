This directory contains the full code examples for documentations published on https://java2ai.com

## Spring AI 2.1 Agent Framework tutorials

The examples under `framework/tutorials` use the Spring AI 2.1.0 APIs for request-scoped tools and approval, Message Parts, typed decisions, and Session Memory.

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
