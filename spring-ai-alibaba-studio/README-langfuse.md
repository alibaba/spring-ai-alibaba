# Langfuse Setup For Studio

The `langfuse` profile is available on the `studio` test classpath for validating
Spring AI and graph traces with Langfuse.

## Environment variables

Do not put real keys into YAML.

```bash
export AI_DASHSCOPE_API_KEY='your-dashscope-key'
export LANGFUSE_ENABLED=true
export LANGFUSE_PUBLIC_KEY='your-public-key'
export LANGFUSE_SECRET_KEY='your-secret-key'
export LANGFUSE_OTEL_AUTH=$(printf '%s' "${LANGFUSE_PUBLIC_KEY}:${LANGFUSE_SECRET_KEY}" | base64)
```

Optional:

```bash
export LANGFUSE_SERVICE_NAME='spring-ai-alibaba-studio-langfuse'
export LANGFUSE_ENV='development'
export SPRING_AI_LOG_PROMPT=false
export SPRING_AI_LOG_COMPLETION=false
export SPRING_AI_LOG_QUERY_RESPONSE=false
```

## Run Studio with Langfuse

```bash
env MAVEN_USER_HOME=/tmp/codex-m2 ./mvnw -U \
  -pl spring-ai-alibaba-studio \
  -am \
  -DskipTests \
  -Dspring-boot.run.useTestClasspath=true \
  -Dspring-boot.run.mainClass=com.alibaba.cloud.ai.StudioApplication \
  -Dspring-boot.run.profiles=graph,langfuse \
  spring-boot:run
```

Useful endpoints:

- `GET /list-apps`
- `GET /list-graphs`
- `POST /run_sse`
- `POST /graph_run_sse`

Langfuse:

- UI: [https://us.cloud.langfuse.com](https://us.cloud.langfuse.com)
- OTLP traces: `https://us.cloud.langfuse.com/api/public/otel/v1/traces`

## Tests

Profile smoke test:

```bash
env MAVEN_USER_HOME=/tmp/codex-m2 ./mvnw -U \
  -pl spring-ai-alibaba-studio \
  -am \
  -Dtest=LangfuseProfileContextTest \
  test
```

Live Langfuse test:

```bash
env MAVEN_USER_HOME=/tmp/codex-m2 ./mvnw -U \
  -pl spring-ai-alibaba-studio \
  -am \
  -Dtest=LangfuseAgentStudioLiveTest \
  test
```
