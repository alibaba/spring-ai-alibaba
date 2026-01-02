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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing Skills.
 * 
 * Provides methods to register, retrieve, and match skills based on user requests.
 */
public class SkillRegistry {

	private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);

	private final Map<String, SkillMetadata> skills = new ConcurrentHashMap<>();

	/**
	 * Register a skill.
	 * 
	 * @param skill the skill metadata to register
	 */
	public void register(SkillMetadata skill) {
		if (skill == null) {
			throw new IllegalArgumentException("Skill cannot be null");
		}
		
		String name = skill.getName();
		if (skills.containsKey(name)) {
			logger.warn("Skill '{}' is already registered, overwriting", name);
		}
		
		skills.put(name, skill);
		logger.debug("Registered skill: {}", name);
	}

	/**
	 * Register multiple skills.
	 * 
	 * @param skillList the list of skills to register
	 */
	public void registerAll(List<SkillMetadata> skillList) {
		if (skillList == null) {
			return;
		}
		skillList.forEach(this::register);
	}

	/**
	 * Get a skill by name.
	 * 
	 * @param name the skill name
	 * @return the skill metadata, or empty if not found
	 */
	public Optional<SkillMetadata> get(String name) {
		return Optional.ofNullable(skills.get(name));
	}

	/**
	 * List all registered skills.
	 * 
	 * @return list of all skill metadata
	 */
	public List<SkillMetadata> listAll() {
		return new ArrayList<>(skills.values());
	}

	/**
	 * Check if a skill is registered.
	 * 
	 * @param name the skill name
	 * @return true if the skill is registered
	 */
	public boolean contains(String name) {
		return skills.containsKey(name);
	}

	/**
	 * Get the number of registered skills.
	 * 
	 * @return the number of skills
	 */
	public int size() {
		return skills.size();
	}

	/**
	 * Clear all registered skills.
	 */
	public void clear() {
		skills.clear();
		logger.debug("Cleared all skills");
	}

	/**
	 * Unregister a skill by name.
	 * Package-private: Only used internally by SkillsHook.
	 * 
	 * @param name the skill name to unregister
	 * @return true if the skill was removed, false if it didn't exist
	 */
	boolean unregister(String name) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Skill name cannot be null or empty");
		}
		
		SkillMetadata removed = skills.remove(name);
		if (removed != null) {
			logger.info("Unregistered skill: {}", name);
			return true;
		} else {
			logger.debug("Attempted to unregister non-existent skill: {}", name);
			return false;
		}
	}

	/**
	 * Match skills based on user request.
	 * 
	 * Uses simple keyword matching: checks if words from the skill's description
	 * appear in the user request.
	 * 
	 * @param userRequest the user's request text
	 * @return list of matched skills, ordered by relevance
	 */
	public List<SkillMetadata> matchSkills(String userRequest) {
		if (userRequest == null || userRequest.isEmpty()) {
			return List.of();
		}

		String normalizedRequest = userRequest.toLowerCase();
		
		return skills.values().stream()
			.filter(skill -> isSkillRelevant(skill, normalizedRequest))
			.collect(Collectors.toList());
	}

	/**
	 * Check if a skill is relevant to the user request.
	 * 
	 * Simple keyword-based matching: checks if significant words from the
	 * skill's description appear in the user request.
	 */
	private boolean isSkillRelevant(SkillMetadata skill, String normalizedRequest) {
		String description = skill.getDescription().toLowerCase();
		
		// Extract keywords from description (words longer than 3 characters)
		List<String> keywords = Arrays.stream(description.split("\\s+"))
			.filter(word -> word.length() > 3)
			.filter(word -> !isStopWord(word))
			.collect(Collectors.toList());

		// Check if any keyword appears in the request
		return keywords.stream()
			.anyMatch(normalizedRequest::contains);
	}

	/**
	 * Check if a word is a common stop word that should be ignored in matching.
	 */
	private boolean isStopWord(String word) {
		// Common English stop words
		List<String> stopWords = List.of(
			"this", "that", "with", "from", "have", "will", "your", "their",
			"what", "which", "when", "where", "about", "would", "there"
		);
		return stopWords.contains(word);
	}

	/**
	 * Generate a prompt describing all available skills.
	 * This prompt is injected into the LLM's context to enable skill discovery.
	 *
	 * @return the skills list prompt
	 */
	public String generateSkillsListPrompt() {
		if (skills.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("\nAvailable Skills\n\n");
		sb.append("You are equipped with specialized skills for specific tasks. ");
		sb.append("When a user's request aligns with the purpose of any skill below, ");
		sb.append("**automatically invoke that skill** by following its instructions precisely.\n\n");
		sb.append("Do **not** mention the skill name unless asked. Instead, seamlessly apply its logic to deliver the best response.\n");
		sb.append("If the request is ambiguous or lacks necessary details for a skill, **ask clarifying questions first** before proceeding.\n\n");

		for (SkillMetadata skill : skills.values()) {
			sb.append(String.format("- **%s**: %s\n", skill.getName(), skill.getDescription()));
		}

		sb.append("\n");
		return sb.toString();
	}

	/**
	 * Get all required tools from all registered skills.
	 * Analyzes the allowed-tools field of all skills and returns a unique set.
	 * 
	 * @return set of required tool names
	 */
	public java.util.Set<String> getAllRequiredTools() {
		return skills.values().stream()
			.flatMap(skill -> skill.getAllowedTools().stream())
			.map(String::toLowerCase)
			.collect(java.util.stream.Collectors.toSet());
	}
}
