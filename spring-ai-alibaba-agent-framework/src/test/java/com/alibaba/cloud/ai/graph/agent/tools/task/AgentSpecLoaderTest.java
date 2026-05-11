/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.tools.task;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentSpecLoader Tests")
class AgentSpecLoaderTest {

	@Test
	@DisplayName("Should parse agent spec from markdown string")
	void shouldParseAgentSpecFromMarkdown() {
		String markdown = """
				---
				name: Explore
				description: Fast agent for exploring codebases
				tools: Read, Grep, Glob
				model: sonnet
				---

				You are a file search specialist.
				""";

		AgentSpec spec = AgentSpecLoader.parse(markdown);

		assertThat(spec).isNotNull();
		assertThat(spec.name()).isEqualTo("Explore");
		assertThat(spec.description()).isEqualTo("Fast agent for exploring codebases");
		assertThat(spec.systemPrompt()).contains("You are a file search specialist");
		assertThat(spec.toolNames()).containsExactly("Read", "Grep", "Glob");
		assertThat(spec.model()).isEqualTo("sonnet");
	}

	@Test
	@DisplayName("Should return null for content without front matter")
	void shouldReturnNullForContentWithoutFrontMatter() {
		AgentSpec spec = AgentSpecLoader.parse("Just plain content");
		assertThat(spec).isNull();
	}

	@Test
	@DisplayName("Should load from classpath resource")
	void shouldLoadFromClasspathResource() throws IOException {
		Resource resource = new ClassPathResource("agent/explore-agent.md");
		AgentSpec spec = AgentSpecLoader.loadFromResource(resource);

		assertThat(spec).isNotNull();
		assertThat(spec.name()).isEqualTo("Explore");
		assertThat(spec.description()).contains("Fast agent");
		assertThat(spec.toolNames()).contains("grep", "glob", "read_file");
	}

	@Test
	@DisplayName("Should load from directory")
	void shouldLoadFromDirectory() throws IOException {
		Path agentsDir = Path.of("src/test/resources/agent");
		if (!agentsDir.toFile().exists()) {
			return;
		}
		var specs = AgentSpecLoader.loadFromDirectory(agentsDir.toString());
		assertThat(specs).isNotEmpty();
		assertThat(specs.stream().map(AgentSpec::name)).contains("Explore");
	}
}
