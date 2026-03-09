# Multi-Agent Workflow Examples

Custom workflow examples implementing patterns from `multiagents/custom.md`, `multiagents/code/rag-agent-workflow.md`, and `multiagents/code/sql-agent-workflow.md`.

## Packages

| Package | Description | Config |
|---------|-------------|--------|
| `ragagent` | RAG workflow: rewrite → retrieve → agent | `workflow.rag.enabled=true` |
| `sqlagent` | SQL agent with list_tables, get_schema, run_query tools | `workflow.sql.enabled=true` |

## RAG Agent

Flow: **Query → Rewrite → Retrieve → Prepare → Agent → Response**

- **Rewrite**: LLM rewrites the query for better retrieval
- **Retrieve**: Vector similarity search (deterministic)
- **Prepare**: Formats context and question into a prompt
- **Agent**: ReactAgent with context; can use `get_latest_news` tool

Requires: DashScope API key, EmbeddingModel (from spring-ai-alibaba-starter-dashscope).

## SQL Agent

Flow: **Question → Agent (list_tables → get_schema → run_query) → Answer**

- Uses H2 in-memory with Chinook-like schema
- Agent uses three tools: `sql_db_list_tables`, `sql_db_schema`, `sql_db_query`
- Only SELECT queries allowed (no DML)

## Run

```bash
# RAG agent
mvn spring-boot:run -Dspring-boot.run.arguments="--workflow.rag.enabled=true --workflow.runner.enabled=true"

# SQL agent
mvn spring-boot:run -Dspring-boot.run.arguments="--workflow.sql.enabled=true --workflow.runner.enabled=true"
```

Set `AI_DASHSCOPE_API_KEY` environment variable.
