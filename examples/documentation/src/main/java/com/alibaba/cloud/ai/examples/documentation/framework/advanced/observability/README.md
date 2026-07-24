# ReactAgent observability with Langfuse

This example exports the ReactAgent graph, model, and tool observations to
Langfuse through OpenTelemetry Protocol (OTLP).

## Prerequisites

- A DashScope API key
- A Langfuse project and its public/secret API keys
- Java 17 or later

Create the HTTP Basic credential expected by Langfuse:

```shell
export LANGFUSE_AUTH_STRING=$(printf '%s' 'pk-lf-...:sk-lf-...' | base64)
```

Set the DashScope key. The example defaults to Langfuse's EU Cloud trace
endpoint; set `LANGFUSE_OTLP_TRACES_ENDPOINT` to the `/v1/traces` endpoint for
another Langfuse Cloud region or a self-hosted deployment:

```shell
export AI_DASHSCOPE_API_KEY=your-dashscope-api-key
# Optional:
# export LANGFUSE_OTLP_TRACES_ENDPOINT=https://us.cloud.langfuse.com/api/public/otel/v1/traces
```

## Run

From the repository root:

```shell
./mvnw -f examples/documentation/pom.xml spring-boot:run \
  -Plangfuse \
  -Dspring-boot.run.main-class=com.alibaba.cloud.ai.examples.documentation.framework.advanced.observability.LangfuseObservabilityExample \
  -Dspring-boot.run.arguments=--spring.config.name=application-langfuse
```

The application runs one ReactAgent request and prints the answer. Langfuse
receives a root `react-agent-demo` trace containing the nested graph and model
observations. The example uses a generated session ID and the fixed
`documentation-user` user ID; replace those values with application identifiers
in production.

Prompt and completion capture is enabled for demonstration. Review the data
privacy requirements of your application before enabling it in production.
