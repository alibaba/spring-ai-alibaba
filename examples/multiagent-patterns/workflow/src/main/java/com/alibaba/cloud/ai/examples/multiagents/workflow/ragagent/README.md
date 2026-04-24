# RAG Agent Workflow

Custom RAG workflow equivalent to `multiagents/custom.md` and `multiagents/code/rag-agent-workflow.md`.

## Flow

```
Query → Rewrite → Retrieve → Prepare → Agent → Response
```

- **Rewrite**: LLM rewrites the query for better retrieval (e.g., focus on player names, stats).
- **Retrieve**: Vector similarity search (no LLM).
- **Prepare**: Formats context and question into a prompt.
- **Agent**: ReactAgent with context; can use `get_latest_news` tool for live updates.

## Enable

```yaml
workflow.rag.enabled: true
workflow.runner.enabled: true  # optional demo on startup
```

## Run

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--workflow.rag.enabled=true --workflow.runner.enabled=true"
```
