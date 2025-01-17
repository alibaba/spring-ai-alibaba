/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.dotprompt;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of DotPromptLoader that loads prompts from the classpath.
 */
public class ClassPathDotPromptLoader implements DotPromptLoader {

	private final PathMatchingResourcePatternResolver resourceResolver;

	private final Map<String, DotPrompt> promptCache;

	private final DotPromptProperties properties;

	private final Yaml yaml;

	private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("^---\\s*$(.+?)^---\\s*$(.+)$",
			Pattern.MULTILINE | Pattern.DOTALL);

	public ClassPathDotPromptLoader(DotPromptProperties properties) {
		Assert.notNull(properties, "properties must not be null");
		this.resourceResolver = new PathMatchingResourcePatternResolver();
		this.promptCache = new HashMap<>();
		this.properties = properties;
		this.yaml = new Yaml();
	}

	@Override
	public List<String> getPromptNames() throws IOException {
		List<String> promptNames = new ArrayList<>();
		String promptLocation = properties.getBasePath();

		Assert.hasText(promptLocation, "Prompt location must not be empty");

		// Ensure the location ends with /
		if (!promptLocation.endsWith("/")) {
			promptLocation += "/";
		}

		// Search for all .prompt files in the configured location
		String locationPattern = "classpath*:" + promptLocation + "**/*" + properties.getFileExtension();
		Resource[] resources = resourceResolver.getResources(locationPattern);

		for (Resource resource : resources) {
			String filename = resource.getFilename();
			if (filename != null) {
				// Remove the file extension
				String promptName = StringUtils.stripFilenameExtension(filename);
				promptNames.add(promptName);
			}
		}

		return promptNames;
	}

	@Override
	public DotPrompt load(String promptName) throws IOException {
		Assert.hasText(promptName, "promptName must not be empty");

		// Check cache first
		DotPrompt cachedPrompt = promptCache.get(promptName);
		if (cachedPrompt != null) {
			return cachedPrompt;
		}

		String promptLocation = properties.getBasePath();
		if (!promptLocation.endsWith("/")) {
			promptLocation += "/";
		}

		String resourcePath = promptLocation + promptName + properties.getFileExtension();
		Resource resource = resourceResolver.getResource("classpath:" + resourcePath);

		if (!resource.exists()) {
			throw new IOException("Prompt file not found: " + resourcePath);
		}

		try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			StringBuilder content = new StringBuilder();
			char[] buffer = new char[1024];
			int read;
			while ((read = reader.read(buffer)) != -1) {
				content.append(buffer, 0, read);
			}

			Matcher matcher = FRONT_MATTER_PATTERN.matcher(content.toString());
			if (!matcher.find()) {
				throw new IOException("Invalid prompt file format. Expected front matter and template sections.");
			}

			String frontMatter = matcher.group(1).trim();
			String template = matcher.group(2).trim();

			Map<String, Object> metadata = yaml.load(frontMatter);

			DotPrompt prompt = new DotPrompt();
			prompt.setModel((String) metadata.get("model"));
			prompt.setConfig((Map<String, Object>) metadata.get("config"));
			prompt.setInput(parseInputSchema((Map<String, Object>) metadata.get("input")));
			prompt.setTemplate(template);

			// Cache the prompt
			promptCache.put(promptName, prompt);

			return prompt;
		}
	}

	private DotPrompt.InputSchema parseInputSchema(Map<String, Object> input) {
		if (input == null) {
			return null;
		}

		DotPrompt.InputSchema schema = new DotPrompt.InputSchema();
		schema.setSchema((Map<String, String>) input.get("schema"));
		schema.setDefaultValues((Map<String, Object>) input.get("default"));
		return schema;
	}

}
