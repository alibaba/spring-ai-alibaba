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
package com.alibaba.cloud.ai.graph.skills.registry.classpath;

import java.nio.file.Files;
import java.nio.file.Path;

/** Test application launched from a repackaged Spring Boot executable JAR. */
public final class ClasspathSkillRegistryFatJarTestApplication {

	private ClasspathSkillRegistryFatJarTestApplication() {
	}

	public static void main(String[] args) throws Exception {
		ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
				.classpathPath("fat-jar-skills")
				.basePath(args[0])
				.build();
		System.out.println("FAT_JAR_SKILLS=" + registry.listAll().stream().map(skill -> skill.getName()).toList());
		if (!registry.contains("fat-jar-skill")) {
			throw new IllegalStateException("Classpath skill was not loaded from the executable JAR");
		}
		Path skillPath = Path.of(registry.get("fat-jar-skill").orElseThrow().getSkillPath());
		if (!Files.exists(skillPath.resolve("references/reference.md"))) {
			throw new IllegalStateException("Nested skill resource was not extracted from the executable JAR");
		}
		if (!Files.exists(skillPath.resolve("references/fat-jar-skills/note.md"))) {
			throw new IllegalStateException("A repeated classpath root was not retained in the resource path");
		}
		if (Files.exists(skillPath.resolve("references/dependency-only.md"))) {
			throw new IllegalStateException("Resources from duplicate dependency skills were merged");
		}
		if (Files.exists(skillPath.resolve("references/obsolete.md"))) {
			throw new IllegalStateException("An obsolete extracted resource remained exposed");
		}
		Path fallbackSkillPath = Path.of(registry.get("fallback-skill").orElseThrow().getSkillPath());
		if (!Files.exists(fallbackSkillPath.resolve("references/dependency-reference.md"))
				|| Files.exists(fallbackSkillPath.resolve("references/app-only.md"))) {
			throw new IllegalStateException("An incomplete higher-precedence source shadowed a complete skill");
		}

		registry.reload();
		Path reloadedSkillPath = Path.of(registry.get("fat-jar-skill").orElseThrow().getSkillPath());
		if (!skillPath.equals(reloadedSkillPath)) {
			throw new IllegalStateException("Identical content was not reused across reloads");
		}
		if (!Files.exists(skillPath.resolve("references/reference.md"))
				|| !Files.exists(reloadedSkillPath.resolve("references/reference.md"))) {
			throw new IllegalStateException("Reload removed a published skill path or exposed an incomplete one");
		}
		Files.writeString(skillPath.resolve("references/reference.md"), "tampered");
		registry.reload();
		Path repairedSkillPath = Path.of(registry.get("fat-jar-skill").orElseThrow().getSkillPath());
		if (skillPath.equals(repairedSkillPath)
				|| !"# Reference".equals(Files.readString(repairedSkillPath.resolve("references/reference.md")))) {
			throw new IllegalStateException("Reload reused a modified content-addressed extraction");
		}
		registry.reload();
		Path latestSkillPath = Path.of(registry.get("fat-jar-skill").orElseThrow().getSkillPath());
		if (!repairedSkillPath.equals(latestSkillPath) || !Files.exists(latestSkillPath)) {
			throw new IllegalStateException("Repeated reloads did not reuse the repaired extraction");
		}
		try (var generations = Files.list(skillPath.getParent().getParent())) {
			if (generations.filter(path -> path.getFileName().toString().startsWith(".extracted-")).count() != 2) {
				throw new IllegalStateException("Reload did not retain the modified and repaired generations");
			}
		}
		registry.close();
		if (Files.exists(skillPath) || Files.exists(repairedSkillPath)) {
			throw new IllegalStateException("Closing the registry did not clean its extraction generations");
		}
		System.out.println("FAT_JAR_CONTENT_ADDRESSED=true");
	}

}
