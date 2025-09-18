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
package com.alibaba.cloud.ai.manus.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader for prompt descriptions from resource files
 */
@Component
public class PromptDescriptionLoader {

	private static final Logger log = LoggerFactory.getLogger(PromptDescriptionLoader.class);

	private final ConcurrentHashMap<String, Properties> descriptionCache = new ConcurrentHashMap<>();

	/**
	 * Load prompt description for a specific language
	 * @param promptName the prompt name
	 * @param language the language code (en, zh)
	 * @return the description or empty string if not found
	 */
	public String loadDescription(String promptName, String language) {
		if (promptName == null || language == null) {
			return "";
		}

		Properties descriptions = getDescriptions(language);
		return descriptions.getProperty(promptName, "");
	}

	/**
	 * Get descriptions for a specific language, with caching
	 * @param language the language code
	 * @return Properties containing descriptions
	 */
	private Properties getDescriptions(String language) {
		return descriptionCache.computeIfAbsent(language, this::loadDescriptionsFromFile);
	}

	/**
	 * Load descriptions from file
	 * @param language the language code
	 * @return Properties containing descriptions
	 */
	private Properties loadDescriptionsFromFile(String language) {
		Properties properties = new Properties();
		String resourcePath = String.format("prompts/%s/descriptions.properties", language);

		try {
			ClassPathResource resource = new ClassPathResource(resourcePath);
			if (resource.exists()) {
				try (InputStream inputStream = resource.getInputStream()) {
					properties.load(inputStream);
					log.debug("Loaded {} descriptions for language: {}", properties.size(), language);
				}
			}
			else {
				log.warn("Description file not found: {}", resourcePath);
			}
		}
		catch (IOException e) {
			log.error("Failed to load descriptions for language: {}", language, e);
		}

		return properties;
	}

	/**
	 * Clear cache for a specific language
	 * @param language the language code
	 */
	public void clearCache(String language) {
		descriptionCache.remove(language);
	}

	/**
	 * Clear all cache
	 */
	public void clearAllCache() {
		descriptionCache.clear();
	}

}
