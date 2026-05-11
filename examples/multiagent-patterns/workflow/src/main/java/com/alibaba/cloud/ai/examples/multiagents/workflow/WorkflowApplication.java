/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Multi-agent workflow examples: RAG agent and SQL agent.
 * Enable specific workflows via application.yml:
 * - workflow.rag.enabled=true for RAG agent
 * - workflow.sql.enabled=true for SQL agent
 */
@SpringBootApplication
public class WorkflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkflowApplication.class, args);
	}

	@Bean
	public ApplicationListener<ApplicationReadyEvent> applicationReadyEventListener(Environment environment) {
		return event -> {
			String port = environment.getProperty("server.port", "8080");
			String contextPath = environment.getProperty("server.servlet.context-path", "");
			String accessUrl = "http://localhost:" + port + contextPath + "/chatui/index.html";
			System.out.println("\n🎉========================================🎉");
			System.out.println("✅ Workflow (RAG / SQL agent) example is ready!");
			System.out.println("🚀 Chat with agents: " + accessUrl);
			System.out.println("   (Enable workflow.rag.enabled or workflow.sql.enabled in application.yml)");
			System.out.println("🎉========================================🎉\n");
		};
	}
}
