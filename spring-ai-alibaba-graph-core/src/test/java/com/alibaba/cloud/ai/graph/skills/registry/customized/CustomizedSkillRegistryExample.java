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
package com.alibaba.cloud.ai.graph.skills.registry.customized;

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.AbstractSkillRegistry;

import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example implementation of SkillRegistry that demonstrates how to integrate
 * with a third-party backend system via API calls.
 *
 * <p>This is a <b>test/example implementation</b> showing how users can extend
 * SkillRegistry to integrate with their own backend systems. It demonstrates:
 * <ul>
 *   <li>Loading skills from a backend API</li>
 *   <li>Parsing JSON responses</li>
 *   <li>Caching skills and resources locally</li>
 *   <li>Implementing all required SkillRegistry interface methods</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * StoreSkillRegistryExample registry = StoreSkillRegistryExample.builder()
 *     .basePath("/tmp/skills")
 *     .apiUrl("https://api.example.com/skills")
 *     .build();
 * }</pre>
 *
 * <p><b>Note:</b> This implementation uses a mock API call. In a real implementation,
 * you would replace {@link #fetchSkillFromApi(String)} with an actual HTTP client
 * call to your backend system.
 */
public class CustomizedSkillRegistryExample extends AbstractSkillRegistry {

	private static final Logger logger = LoggerFactory.getLogger(CustomizedSkillRegistryExample.class);

	private final String basePath;
	private final String apiUrl;
	private final ObjectMapper objectMapper = new ObjectMapper();

	private CustomizedSkillRegistryExample(Builder builder) {
		this.basePath = builder.basePath;
		this.apiUrl = builder.apiUrl;

		// Load skills during initialization
		loadSkillsToRegistry();
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Simulates an API call to fetch skill data.
	 * In a real implementation, this would make an HTTP request to the backend.
	 *
	 * <p>Example API response format:
	 * <pre>{@code
	 * {
	 *   "code": 200,
	 *   "message": "success",
	 *   "data": {
	 *     "name": "my_skill",
	 *     "description": "技能描述",
	 *     "fullContent": "详细的 Agent 指令...",
	 *     "resource": {
	 *       "references/split-script.py": {
	 *         "name": "split-script.py",
	 *         "type": "reference",
	 *         "content": "资源内容..."
	 *       }
	 *     }
	 *   }
	 * }
	 * }</pre>
	 *
	 * @param skillName the name of the skill to fetch
	 * @return JSON response string
	 */
	private String fetchSkillFromApi(String skillName) {
		// TODO: Replace with actual API call using HTTP client (e.g., RestTemplate, WebClient)
		// For now, return a mock response
		return String.format(
				"{\n" +
						"  \"code\": 200,\n" +
						"  \"message\": \"success\",\n" +
						"  \"data\": {\n" +
						"    \"name\": \"%s\",\n" +
						"    \"description\": \"技能描述 for %s\",\n" +
						"    \"fullContent\": \"# %s\\n\\n详细的 Agent 指令内容...\",\n" +
						"    \"resource\": {\n" +
						"      \"references/split-script.py\": {\n" +
						"        \"name\": \"split-script.py\",\n" +
						"        \"type\": \"reference\",\n" +
						"        \"content\": \"#!/usr/bin/env python3\\n# Resource content for %s\"\n" +
						"      }\n" +
						"    }\n" +
						"  }\n" +
						"}",
				skillName, skillName, skillName, skillName
		);
	}

	/**
	 * Saves a resource to the local filesystem.
	 * Resources are saved under basePath with the resource key as the subpath.
	 *
	 * @param resourceKey the resource key (e.g., "references/split-script.py")
	 * @param resourceContent the content of the resource
	 * @throws IOException if the file cannot be written
	 */
	private void saveResourceToDisk(String resourceKey, String resourceContent) throws IOException {
		Path resourcePath = Path.of(basePath, resourceKey);
		Files.createDirectories(resourcePath.getParent());
		Files.writeString(resourcePath, resourceContent);
		logger.debug("Saved resource to: {}", resourcePath);
	}

	@Override
	protected void loadSkillsToRegistry() {
		Map<String, SkillMetadata> newSkills = new HashMap<>();
		// TODO: In real implementation, fetch list of available skills from API
		// Example: GET {apiUrl}/skills to get list of skill names
		// For now, simulate fetching a single skill
		List<String> skillNames = List.of("my_skill"); // Mock: should come from API

		for (String skillName : skillNames) {
			try {
				// Simulate API call
				String jsonResponse = fetchSkillFromApi(skillName);

				// Parse JSON response
				JsonNode rootNode = objectMapper.readTree(jsonResponse);

				if (rootNode.get("code").asInt() != 200) {
					logger.warn("Failed to fetch skill {}: {}", skillName, rootNode.get("message").asText());
					continue;
				}

				JsonNode dataNode = rootNode.get("data");
				if (dataNode == null) {
					logger.warn("No data field in response for skill: {}", skillName);
					continue;
				}

				// Extract skill information
				String name = dataNode.get("name").asText();
				String description = dataNode.get("description").asText();
				String fullContent = dataNode.has("fullContent") ? dataNode.get("fullContent").asText() : "";

				// Create skill directory path
				Path skillDir = Path.of(basePath, name);
				String skillPath = skillDir.toString();

				// Save resources to disk
				if (dataNode.has("resource") && dataNode.get("resource").isObject()) {
					JsonNode resourceNode = dataNode.get("resource");
					Iterator<Map.Entry<String, JsonNode>> fields = resourceNode.fields();

					while (fields.hasNext()) {
						Map.Entry<String, JsonNode> entry = fields.next();
						String resourceKey = entry.getKey();
						JsonNode resourceData = entry.getValue();

						if (resourceData.has("content")) {
							String resourceContent = resourceData.get("content").asText();
							saveResourceToDisk(resourceKey, resourceContent);
						}
					}
				}

				// Create SkillMetadata
				SkillMetadata skill = SkillMetadata.builder()
						.name(name)
						.description(description)
						.skillPath(skillPath)
						.fullContent(fullContent)
						.source("customized")
						.build();

				// Store skill and its content
				newSkills.put(name, skill);

				logger.info("Loaded skill: {} from customized", name);

			}
			catch (Exception e) {
				logger.error("Failed to load skill {} from customized: {}", skillName, e.getMessage(), e);
			}
		}

		logger.info("Loaded {} skills from customized", newSkills.size());
		this.skills = newSkills;
	}

	@Override
	public String readSkillContent(String name) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Skill name cannot be null or empty");
		}

		// Get the skill by name
		Optional<SkillMetadata> skillOpt = get(name);
		if (skillOpt.isEmpty()) {
			throw new IllegalStateException("Skill not found: " + name);
		}

		return skillOpt.get().getFullContent();
	}

	@Override
	public String getSkillLoadInstructions() {
		StringBuilder instructions = new StringBuilder();
		instructions.append("**Skill Source:**\n");
		instructions.append("- Skills are loaded from a backend customized via API\n");
		instructions.append("- Skills and resources are cached locally at: `").append(basePath).append("`\n");
		instructions.append("\n");
		instructions.append("**Skill Path Format:**\n");
		instructions.append("Each skill has a unique path shown in the skill list above. ");
		instructions.append("Use the skill id shown in the skill list when calling `read_skill` to read the skill content.\n");
		instructions.append("Use absolute path to read the skill supporting files (scripts, references, etc.).\n");

		return instructions.toString();
	}

	@Override
	public String getRegistryType() {
		return "Store";
	}

	@Override
	public SystemPromptTemplate getSystemPromptTemplate() {
		// Use a simple default template for Store registry
		String template = """
				## Skills System
				
				You have access to a skills library that provides specialized capabilities and domain knowledge. 
				All skills are stored in a Store-based Skill Registry.
				
				### Available Skills
				
				{skills_list}
				
				### How to Use Skills
				
				Use the `read_skill` tool to read the full content of any skill by its name.
				
				{skills_load_instructions}
				""";

		return SystemPromptTemplate.builder()
				.template(template)
				.build();
	}

	@Override
	public List<SkillMetadata> listAll() {
		return new ArrayList<>(skills.values());
	}

	@Override
	public boolean contains(String name) {
		return skills.containsKey(name);
	}

	@Override
	public int size() {
		return skills.size();
	}

	public static class Builder {
		private String basePath;
		private String apiUrl;

		/**
		 * Sets the base path for storing skills and resources locally.
		 *
		 * @param basePath the base directory path
		 * @return this builder
		 */
		public Builder basePath(String basePath) {
			this.basePath = basePath;
			return this;
		}

		/**
		 * Sets the API URL for fetching skills from the backend customized.
		 *
		 * @param apiUrl the API endpoint URL
		 * @return this builder
		 */
		public Builder apiUrl(String apiUrl) {
			this.apiUrl = apiUrl;
			return this;
		}

		/**
		 * Builds the StoreSkillRegistryExample instance.
		 *
		 * @return a new StoreSkillRegistryExample instance
		 */
		public CustomizedSkillRegistryExample build() {
			if (basePath == null || basePath.isEmpty()) {
				throw new IllegalArgumentException("basePath is required");
			}
			if (apiUrl == null || apiUrl.isEmpty()) {
				throw new IllegalArgumentException("apiUrl is required");
			}
			return new CustomizedSkillRegistryExample(this);
		}
	}
}
