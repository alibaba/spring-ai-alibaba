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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSkillRegistry implements SkillRegistry {
	private static final Logger logger = LoggerFactory.getLogger(AbstractSkillRegistry.class);

	protected volatile Map<String, SkillMetadata> skills = new HashMap<>();

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
		return Optional.ofNullable(skills.get(name));
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

	protected abstract void loadSkillsToRegistry();
}
