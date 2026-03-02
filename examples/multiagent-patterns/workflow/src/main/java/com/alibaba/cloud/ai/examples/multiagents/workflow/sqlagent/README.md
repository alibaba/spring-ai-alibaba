# SQL Agent Workflow

Custom SQL agent using StateGraph.

## Flow

```
START → list_tables → call_get_schema → get_schema → generate_query(ReactAgent) → END
```

- **list_tables**: Deterministic node, creates synthetic tool call, returns available tables
- **call_get_schema**: LLM with `sql_db_schema` tool (forced tool choice)
- **get_schema**: ToolNode runs `sql_db_schema`
- **generate_query**: ReactAgent with `sql_db_query` tool; handles LLM↔tools loop until final answer

## Enable

```yaml
workflow.sql.enabled: true
workflow.runner.enabled: true  # optional demo on startup
```

## Database

Uses H2 in-memory with a Chinook-like schema. Schema is initialized from `schema-chinook.sql`.

## Run

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--workflow.sql.enabled=true --workflow.runner.enabled=true"
```
