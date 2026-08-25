# QA Assistant Example

This example implements a **multi-agent QA (Question & Answer) assistant** with Spring AI Alibaba. A central supervisor agent coordinates specialized agents (knowledge base and web search) by calling them as **tools** via `AgentTool`.

## Architecture

- **Supervisor (main agent)**  
  Receives user questions, decides which specialized agent(s) to call, and synthesizes results. It only sees high-level tools: `search_knowledge_base` and `search_web`.

- **Knowledge base agent**  
  Searches the enterprise knowledge base to find accurate answers to company-specific questions. Exposed to the supervisor as the tool `search_knowledge_base`.

- **Web search agent**  
  Searches the web for up-to-date information on general topics. Exposed as the tool `search_web`.

Specialized agents are **stateless** from the user's perspective; the supervisor keeps the conversation and delegates one-off tasks to them.

## Design choices (aligned with the reference)

1. **Specialized agents as tools**  
   Knowledge base and web search agents are wrapped with `AgentTool.getFunctionToolCallback(agent)` so the supervisor invokes them as tools.

2. **Instruction and input type**  
   Each specialized agent has:
   - **Instruction**: system behavior (e.g. "You are a knowledge base assistant…", "You are a web search assistant…").
   - **inputType(String.class)**: the supervisor passes a single natural-language query string; the framework wraps it as the tool's `input` parameter.

3. **Tool-per-agent**  
   One tool per specialized agent (`search_knowledge_base`, `search_web`) for clear routing and descriptions.

4. **Stub APIs**  
   Knowledge base and web search "API" calls are stubbed. In production you would replace these with real vector database / search engine integrations.

## Project layout

```
examples/multiagent-patterns/qa-assistant/
├── README.md
├── pom.xml
└── src/main/
    ├── java/.../qa/
    │   ├── QaAssistantApplication.java      # Spring Boot entry
    │   ├── QaAssistantConfig.java           # Beans: kbAgent, webAgent, qaSupervisorAgent
    │   ├── QaAssistantRunner.java           # Optional demo runner (see below)
    │   ├── QaAgentStaticLoader.java         # Exposes agent to Spring AI Alibaba Studio
    │   └── tools/
    │       ├── KnowledgeBaseTools.java      # search_knowledge_base, list_knowledge_categories
    │       └── WebSearchTools.java          # search_web, fetch_web_page
    └── resources/
        └── application.yml
```

## How to run

### Prerequisites

- JDK 17+
- Maven 3.6+
- **DashScope API key** for the chat model.

Set your API key:

```bash
export AI_DASHSCOPE_API_KEY=your-dashscope-api-key
```

### Build

From the repo root:

```bash
./mvnw -pl :qa-assistant -am -B package -DskipTests
```

Or from this directory (if the parent POM is available):

```bash
cd examples/multiagent-patterns/qa-assistant
mvn -B package -DskipTests
```

### Run the application

Default: the app starts **without** calling the model (no demo run):

```bash
java -jar target/qa-assistant-0.0.1-SNAPSHOT.jar
# or
./mvnw -pl :qa-assistant spring-boot:run
```

To run the **three demo scenarios** on startup:

1. **Knowledge base only**: "What is our company's remote work policy?" (KB only).  
2. **Web search only**: "What are the latest developments in AI agent frameworks in 2026?" (web only).  
3. **Hybrid**: "How does our product compare to competitors in the market?" (KB + web).

Set:

```bash
export qa-assistant.run-examples=true
# or add to application.yml: qa-assistant.run-examples: true
```

Then start the app as above. The runner will call the supervisor with these three user messages and log the assistant replies.

### Using the QA assistant in your own code

Inject the supervisor agent and call it with a user message:

```java
@Qualifier("qaSupervisorAgent")
@Autowired
ReactAgent qaSupervisorAgent;

// Single turn
AssistantMessage response = qaSupervisorAgent.call(new UserMessage("What is our refund policy?"));
String text = response.getText();
```

## Configuration

- **`spring.ai.dashscope.api-key`**  
  Required for the chat model. Defaults to `AI_DASHSCOPE_API_KEY` env var.

- **`qa-assistant.run-examples`**  
  If `true`, runs the three demo scenarios on startup. Default: `false`.

## Example flow (hybrid question)

1. User: "How does our product compare to competitors in the market?"
2. Supervisor decides to call two tools: `search_knowledge_base` and `search_web`.
3. **search_knowledge_base** (KB agent): finds product features from internal docs.
4. **search_web** (web agent): finds market analysis and competitor information.
5. Supervisor combines both results and replies to the user with a comprehensive answer.
