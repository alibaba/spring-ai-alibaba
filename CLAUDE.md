# CLAUDE.md - AI Assistant Guide for Spring AI Alibaba

This file provides guidance for AI assistants working with the Spring AI Alibaba codebase.

## Project Overview

Spring AI Alibaba is a production-ready framework for building Agentic, Workflow, and Multi-agent applications. It is an implementation of the Spring AI framework tailored for Alibaba Cloud services and components. It provides a comprehensive ecosystem for developing AI-powered applications with built-in context engineering and human-in-the-loop support.

**Key Features:**

- Multi-Agent Orchestration with built-in patterns
- Context Engineering with human-in-the-loop, context compaction, editing, model call limits
- Graph-based workflow with conditional routing, nested graphs, parallel execution
- A2A (Agent-to-Agent) support with Nacos integration
- Rich model support (DashScope, OpenAI, DeepSeek) and MCP (Model Context Protocol)
- One-stop visual agent platform

## Repository Structure

```
spring-ai-alibaba/
├── spring-ai-alibaba-agent-framework/ # Multi-agent framework (Sequential, Parallel, Routing, etc.)
├── spring-ai-alibaba-graph-core/      # Runtime providing persistence, workflow orchestration, state mgmt
├── spring-ai-alibaba-studio/          # Embedded UI for debugging agents visually
├── spring-ai-alibaba-admin/           # One-stop Agent platform (visual dev, observability, MCP mgmt)
├── spring-ai-alibaba-bom/             # Bill of Materials for dependency management
├── spring-boot-starters/              # Spring Boot Starters
│   ├── spring-ai-alibaba-starter-a2a-nacos/     # Nacos A2A communication
│   ├── spring-ai-alibaba-starter-builtin-nodes/ # Built-in workflow nodes
│   ├── spring-ai-alibaba-starter-config-nacos/  # Dynamic config with Nacos
│   └── spring-ai-alibaba-starter-graph-observation/ # Observability
├── examples/                          # Example applications
│   ├── chatbot/                       # Chatbot example
│   ├── deepresearch/                  # Deep research agent example
│   └── documentation/                 # Documentation examples
├── tools/                             # Build and linting tools
└── docs/                              # Documentation
```

## Build System

### Prerequisites

- **JDK**: 17 (Required by `java.version` property)
- **Maven**: 3.6+
- **Git**

### Common Build Commands

```shell
# Build the entire project (skip tests)
./mvnw -B package -DskipTests=true

# Build a specific module
./mvnw -pl :spring-ai-alibaba-agent-framework -B package -DskipTests=true

# Clean project
./mvnw clean

# Run tests
./mvnw test

# Run linting checks (using Makefile)
make lint
make licenses-check
```

## Architecture & Key Concepts

### Core Components

- **Agent Framework**: Built-in agents like `SequentialAgent`, `ParallelAgent`, `RoutingAgent`, `LoopAgent`, `SupervisorAgent`.
- **Graph Core**: Underlying engine for stateful agents, supporting persistence (PostgreSQL, MySQL, Oracle, MongoDB, Redis, File).
- **A2A (Agent-to-Agent)**: Enables agents to seek and communicate with each other using Nacos as a registry.
- **Admin & Studio**: Provides visual tools for developing and debugging agent workflows.

### Technology Stack

- **Framework**: Spring Boot 3.5.x, Spring AI 1.1.x
- **Cloud Integration**: Alibaba Cloud DashScope, Nacos (Service Discovery & Config)
- **Observability**: Spring Cloud Observation (Micrometer/OpenTelemetry)

## Code Style & Conventions

### General Guidelines

- Follow **Spring AI** standard code formatting.
- Use **Apache 2.0** license headers for all Java files.
- **Java 17** features are encouraged (records, switch expressions, text blocks).
- Avoid `System.out.println` - use SLF4J logging.
- Use `final` for local variables and parameters where appropriate.
- Use Lombok annotations (`@Data`, `@Slf4j`, etc.) to reduce boilerplate.

### Linting & Formatting

The project uses `make` for linting tasks:
- `make codespell`: Checks for spelling errors.
- `make yaml-lint`: Checks YAML file formatting.
- `make licenses-check`: Verifies license headers.

### License Header

```java
/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

## Testing

### Frameworks

- **JUnit 5** (`org.junit.jupiter`)
- **Mockito**

### Running Tests

```shell
# Run all tests
./mvnw test

# Run a specific test class
./mvnw -pl :<module-name> -Dtest=<TestClassName> test
```

## Tips for AI Assistants

1.  **JDK Version**: Project targets JDK 17. Use appropriate language features.
2.  **Spring Boot**: Uses Spring Boot 3.x. Be aware of `jakarta.*` namespace vs `javax.*`.
3.  **Dependencies**: Check `spring-ai-alibaba-bom` or parent pom for version management.
4.  **Makefile**: Use the Makefile in the root for project maintenance tasks (linting, license checks).
5.  **Structure**: When adding new features, prefer creating or updating modules within `spring-ai-alibaba-agent-framework` or `spring-boot-starters` depending on the scope.

## Important Links

- **Issues**: [https://github.com/alibaba/spring-ai-alibaba/issues](https://github.com/alibaba/spring-ai-alibaba/issues)
- **Source**: [https://github.com/alibaba/spring-ai-alibaba](https://github.com/alibaba/spring-ai-alibaba)
- **Contributing**: [CONTRIBUTING.md](CONTRIBUTING.md)
