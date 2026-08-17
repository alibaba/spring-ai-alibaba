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

1. **classify** — routes purely conversational questions straight to `answer`; every
   product question goes to `retrieve`, even when the knowledge base may not hold the
   answer (so the model reports missing information instead of inventing it).
2. **answer** — answers strictly from the retrieved context and says so when the
   information is not available.
3. **check** — a quality gate that detects incomplete answers, writes a more specific
   `search_query`, and routes back to `retrieve`. Up to **2 retries** (`CheckNode.MAX_RETRIES`),
   so a question can trigger up to 3 retrieval rounds before the agent gives up.

This contrasts with the fixed-pipeline RAG workflow in the `workflow` example
(`rewrite → retrieve → prepare → agent`): there, retrieval always runs exactly once;
here, the number of retrieval rounds (including zero) is decided at runtime by the model.

## Design choices

1. **One-shot turns, no checkpoint saver**  
   The graph is compiled with an empty `SaverConfig`. This demo treats every question
   as an independent turn: `graph.invoke()` always starts from a fresh state, so
   nothing from a previous run is replayed, and nothing is retained in memory after a
   run completes — whether it succeeded or failed.

2. **Documents accumulate across retrieval retries**  
   The `documents` state key uses `AppendStrategy` with deduplication, so each retry
   round adds its newly retrieved documents to the previous ones. A multi-part
   question (for example, "compare the annual and monthly refund policies") can
   combine facts retrieved in different rounds instead of seeing only the last round.

3. **Routing decision drives the prompt, not the document list**  
   The `answer` node chooses the conversational prompt only when the router decided
   to answer directly (`route == "answer"`). If the router chose retrieval but the
   store returned no matches, the answer is still generated from the grounded prompt
   and the quality gate still runs — the model reports missing information instead of
   inventing facts. `check` is skipped only for direct answers.

4. **Bounded retries**  
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
| `What is your name and what can you help me with?` | Purely conversational — router answers directly, zero retrieval rounds. |
| `Compare the refund policy of the annual plan with the monthly plan.` | May need 2+ retrieval rounds when the first pass only finds one of the two policies. |
| `Where is Acme Analytics customer data hosted?` | A product fact the knowledge base does not contain — still routed through retrieval, and the honest answer is "not in the knowledge base" instead of a guess. |

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
