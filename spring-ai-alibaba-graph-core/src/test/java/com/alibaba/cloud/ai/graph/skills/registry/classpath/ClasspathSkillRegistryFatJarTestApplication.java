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
	}

}
