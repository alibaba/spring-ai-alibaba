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
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSkillRegistry implements SkillRegistry {
	private static final Logger logger = LoggerFactory.getLogger(AbstractSkillRegistry.class);

	protected volatile Map<String, SkillMetadata> skills = new HashMap<>();

	protected final Set<String> disabledSkillNames = ConcurrentHashMap.newKeySet();

	/**
	 * Reloads all skills from configured directories.
	 * Clears existing skills and rescans the directories.
	 */
	@Override
	public synchronized void reload() {
		logger.info("Reloading skills...");
		loadSkillsToRegistry();
	}

	@Override
	public Optional<SkillMetadata> get(String name) {
		if (name == null || name.isEmpty() || disabledSkillNames.contains(name)) {
			return Optional.empty();
		}
		return Optional.ofNullable(skills.get(name));
	}

	@Override
	public Optional<SkillMetadata> getByPath(String skillPath) {
		return findByPathInternal(skillPath).filter(skill -> !disabledSkillNames.contains(skill.getName()));
	}

	@Override
	public List<SkillMetadata> listAll() {
		return skills.values()
				.stream()
				.filter(skill -> !disabledSkillNames.contains(skill.getName()))
				.sorted(Comparator.comparing(SkillMetadata::getName))
				.toList();
	}

	@Override
	public boolean contains(String name) {
		return get(name).isPresent();
	}

	@Override
	public List<SkillMetadata> search(String query) {
		if (query == null || query.isBlank()) {
			return listAll();
		}
		String normalized = query.trim().toLowerCase(Locale.ROOT);
		return listAll().stream()
				.map(skill -> Map.entry(skill, searchRank(skill, normalized)))
				.filter(entry -> entry.getValue() < Integer.MAX_VALUE)
				.sorted(Comparator.<Map.Entry<SkillMetadata, Integer>, Integer>comparing(Map.Entry::getValue)
						.thenComparing(entry -> entry.getKey().getName()))
				.map(Map.Entry::getKey)
				.toList();
	}

	@Override
	public int size() {
		return listAll().size();
	}

	@Override
	public boolean disable(String name) {
		if (name == null || name.isBlank() || !skills.containsKey(name)) {
			return false;
		}
		return disabledSkillNames.add(name);
	}

	@Override
	public boolean disableByPath(String skillPath) {
		return findByPathInternal(skillPath)
				.map(SkillMetadata::getName)
				.map(this::disable)
				.orElse(false);
	}

	@Override
	public boolean isDisabled(String name) {
		return name != null && disabledSkillNames.contains(name);
	}

	@Override
	public String readSkillContentByPath(String skillPath) throws IOException {
		if (skillPath == null || skillPath.isBlank()) {
			throw new IllegalArgumentException("Skill path cannot be null or empty");
		}
		SkillMetadata skill = getByPath(skillPath)
				.orElseThrow(() -> new IllegalStateException("Skill not found: " + skillPath));
		return skill.loadFullContent();
	}

	protected abstract void loadSkillsToRegistry();

	protected static String normalizeSkillPath(String skillPath) {
		return Path.of(skillPath).toAbsolutePath().normalize().toString();
	}

	private Optional<SkillMetadata> findByPathInternal(String skillPath) {
		if (skillPath == null || skillPath.isBlank()) {
			return Optional.empty();
		}
		String normalized = normalizeSkillPath(skillPath);
		return skills.values().stream()
				.filter(skill -> normalized.equals(normalizeSkillPath(skill.getSkillPath())))
				.findFirst();
	}

	private int searchRank(SkillMetadata skill, String query) {
		String name = skill.getName().toLowerCase(Locale.ROOT);
		String description = skill.getDescription().toLowerCase(Locale.ROOT);
		String path = skill.getSkillPath().toLowerCase(Locale.ROOT);
		if (name.equals(query)) {
			return 0;
		}
		if (name.startsWith(query)) {
			return 1;
		}
		if (name.contains(query)) {
			return 2;
		}
		if (description.contains(query) || path.contains(query)) {
			return 3;
		}
		return Integer.MAX_VALUE;
	}
}
