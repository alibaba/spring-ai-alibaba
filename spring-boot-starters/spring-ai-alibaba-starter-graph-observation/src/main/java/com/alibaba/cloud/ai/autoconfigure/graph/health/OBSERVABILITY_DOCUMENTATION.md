# Observability Module

## Why This Exists

Running an AI agent in production is different from running a normal web application. If your server is slow, it's usually a code issue. If your AI agent is slow, it could be the provider, a bad prompt, or the agent stuck in a reasoning loop.

This module gives developers visibility into what happens inside the agent without writing any extra code.

## Requirements

**FR-1: Cost Control**  
The system tracks every token sent and received, allowing real-time cost estimation.

**FR-2: Performance Monitoring**  
The system measures how long the AI takes to respond and how often tools fail.

**FR-3: Startup Validation**  
The system checks that API keys are present and valid when the application starts. If something is missing, the app stops immediately instead of breaking when a user tries to use it.

**FR-4: Non-Invasive Integration**  
Metrics are collected automatically without modifying agent code.

## User Stories

**DevOps Engineer**: "I want alerts when AI costs exceed the monthly budget so we don't get surprised by the bill."

**Platform Engineer**: "I want to see which agents are slower than others to understand where to optimize prompts or switch providers."

**SRE**: "I want the system to tell me immediately if configuration is wrong before user traffic reaches the servers."

## Architecture

Instead of asking every developer to manually add timers in their code, we use AOP (Aspect-Oriented Programming). Think of it as an invisible observer sitting on top of every agent:

1. Watches the start - Timer starts when the agent receives a command
2. Analyzes content - Reads response metadata to understand token usage
3. Handles errors - Even if the AI crashes, the observer records the failure

The data flows through Micrometer to your monitoring system (Prometheus, Datadog, whatever you use).

## Design Decisions

**Why automatic integration?**  
To avoid human error. When a new developer creates a new agent, it gets monitored automatically without them having to remember to enable anything.

**Why separate agent and LLM metrics?**  
Because they're different problems. If the AI responds quickly but the agent takes too long to decide what to do, the problem is in our code. If the AI is slow, the problem is with the provider.

**Why fail at startup?**  
Better to find out immediately than when a user hits the endpoint. Kubernetes uses health checks to avoid routing traffic to broken pods.

**Why defensive metadata extraction?**  
Spring AI's internal structure varies between providers and versions. The code uses reflection and null checks because the Usage data might not always be there. If it's missing, we log and continue instead of crashing.

**Why opt-in cost tracking?**  
Cost calculation needs accurate pricing data. If we enable it by default with zero values, the numbers would be meaningless. You have to explicitly configure the per-million-token costs.

## Integration Points

The module hooks into three key points:

1. **Simple Agents**: When the AI responds to a single question (ReactAgent.call)
2. **Complex Workflows**: When the AI needs multiple reasoning steps (Sequential/Parallel/Loop agents)
3. **Provider Metadata**: Extracts hidden information from provider responses (Alibaba DashScope, etc)

All interception happens in finally blocks so metrics get recorded even when exceptions occur. This guarantees accurate failure rate tracking.

## Configuration

It's enabled by default. Just add the dependency and it works.

To enable cost tracking:
```yaml
spring.ai.alibaba.observability.enable-cost-estimation: true
spring.ai.alibaba.observability.pricing.input-token-cost-per-million: 0.04
spring.ai.alibaba.observability.pricing.output-token-cost-per-million: 0.12
```

Metrics show up at `/actuator/prometheus`. Hook that into your monitoring system and you're done.
