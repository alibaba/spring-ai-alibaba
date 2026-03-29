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
package com.alibaba.cloud.ai.graph.skills.registry;

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;

import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Registry interface for managing Skills metadata.
 *
 * This interface provides read-only access to skills. Implementations may provide
 * additional methods for loading and managing skills from various sources (filesystem,
 * database, etc.).
 *
 * @see FileSystemSkillRegistry for filesystem-based implementation
 */
public interface SkillRegistry {

	/**
	 * Get a skill by name.
	 *
	 * @param name the skill name (must not be null)
	 * @return Optional containing the skill metadata if found, empty otherwise
	 */
	Optional<SkillMetadata> get(String name);

	/**
	 * Get a skill by its resolved skill path.
	 *
	 * @param skillPath the absolute or relative skill directory path
	 * @return Optional containing the skill metadata if found, empty otherwise
	 */
	Optional<SkillMetadata> getByPath(String skillPath);

	/**
	 * List all registered skills.
	 *
	 * @return a list of all skill metadata (may be empty, never null)
	 */
	List<SkillMetadata> listAll();

	/**
	 * Check if a skill exists by name.
	 *
	 * @param name the skill name (must not be null)
	 * @return true if the skill exists, false otherwise
	 */
	boolean contains(String name);

	/**
	 * Search skills by name, description, or path.
	 *
	 * @param query the search query
	 * @return matching skills in ranked order
	 */
	List<SkillMetadata> search(String query);

	/**
	 * Get the total number of registered skills.
	 *
	 * @return the number of skills
	 */
	int size();

	/**
	 * Disable a skill in the current registry instance.
	 *
	 * @param name the skill name
	 * @return true if the skill was disabled, false otherwise
	 */
	boolean disable(String name);

	/**
	 * Disable a skill in the current registry instance by path.
	 *
	 * @param skillPath the skill path
	 * @return true if the skill was disabled, false otherwise
	 */
	boolean disableByPath(String skillPath);

	/**
	 * Check whether a skill is disabled in the current registry instance.
	 *
	 * @param name the skill name
	 * @return true if disabled, false otherwise
	 */
	boolean isDisabled(String name);

	/**
	 * Reloads all skills from the underlying source.
	 *
	 * This method clears existing skills and reloads them from the configured source
	 * (e.g., filesystem, database, etc.). The exact behavior depends on the implementation.
	 *
	 * Implementations should ensure thread-safety when reloading skills.
	 *
	 * @throws UnsupportedOperationException if the implementation does not support reloading
	 */
	void reload();

	/**
	 * Reads the full content of a skill by name.
	 *
	 * This method retrieves the complete content of a skill file (e.g., SKILL.md)
	 * based on the skill name. The skill path is obtained from the SkillMetadata
	 * associated with the skill name.
	 *
	 * @param name the skill name (must not be null)
	 * @return the full content of the skill file
	 * @throws IOException if the skill file cannot be read
	 * @throws IllegalArgumentException if name is null or empty
	 * @throws IllegalStateException if the skill is not found
	 */
	String readSkillContent(String name) throws IOException;

	/**
	 * Reads the full content of a skill by path.
	 *
	 * @param skillPath the skill path
	 * @return the full content of the skill file
	 * @throws IOException if the skill file cannot be read
	 */
	String readSkillContentByPath(String skillPath) throws IOException;

	/**
	 * Gets the skill load instructions for the system prompt.
	 *
	 * This method provides instructions on how skills are loaded and organized,
	 * including information about skill locations, paths, and loading order.
	 * The format and content depend on the specific implementation.
	 *
	 * @return the skill load instructions as a formatted string (may be empty, never null)
	 */
	String getSkillLoadInstructions();

	/**
	 * Gets the registry type name.
	 *
	 * This method returns a human-readable name for the registry type (e.g., "FileSystem", "Database").
	 * This is used in system prompts to inform the model about the type of skill registry being used.
	 *
	 * @return the registry type name (never null)
	 */
	String getRegistryType();

	/**
	 * Gets the system prompt template for skills.
	 *
	 * This method returns a SystemPromptTemplate that defines how skills are presented
	 * in the system prompt. Each implementation can provide its own template format.
	 * The template should support the following variables:
	 * - {skills_registry}: The registry type name
	 * - {skills_list}: The formatted list of available skills
	 * - {skills_load_instructions}: Instructions on how skills are loaded
	 *
	 * @return the SystemPromptTemplate for skills (never null)
	 */
	SystemPromptTemplate getSystemPromptTemplate();
}
