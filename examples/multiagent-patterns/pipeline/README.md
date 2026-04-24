# Pipeline Examples

Pipeline examples demonstrating **SequentialAgent**, **ParallelAgent**, and **LoopAgent** from Spring AI Alibaba Agent Framework.

## Prerequisites

- JDK 17+
- `AI_DASHSCOPE_API_KEY` environment variable set (for DashScope API)

## Run

```bash
./mvnw spring-boot:run -pl examples/multiagent-patterns/pipeline
```

Then open http://localhost:8080/chatui/index.html to chat with the agents via Spring AI Alibaba Studio.

## Agents

### 1. SequentialAgent: `sequential_sql_agent`

**Business scenario:** Natural language to SQL generation pipeline.

- **SQL Generator** → Converts user's natural language to MySQL SQL
- **SQL Rater** → Scores how well the SQL matches user intent (0-1)

Sub-agents run in sequence; each output feeds the next.

**Example prompt:** "I have a user table with columns (id, name, email). Find all users whose name starts with 'A'."

---

### 2. ParallelAgent: `parallel_research_agent`

**Business scenario:** Multi-topic research.

Given a broad topic, researches from three angles in parallel:

- **Tech Researcher** → Technology perspective (trends, innovations)
- **Finance Researcher** → Finance/business perspective (market size, investments)
- **Market Researcher** → Industry/market perspective (competition, growth)

Results are merged into a single report.

**Example prompt:** "Research the current state of large language models"

---

### 3. LoopAgent: `loop_sql_refinement_agent`

**Business scenario:** Iterative SQL refinement until quality threshold.

Runs the SQL generation + rating pipeline in a loop until the quality score exceeds 0.5 (or max iterations).

- Each iteration: Generate SQL → Rate SQL
- Stops when score > 0.5

**Example prompt:** "Query the top 10 users from the user table ordered by created_at"
