# Graph and Agentic RAG with Bailian

`BailianRagExample` uses one `DashScopeRagDocumentRetriever` for both
`StateGraph` and the `ReactAgent` search tool. It verifies that each cited
reference comes from the evidence supplied to the model and prints the
source name and chunk ID.

## Run the example

Use JDK 17+, Maven, a Beijing-region Bailian knowledge base, and a DashScope
API Key from its workspace. Wait for any document imports to finish.

Choose a question with a known answer, an exact phrase in its source
passage, and an initial search query whose top result is a different passage.
For example, index a warranty document containing `标准版设备享受 12 个月保修`
and a separate invoice document about `电子发票抬头和税号`.

From the repository root, set the environment variables and run the example.

```bash
export DASHSCOPE_API_KEY='<your DashScope API Key>'
export BAILIAN_WORKSPACE_ID='<workspace ID>'
export BAILIAN_INDEX_ID='<knowledge base ID>'
export BAILIAN_RAG_QUESTION='标准版设备的保修期是多久？'
export BAILIAN_RAG_EVIDENCE='12 个月'
export BAILIAN_RAG_INITIAL_QUERY='电子发票抬头和税号'

mvn -B -f examples/bailian-rag/pom.xml compile exec:java
```

Set `BAILIAN_RAG_MODEL` to use another model that supports tool calling.
The default is `qwen-plus`.

| Scenario | Expected status | Retrieval calls |
| --- | --- | --- |
| `graph.direct` | `answered` with a verified citation | 1 |
| `graph.rewrite` | `answered` with a verified citation after the model rewrites the query | 2 |
| `graph.insufficient` | `insufficient_evidence` | 2 |
| `agent.search` | `answered` with a verified citation | 1 |
| `agent.insufficient` | `insufficient_evidence` | 1 |
| `agent.greeting` | `answered` | 0 |

The demo uses a one-hit retrieval limit to make the initial distractor
observable. It checks the configured phrase in retrieved text as a
repeatable evidence predicate. If the initial query already finds the
target passage, choose a query for a different topic in the same index.

## Run locally with scripted responses

The tests exercise routing, the agent tool, and citation checks without
requiring credentials.

```bash
mvn -B -f examples/bailian-rag/pom.xml test
```
