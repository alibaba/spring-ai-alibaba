/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.format;

import io.spring.initializr.generator.project.contributor.ProjectContributor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

/**
 * Copy the file `classpath:/templates/default-application.yml` to the
 * `src/main/resources/default-application.yml` directory in the generated project.
 */
public class ApplicationYamlContributor implements ProjectContributor {

	/**
	 * Corresponding src/main/resources/templates/default-application.yml
	 */
	private static final String RESOURCE_PATH = "/templates/default-application.yml";

	@Override
	public void contribute(Path projectRoot) throws IOException {
		Path resourcesDir = projectRoot.resolve("src/main/resources");
		Files.createDirectories(resourcesDir);
		Path target = resourcesDir.resolve("application.yml");
		try (InputStream in = getClass().getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new IllegalStateException("Resource not found: " + RESOURCE_PATH);
			}
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

}
