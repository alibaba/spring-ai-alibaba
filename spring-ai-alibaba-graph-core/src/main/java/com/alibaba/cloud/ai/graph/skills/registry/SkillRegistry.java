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
	 * Get the total number of registered skills.
	 * 
	 * @return the number of skills
	 */
	int size();

	/**
	 * Get the project skills directory path.
	 * This may return null if not applicable for the implementation.
	 * 
	 * @return the project skills directory path, or null if not set
	 */
	String getProjectSkillsDirectory();

	/**
	 * Get the user skills directory path.
	 * This may return null if not applicable for the implementation.
	 * 
	 * @return the user skills directory path, or null if not set
	 */
	String getUserSkillsDirectory();

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
	 * Reads the full content of a skill by name and path.
	 * 
	 * This method retrieves the complete content of a skill file (e.g., SKILL.md)
	 * based on the skill name and path. The path is used to locate the skill file,
	 * while the name is used for validation.
	 * 
	 * @param name the skill name (must not be null)
	 * @param skillPath the path to the skill directory or file (must not be null)
	 * @return the full content of the skill file
	 * @throws IOException if the skill file cannot be read
	 * @throws IllegalArgumentException if name or skillPath is null
	 * @throws IllegalStateException if the skill is not found or path is invalid
	 */
	String readSkillContent(String name, String skillPath) throws IOException;
}
