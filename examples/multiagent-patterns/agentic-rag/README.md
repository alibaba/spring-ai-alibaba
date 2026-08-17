# Agentic RAG Example

This example implements the **agentic RAG** pattern with Spring AI Alibaba: instead of
running a fixed retrieval pipeline for every question, the model itself decides whether
to retrieve, and a quality gate re-runs retrieval with a refined query until the answer
is complete.

## Architecture

```
                    ┌────────────┐
       question ──▶ │  classify  │  LLM: does the question need retrieval?
                    └─────┬──────┘
              retrieve   │         answer (no retrieval)
              ┌──────────▼──────┐
              │    retrieve     │  vector similarity search (no LLM)
              └──────────┬──────┘
                         ▼
                    ┌────────────┐
                    │   answer   │  LLM: answer from retrieved context
                    └─────┬──────┘
                          ▼
                    ┌────────────┐
                    │   check    │  LLM: is the answer complete and grounded?
                    └─────┬──────┘
                retry     │         done
      (refined query ┌────▼─────┐
       + retry_count)│   END    │
                     └──────────┘
```

The three LLM decision points are what make this pattern **agentic**:

1. **classify** — routes to `retrieve` or answers directly, so conversational questions
   never touch the vector store.
2. **answer** — answers strictly from the retrieved context and says so when the
   information is not available.
3. **check** — a quality gate that detects incomplete answers, writes a more specific
   `search_query`, and routes back to `retrieve`. Up to **2 retries** (`CheckNode.MAX_RETRIES`),
   so a question can trigger up to 3 retrieval rounds before the agent gives up.

This contrasts with the fixed-pipeline RAG workflow in the `workflow` example
(`rewrite → retrieve → prepare → agent`): there, retrieval always runs exactly once;
here, the number of retrieval rounds (including zero) is decided at runtime by the model.

## Design choices

1. **Independent Q&A turns**  
   Each `AgenticRagService.run()` call invokes the graph with a fresh
   `RunnableConfig` thread id, because compiled graphs register a `MemorySaver` checkpoint
   saver by default, which replays state from a previous run on the same thread id.
   The graph is also compiled with `releaseThread(true)` so each one-shot thread's
   checkpoint is released when the run completes, avoiding unbounded memory growth.

2. **Quality gate only after retrieval**  
   The `check` node is skipped entirely when the router answered without retrieval,
   so conversational questions never trigger a spurious retry loop.

3. **Bounded retries**  
   `CheckNode.MAX_RETRIES` (default `2`) bounds the answer → check → refine → retrieve
   loop, so a question can trigger at most 3 retrieval rounds.

## Demo scenario

The knowledge base is an in-memory `SimpleVectorStore` seeded with 6 documents about a
fictional SaaS product ("Acme Analytics"): pricing plans, refund policies, API rate
limits, features, and support. Swap it for a persistent store (PostgreSQL + pgvector,
Redis, ...) in production.

The three runner questions demonstrate the pattern:

| Question | What it shows |
|----------|---------------|
| `What is the refund policy for the annual plan?` | Retrieval needed; answered from the knowledge base. |
| `What is your name and what can you help me with?` | Router answers directly — zero retrieval rounds. |
| `Compare the refund policy of the annual plan with the monthly plan.` | May need 2+ retrieval rounds when the first pass only finds one of the two policies. |

## Project layout

```
examples/multiagent-patterns/agentic-rag/
├── README.md
├── pom.xml
└── src/main/
    ├── java/.../agenticrag/
    │   ├── AgenticRagApplication.java      # Spring Boot entry
    │   ├── AgenticRagConfig.java           # Beans: vector store, graph, service, Studio agent
    │   ├── AgenticRagService.java          # Runs a question through the graph
    │   ├── AgenticRagRunner.java           # Optional demo runner (see below)
    │   ├── AgentStaticLoader.java          # Registers the entry agent for Studio
    │   ├── node/
    │   │   ├── ClassifyNode.java           # Router: retrieve or answer directly
    │   │   ├── RetrieveNode.java           # Vector similarity search
    │   │   ├── AnswerNode.java             # Answer from retrieved context
    │   │   └── CheckNode.java              # Quality gate: complete, or retry with a refined query
    │   └── tools/
    │       └── AgenticRagTools.java        # answer_question tool (Studio entry agent)
    └── resources/
        └── application.yml
```

## How to run

### Prerequisites

- JDK 17+
- Maven 3.6+
- **DashScope API key** for the chat model and the embedding model.

Set your API key:

```bash
export AI_DASHSCOPE_API_KEY=your-dashscope-api-key
```

### Build

From the repository root:

```bash
./mvnw -f examples/multiagent-patterns/agentic-rag -B package -DskipTests
```

### Run the application

The demo runner is disabled by default, but the application always seeds the in-memory
vector store at startup, which sends the 6 knowledge-base documents through the
configured embedding model — so a valid API key is required even without the runner:

```bash
java -jar examples/multiagent-patterns/agentic-rag/target/agentic-rag-0.0.1-SNAPSHOT.jar
```

To run the **three demo scenarios** on startup:

```bash
./mvnw -f examples/multiagent-patterns/agentic-rag spring-boot:run \
  -Dspring-boot.run.arguments="--agentic-rag.runner.enabled=true"
```

The runner logs each question, the number of retrieval rounds performed, and the final
answer.

### Using the agentic RAG service in your own code

```java
AgenticRagService service = ...; // or @Autowired

AgenticRagService.AgenticRagResult result = service.run("What is the refund policy for the annual plan?");
String answer = result.answer();          // final answer
int rounds = result.retrievalRounds();    // 0 = answered without retrieval
```

## Configuration

- **`spring.ai.dashscope.api-key`**  
  Required for the chat model and the embedding model. Defaults to the
  `AI_DASHSCOPE_API_KEY` environment variable.

- **`agentic-rag.runner.enabled`**  
  If `true`, runs the three demo scenarios on startup. Default: `false`.

- **`CheckNode.MAX_RETRIES`**  
  Maximum number of retrieval retries after the first round (default `2`). Edit the
  constant in `CheckNode` to change it.
