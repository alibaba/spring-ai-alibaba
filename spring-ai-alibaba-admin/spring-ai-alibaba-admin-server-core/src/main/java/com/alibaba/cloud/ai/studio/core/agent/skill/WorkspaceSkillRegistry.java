/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.agent.skill;

import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.Skill;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory skill registry scoped to skills attached to an agent.
 *
 * @since 1.0.0.3
 */
public class WorkspaceSkillRegistry {

	private final Map<String, SkillMetadata> byName = new LinkedHashMap<>();

	private final Map<String, SkillMetadata> byPath = new LinkedHashMap<>();

	private final StudioProperties studioProperties;

	public WorkspaceSkillRegistry(StudioProperties studioProperties, List<Skill> skills) {
		this.studioProperties = studioProperties;
		if (skills == null) {
			return;
		}
		for (Skill skill : skills) {
			if (skill == null || StringUtils.isBlank(skill.getSkillName())
					|| StringUtils.isBlank(skill.getStoragePath())) {
				continue;
			}
			Path absolute = resolveAbsolute(skill.getStoragePath());
			SkillMetadata meta = SkillMetadata.builder()
				.skillId(skill.getSkillId())
				.name(skill.getSkillName())
				.description(StringUtils.defaultString(skill.getDescription()))
				.skillPath(absolute.toAbsolutePath().normalize().toString())
				.build();
			byName.put(meta.getName(), meta);
			byPath.put(meta.getSkillPath(), meta);
		}
	}

	public List<SkillMetadata> listAll() {
		return Collections.unmodifiableList(new ArrayList<>(byName.values()));
	}

	public boolean isEmpty() {
		return byName.isEmpty();
	}

	public Optional<SkillMetadata> get(String skillName) {
		if (StringUtils.isBlank(skillName)) {
			return Optional.empty();
		}
		return Optional.ofNullable(byName.get(skillName.trim()));
	}

	public Optional<SkillMetadata> getByPath(String skillPath) {
		if (StringUtils.isBlank(skillPath)) {
			return Optional.empty();
		}
		return Optional.ofNullable(byPath.get(Path.of(skillPath).toAbsolutePath().normalize().toString()));
	}

	public String readSkillContent(String skillName) throws IOException {
		SkillMetadata meta = get(skillName)
			.orElseThrow(() -> new IllegalStateException("Skill not found: " + skillName));
		return readBody(meta);
	}

	public String readSkillContentByPath(String skillPath) throws IOException {
		SkillMetadata meta = getByPath(skillPath)
			.orElseThrow(() -> new IllegalStateException("Skill not found: " + skillPath));
		return readBody(meta);
	}

	public String readSkillResource(String skillName, String relativePath) throws IOException {
		SkillMetadata meta = get(skillName)
			.orElseThrow(() -> new IllegalStateException("Skill not found: " + skillName));
		if (StringUtils.isBlank(relativePath)) {
			throw new IllegalArgumentException("relative_path is required");
		}
		if (relativePath.contains("..")) {
			throw new IllegalArgumentException("relative_path must not contain '..'");
		}

		Path skillRoot = Path.of(meta.getSkillPath()).toAbsolutePath().normalize();
		Path target = skillRoot.resolve(relativePath).normalize();
		if (!target.startsWith(skillRoot)) {
			throw new IllegalArgumentException("relative_path escapes skill directory");
		}
		if (!Files.isRegularFile(target)) {
			throw new IllegalStateException("Resource not found: " + relativePath);
		}
		return Files.readString(target, StandardCharsets.UTF_8);
	}

	private String readBody(SkillMetadata meta) throws IOException {
		Path skillMd = Path.of(meta.getSkillPath()).resolve(SkillPackageUtils.SKILL_MD);
		if (!Files.isRegularFile(skillMd)) {
			throw new IllegalStateException("SKILL.md not found for skill: " + meta.getName());
		}
		SkillMdParser.ParsedSkillMd parsed = SkillMdParser.parse(Files.readString(skillMd, StandardCharsets.UTF_8));
		return parsed.getBody();
	}

	private Path resolveAbsolute(String relativeStoragePath) {
		return Path.of(studioProperties.getStoragePath(), relativeStoragePath).toAbsolutePath().normalize();
	}

}
