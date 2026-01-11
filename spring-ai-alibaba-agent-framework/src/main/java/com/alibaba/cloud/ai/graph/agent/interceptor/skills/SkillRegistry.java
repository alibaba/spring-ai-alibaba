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
package com.alibaba.cloud.ai.graph.agent.interceptor.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SkillRegistry {

	private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);

	private final Map<String, SkillMetadata> skills = new ConcurrentHashMap<>();

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

	public void registerAll(List<SkillMetadata> skillList) {
		if (skillList == null) {
			return;
		}
		skillList.forEach(this::register);
	}

	public Optional<SkillMetadata> get(String name) {
		return Optional.ofNullable(skills.get(name));
	}

	public List<SkillMetadata> listAll() {
		return new ArrayList<>(skills.values());
	}

	public boolean contains(String name) {
		return skills.containsKey(name);
	}

	public int size() {
		return skills.size();
	}

	public void clear() {
		skills.clear();
		logger.debug("Cleared all skills");
	}

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
}
