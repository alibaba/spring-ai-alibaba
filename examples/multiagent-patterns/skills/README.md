# Skills (Progressive Disclosure) SQL Assistant Example

This example implements the **skills** (progressive disclosure) pattern with Spring AI Alibaba using the **framework's built-in Skill support**. A single SQL assistant agent is aware of available skills via descriptions in the system prompt and loads full skill content on demand with the framework-provided **`read_skill`** tool.

## Architecture

- **Framework components**  
  - **ClasspathSkillRegistry**: Loads skills from classpath `skills/` (each skill is a directory with a `SKILL.md` file; frontmatter defines `name` and `description`, body is full content).  
  - **SkillsAgentHook**: Provides the `read_skill` tool and registers **SkillsInterceptor** so the system prompt is augmented with an “Available Skills” section (descriptions only).  
  - The hook’s tools and model interceptors are merged into the agent automatically.

- **Progressive disclosure**  
  - The model sees only skill **descriptions** in the prompt (e.g. “sales_analytics: Database schema and business logic for sales data…”).  
  - When the user asks for something that requires schema or examples, the agent calls **read_skill(skill_name)** to load the full SKILL.md content (tables, business logic, example queries).  
  - This keeps the initial context small and avoids loading all skill text upfront.

- **Skills in this example**  
  Two skills under `src/main/resources/skills/`: **sales_analytics** (customers, orders, revenue) and **inventory_management** (products, warehouses, stock). Each has a `SKILL.md` with YAML frontmatter and markdown body.

## Design choices (aligned with framework API)

1. **SkillRegistry**  
   Uses **ClasspathSkillRegistry.builder().classpathPath("skills").build()** so skills are loaded from `src/main/resources/skills/` (and the same path inside the packaged JAR). No custom in-memory skill list.

2. **SkillsAgentHook**  
   **SkillsAgentHook.builder().skillRegistry(registry).build()** provides the `read_skill` tool and the interceptor that injects skill descriptions into the system message. No custom LoadSkillTool or SkillInterceptor.

3. **Agent configuration**  
   ReactAgent is built with **.hooks(List.of(skillsAgentHook))**; no need to add tools or interceptors explicitly—the hook contributes both.

4. **SKILL.md format**  
   Each skill directory contains one `SKILL.md` with frontmatter (`name`, `description`) and a markdown body (schema, business logic, example queries). Same format as in `AgentSkillsTest` and framework test resources.

## Project layout

```
examples/multiagents/skills/
├── README.md
├── pom.xml
└── src/main/
    ├── java/.../skills/
    │   ├── SkillsApplication.java
    │   ├── SkillsConfig.java           # SkillRegistry, SkillsAgentHook, sqlAssistantAgent
    │   └── SkillsRunner.java           # optional demo runner
    └── resources/
        ├── application.yml
        └── skills/                     # classpath:skills (framework format)
            ├── sales_analytics/
            │   └── SKILL.md
            └── inventory_management/
                └── SKILL.md
```

## How to run

### Prerequisites

- JDK 17+
- Maven 3.6+
- **DashScope API key** for the chat model.

Set it:

```bash
export AI_DASHSCOPE_API_KEY=your-dashscope-api-key
```

### Build

From the repo root:

```bash
./mvnw -pl :skills -am -B package -DskipTests
```

Or from this directory:

```bash
cd examples/multiagents/skills
mvn -B package -DskipTests
```

### Run the application

Default: the app starts **without** running the demo:

```bash
java -jar target/skills-0.0.1-SNAPSHOT.jar
# or
./mvnw -pl :skills spring-boot:run
```

To run the **demo** on startup (one user query that should trigger `read_skill("sales_analytics")` and then generate a SQL query):

Set:

```bash
export skills.runner.enabled=true
# or in application.yml: skills.runner.enabled: true
```

Then start the app. The runner sends: “Write a SQL query to find all customers who made orders over $1000 in the last month” and logs the assistant reply.

### Using the SQL assistant in your own code

Inject the agent and call it with a string or user message:

```java
@Qualifier("sqlAssistantAgent")
@Autowired
ReactAgent sqlAssistantAgent;

AssistantMessage response = sqlAssistantAgent.call(
    "Write a SQL query to find products below reorder point"
);
String text = response.getText();
```

The agent will use the skill descriptions to decide when to call **read_skill** (e.g. `inventory_management` for reorder-point queries) and then generate the SQL.

## Configuration

- **`spring.ai.dashscope.api-key`**  
  Required. Defaults to `AI_DASHSCOPE_API_KEY` env var.

- **`skills.runner.enabled`**  
  If `true`, runs the single-query demo on startup. Default: `false`.

## Example flow

1. User: “Write a SQL query to find all customers who made orders over $1000 in the last month.”
2. Agent sees “Available Skills” in the system prompt (sales_analytics, inventory_management descriptions) via SkillsInterceptor.
3. Agent calls **read_skill("sales_analytics")** (framework tool from SkillsAgentHook) to get the full SKILL.md content.
4. Tool returns the sales_analytics content (customers, orders, revenue rules, example query).
5. Agent uses that content to generate a SQL query and replies to the user.
