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

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.AbstractSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.SkillScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry.DEFAULT_SYSTEM_PROMPT_TEMPLATE;

/**
 * Classpath-based implementation of SkillRegistry.
 *
 * This implementation loads skills from classpath:resources/skills, supporting both
 * filesystem (development) and JAR (production) environments.
 *
 * <p>Key features:
 * <ul>
 *   <li>Uses Spring's PathMatchingResourcePatternResolver for classpath scanning</li>
 *   <li>Compatible with Spring Boot fat JAR (both 2.x and 3.x nested JAR formats)</li>
 *   <li>Copies resources to a local base path for filesystem-based access</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Use default classpath location (resources/skills)
 * ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder().build();
 *
 * // Custom classpath path
 * ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
 *     .classpathPath("custom/skills")
 *     .build();
 * }</pre>
 */
public class ClasspathSkillRegistry extends AbstractSkillRegistry {

	private static final Logger logger = LoggerFactory.getLogger(ClasspathSkillRegistry.class);
	private final String classpathPath;
	private final Path basePath;
	private final SkillScanner scanner = new SkillScanner();
	private final SystemPromptTemplate systemPromptTemplate;

	private ClasspathSkillRegistry(Builder builder) {
		this.classpathPath = builder.classpathPath != null && !builder.classpathPath.isEmpty()
				? builder.classpathPath
				: "skills";

		// Set basePath - default to /tmp if not specified
		this.basePath = builder.basePath != null && !builder.basePath.isEmpty()
				? Path.of(builder.basePath)
				: Path.of("/tmp");

		// Ensure basePath directory exists
		try {
			Files.createDirectories(basePath);
		}
		catch (IOException e) {
			logger.error("Failed to create basePath directory {}: {}", basePath, e.getMessage());
		}

		// Set system prompt template - use provided or default
		if (builder.systemPromptTemplate != null) {
			this.systemPromptTemplate = builder.systemPromptTemplate;
		}
		else {
			this.systemPromptTemplate = SystemPromptTemplate.builder()
					.template(DEFAULT_SYSTEM_PROMPT_TEMPLATE)
					.build();
		}

		// Load skills during initialization if auto-load is enabled (default: true)
		if (builder.autoLoad) {
			loadSkillsToRegistry();
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Loads skills from classpath into the registry.
	 * Uses Spring Resource Resolver to extract nested JAR contents to a local temp folder.
	 */
	@Override
	protected void loadSkillsToRegistry() {
		Map<String, SkillMetadata> loadedSkills = new HashMap<>();
		Path targetBasePath = basePath.resolve(classpathPath);

		try {
			// 1. Resolve and extract resources using Spring Pattern Resolver
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			String locationPattern = "classpath*:" + classpathPath + "/**/*";
			Resource[] resources = resolver.getResources(locationPattern);

			for (Resource resource : resources) {
				// Only process readable files (skip directories)
				if (resource.isReadable()) {
					String urlStr = resource.getURL().toString();
					// Extract relative path after "/skills/"
					String triggerStr = "/" + classpathPath + "/";
					int idx = urlStr.lastIndexOf(triggerStr);
					if (idx != -1) {
						String relativePath = urlStr.substring(idx + triggerStr.length());
						// Discard URL parameters if any (like in some raw jar URLs)
						if (relativePath.contains("?")) {
							relativePath = relativePath.substring(0, relativePath.indexOf("?"));
						}

						Path targetFile = targetBasePath.resolve(relativePath);
						Files.createDirectories(targetFile.getParent());
						try (InputStream is = resource.getInputStream()) {
							Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
						}
					}
				}
			}

			// 2. Scan the local extracted directory (Safe standard java.nio on local disk)
			if (Files.exists(targetBasePath) && Files.isDirectory(targetBasePath)) {
				try (Stream<Path> stream = Files.list(targetBasePath)) {
					stream.filter(Files::isDirectory).forEach(skillDir -> {
						try {
							SkillMetadata metadata = scanner.loadSkill(skillDir, "classpath");
							if (metadata != null) {
								metadata.setSkillPath(skillDir.toString());
								loadedSkills.put(metadata.getName(), metadata);
								logger.debug("Loaded skill from extracted classpath: {} from {}", metadata.getName(), skillDir);
							}
						}
						catch (Exception e) {
							logger.error("Failed to load skill from newly extracted dir {}: {}", skillDir, e.getMessage());
						}
					});
				}
			} else {
				logger.debug("No classpath skills found matching '{}'", classpathPath);
			}

			logger.info("Loaded {} skills from classpath: {}", loadedSkills.size(), classpathPath);

		}
		catch (Exception e) {
			logger.debug("Failed to process skills from classpath: {}", e.getMessage(), e);
		}

		this.skills = loadedSkills;
	}

	@Override
	public synchronized void reload() {
		logger.info("Reloading skills from classpath...");
		loadSkillsToRegistry();
	}

	@Override
	public String readSkillContent(String name) throws IOException {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Skill name cannot be null or empty");
		}

		Optional<SkillMetadata> skillOpt = get(name);
		if (skillOpt.isEmpty()) {
			throw new IllegalStateException("Skill not found: " + name);
		}

		// Since files are safely extracted to standard filesystem path (/tmp/skills/...) by Spring Resolver,
		// standard loadFullContent() will work perfectly without caching hacks.
		return skillOpt.get().loadFullContent();
	}

	@Override
	public String getSkillLoadInstructions() {
		return "**Skill Location:**\n"
				+ String.format("- **Classpath Skills**: `classpath:%s`\n", classpathPath)
				+ "\n"
				+ "**Skill Path Format:**\n"
				+ "Each skill has a unique id shown in the skill list above. "
				+ "Use the exact id shown when calling `read_skill` to read the SKILL.md file.\n";
	}

	@Override
	public String getRegistryType() {
		return "Classpath";
	}

	@Override
	public SystemPromptTemplate getSystemPromptTemplate() {
		return systemPromptTemplate;
	}

	/**
	 * No unmanaged resources need to be closed anymore.
	 */
	public void close() {
		// Cleaned up, no jarFileSystem to manage
	}

	/**
	 * Builder for creating ClasspathSkillRegistry instances.
	 */
	public static class Builder {
		private String classpathPath;
		private String basePath;
		private boolean autoLoad = true;
		private SystemPromptTemplate systemPromptTemplate;

		public Builder classpathPath(String classpathPath) {
			this.classpathPath = classpathPath;
			return this;
		}

		public Builder basePath(String basePath) {
			this.basePath = basePath;
			return this;
		}

		public Builder autoLoad(boolean autoLoad) {
			this.autoLoad = autoLoad;
			return this;
		}

		public Builder systemPromptTemplate(SystemPromptTemplate systemPromptTemplate) {
			this.systemPromptTemplate = systemPromptTemplate;
			return this;
		}

		public Builder systemPromptTemplate(String template) {
			this.systemPromptTemplate = SystemPromptTemplate.builder()
					.template(template)
					.build();
			return this;
		}

		public ClasspathSkillRegistry build() {
			return new ClasspathSkillRegistry(this);
		}
	}
}
