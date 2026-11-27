This directory contains the full code examples for documentations published on https://java2ai.com

## Spring AI 2.1 Agent Framework tutorials

The examples under `framework/tutorials` use the Spring AI 2.1.0 APIs for request-scoped tools and approval, Message Parts, typed decisions, and Session Memory.

Run the examples module tests from the repository root. They use local responses and require no model credentials.

```bash
mvn -B -f examples/documentation/pom.xml test
```

The live DashScope examples read `AI_DASHSCOPE_API_KEY` from the environment.
