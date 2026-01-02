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
package com.alibaba.cloud.ai.graph.agent.hook.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for a Claude-style Skill.
 * 
 * A Skill is a reusable package of instructions and context that extends the LLM's capabilities.
 * Skills are automatically discovered and used by the LLM when relevant to the user's request.
 */
public class SkillMetadata {

	private String name;

	private String description;

	private String skillPath;

	private List<String> allowedTools;

	private String model;

	// Lazy-loaded full content
	private String fullContent;

	public SkillMetadata() {
		this.allowedTools = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSkillPath() {
		return skillPath;
	}

	public void setSkillPath(String skillPath) {
		this.skillPath = skillPath;
	}

	public List<String> getAllowedTools() {
		return allowedTools;
	}

	public void setAllowedTools(List<String> allowedTools) {
		this.allowedTools = allowedTools != null ? allowedTools : new ArrayList<>();
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	/**
	 * Load the full content of the SKILL.md file.
	 * The content is cached after the first load (lazy loading).
	 * 
	 * @return the full content of SKILL.md (without frontmatter)
	 * @throws IOException if the skill file cannot be read
	 */
	public String loadFullContent() throws IOException {
		if (fullContent == null) {
			Path skillFile = Path.of(skillPath, "SKILL.md");
			if (!Files.exists(skillFile)) {
				throw new IOException("SKILL.md not found at: " + skillFile);
			}
			
			String rawContent = Files.readString(skillFile);
			fullContent = removeFrontmatter(rawContent);
		}
		return fullContent;
	}

	/**
	 * Remove YAML frontmatter from the skill content.
	 * Frontmatter is delimited by --- at the start and end.
	 */
	private String removeFrontmatter(String content) {
		if (!content.startsWith("---")) {
			return content;
		}
		
		int endIndex = content.indexOf("---", 3);
		if (endIndex == -1) {
			return content;
		}
		
		return content.substring(endIndex + 3).trim();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final SkillMetadata metadata = new SkillMetadata();

		public Builder name(String name) {
			metadata.name = name;
			return this;
		}

		public Builder description(String description) {
			metadata.description = description;
			return this;
		}

		public Builder skillPath(String skillPath) {
			metadata.skillPath = skillPath;
			return this;
		}

		public Builder allowedTools(List<String> allowedTools) {
			metadata.allowedTools = allowedTools != null ? new ArrayList<>(allowedTools) : new ArrayList<>();
			return this;
		}

		public Builder model(String model) {
			metadata.model = model;
			return this;
		}

		public SkillMetadata build() {
			if (metadata.name == null || metadata.name.isEmpty()) {
				throw new IllegalStateException("Skill name is required");
			}
			if (metadata.description == null || metadata.description.isEmpty()) {
				throw new IllegalStateException("Skill description is required");
			}
			if (metadata.skillPath == null || metadata.skillPath.isEmpty()) {
				throw new IllegalStateException("Skill path is required");
			}
			return metadata;
		}
	}

	@Override
	public String toString() {
		return "SkillMetadata{" +
				"name='" + name + '\'' +
				", description='" + description + '\'' +
				", skillPath='" + skillPath + '\'' +
				", allowedTools=" + allowedTools +
				", model='" + model + '\'' +
				'}';
	}
}
