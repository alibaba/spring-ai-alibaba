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
package com.alibaba.cloud.ai.graph.agent.tools.task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads agent specs from Markdown files with YAML front matter.
 * <p>
 * File format (compatible with spring-ai-agent-utils):
 *
 * <pre>
 * ---
 * name: Explore
 * description: Fast agent for exploring codebases...
 * tools: Read, Grep, Glob   # optional, comma-separated
 * model: sonnet            # optional
 * ---
 *
 * # System prompt (markdown body)
 * You are a file search specialist...
 * </pre>
 *
 * @author Spring AI Alibaba
 */
public final class AgentSpecLoader {

	private static final Logger logger = LoggerFactory.getLogger(AgentSpecLoader.class);

	private static final Yaml YAML = new Yaml();

	private AgentSpecLoader() {
	}

	/**
	 * Load agent specs from a directory (recursively scans for .md files).
	 * @param directoryPath path to directory containing agent spec files
	 * @return list of parsed specs
	 */
	public static List<AgentSpec> loadFromDirectory(String directoryPath) throws IOException {
		if (!StringUtils.hasText(directoryPath)) {
			return List.of();
		}
		return loadFromDirectory(Paths.get(directoryPath));
	}

	/**
	 * Load agent specs from a directory (recursively scans for .md files).
	 */
	public static List<AgentSpec> loadFromDirectory(Path rootPath) throws IOException {
		if (rootPath == null || !Files.exists(rootPath)) {
			logger.warn("Agent spec directory does not exist: {}", rootPath);
			return List.of();
		}
		if (!Files.isDirectory(rootPath)) {
			throw new IOException("Path is not a directory: " + rootPath);
		}

		List<AgentSpec> specs = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(rootPath)) {
			paths.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith(".md"))
					.forEach(path -> {
						try {
							AgentSpec spec = loadFromFile(path);
							if (spec != null) {
								specs.add(spec);
								logger.debug("Loaded agent spec: {} from {}", spec.name(), path);
							}
						}
						catch (Exception e) {
							logger.warn("Failed to load agent spec from {}: {}", path, e.getMessage());
						}
					});
		}
		return specs;
	}

	/**
	 * Load a single agent spec from a file.
	 */
	public static AgentSpec loadFromFile(Path filePath) throws IOException {
		String content = Files.readString(filePath, StandardCharsets.UTF_8);
		return parse(content);
	}

	/**
	 * Load agent spec from a Spring Resource.
	 */
	public static AgentSpec loadFromResource(Resource resource) throws IOException {
		String content = resource.getContentAsString(StandardCharsets.UTF_8);
		return parse(content);
	}

	/**
	 * Parse markdown content with YAML front matter into AgentSpec.
	 */
	public static AgentSpec parse(String markdown) {
		if (!StringUtils.hasText(markdown)) {
			return null;
		}
		if (!markdown.startsWith("---")) {
			logger.warn("Agent spec must start with YAML front matter (---)");
			return null;
		}

		int endIndex = markdown.indexOf("---", 3);
		if (endIndex == -1) {
			logger.warn("Agent spec front matter not properly closed with ---");
			return null;
		}

		String frontMatterStr = markdown.substring(3, endIndex).trim();
		String content = markdown.substring(endIndex + 3).trim();

		Map<String, Object> frontMatter;
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> parsed = YAML.load(frontMatterStr);
			frontMatter = parsed;
		}
		catch (Exception e) {
			logger.warn("Failed to parse YAML front matter: {}", e.getMessage());
			return null;
		}

		if (CollectionUtils.isEmpty(frontMatter)) {
			return null;
		}

		String name = getString(frontMatter, "name");
		String description = getString(frontMatter, "description");

		if (!StringUtils.hasText(name)) {
			logger.warn("Agent spec missing required 'name' in front matter");
			return null;
		}
		if (!StringUtils.hasText(description)) {
			logger.warn("Agent spec missing required 'description' in front matter");
			return null;
		}

		List<String> toolNames = parseToolNames(getString(frontMatter, "tools"));
		String model = getString(frontMatter, "model");

		return new AgentSpec(name, description, content, toolNames, model);
	}

	private static String getString(Map<String, Object> map, String key) {
		Object v = map.get(key);
		return v != null ? v.toString().trim() : null;
	}

	private static List<String> parseToolNames(String toolsStr) {
		if (!StringUtils.hasText(toolsStr)) {
			return List.of();
		}
		return Stream.of(toolsStr.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.toList();
	}
}
