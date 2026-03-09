# Supervisor Personal Assistant Example

This example implements the **supervisor** pattern with Spring AI Alibaba. A central supervisor agent coordinates specialized agents (calendar and email) by calling them as **tools** via `AgentTool`.

## Architecture

- **Supervisor (main agent)**  
  Receives user requests, decides which specialized agent(s) to call, and synthesizes results. It only sees high-level tools: `schedule_event` and `manage_email`.

- **Calendar agent**  
  Handles scheduling: parses natural language (e.g. "next Tuesday at 2pm"), checks availability, and creates events. Exposed to the supervisor as the tool `schedule_event` with a single string input (the user's scheduling request).

- **Email agent**  
  Handles email: composes and sends messages from natural language. Exposed as the tool `manage_email` with a single string input (the user's email request).

Specialized agents are **stateless** from the user's perspective; the supervisor keeps the conversation and delegates one-off tasks to them. Each specialized agent runs in a focused context (its own instruction + the request string).

## Design choices (aligned with the reference)

1. **Specialized agents as tools**  
   Calendar and email agents are wrapped with `AgentTool.getFunctionToolCallback(agent)` so the supervisor invokes them as tools.

2. **Instruction and input type**  
   Each specialized agent has:
   - **Instruction**: system behavior (e.g. "You are a calendar scheduling assistant…", "You are an email assistant…").
   - **inputType(String.class)**: the supervisor passes a single natural-language request string; the framework wraps it as the tool's `input` parameter.

3. **Tool-per-agent**  
   One tool per specialized agent (`schedule_event`, `manage_email`) for clear routing and descriptions.

4. **Stub APIs**  
   Calendar and email "API" calls are stubbed (e.g. in `CalendarStubTools`, `EmailStubTools`). In production you would replace these with real calendar/email integrations.

## Project layout

```
examples/multiagent-patterns/supervisor/
├── README.md
├── pom.xml
└── src/main/
    ├── java/.../supervisor/
    │   ├── SupervisorApplication.java      # Spring Boot entry
    │   ├── SupervisorConfig.java           # Beans: calendarAgent, emailAgent, supervisorAgent
    │   ├── SupervisorRunner.java           # Optional demo runner (see below)
    │   └── tools/
    │       ├── CalendarStubTools.java     # create_calendar_event, get_available_time_slots
    │       └── EmailStubTools.java         # send_email
    └── resources/
        └── application.yml
```

## How to run

### Prerequisites

- JDK 17+
- Maven 3.6+
- **DashScope API key** for the chat model (used by both supervisor and specialized agents).

Set your API key:

```bash
export AI_DASHSCOPE_API_KEY=your-dashscope-api-key
```

### Build

From the repo root:

```bash
./mvnw -pl :supervisor -am -B package -DskipTests
```

Or from this directory (if the parent POM is available):

```bash
cd examples/multiagent-patterns/supervisor
mvn -B package -DskipTests
```

### Run the application

Default: the app starts **without** calling the model (no demo run):

```bash
java -jar target/supervisor-0.0.1-SNAPSHOT.jar
# or
./mvnw -pl :supervisor spring-boot:run
```

To run the **two demo scenarios** on startup (same as in the reference doc):

1. **Single-domain**: "Schedule a team standup for tomorrow at 9am" (calendar only).  
2. **Multi-domain**: "Schedule a meeting with the design team next Tuesday at 2pm for 1 hour, and send them an email reminder about reviewing the new mockups." (calendar + email).

Set:

```bash
export supervisor.run-examples=true
# or add to application.yml: supervisor.run-examples: true
```

Then start the app as above. The runner will call the supervisor with these two user messages and log the assistant replies.

### Using the supervisor in your own code

Inject the supervisor agent and call it with a user message:

```java
@Qualifier("supervisorAgent")
@Autowired
ReactAgent supervisorAgent;

// Single turn
AssistantMessage response = supervisorAgent.call(new UserMessage("Schedule a meeting tomorrow at 10am"));
String text = response.getText();
```

You can also use `supervisorAgent.call(List<Message> messages)` or `supervisorAgent.call(Map<String, Object> inputs)` for more control over state and history.

## Configuration

- **`spring.ai.dashscope.api-key`**  
  Required for the chat model (supervisor and specialized agents). Defaults to `AI_DASHSCOPE_API_KEY` env var.

- **`supervisor.run-examples`**  
  If `true`, runs the two demo scenarios on startup. Default: `false`.

## Example flow (multi-domain request)

1. User: "Schedule a meeting with the design team next Tuesday at 2pm for 1 hour, and send them an email reminder about reviewing the new mockups."
2. Supervisor decides to call two tools: `schedule_event` and `manage_email`.
3. **schedule_event** (calendar agent): receives the scheduling part, may call stub tools `get_available_time_slots` and `create_calendar_event`, returns a short confirmation text.
4. **manage_email** (email agent): receives the email part, calls stub `send_email`, returns a short confirmation text.
5. Supervisor combines both confirmations and replies to the user.

This mirrors the reference example's flow: supervisor routes to specialized agents, each agent runs in isolation with its own instruction and tools, and only the final assistant message is shown to the user.
