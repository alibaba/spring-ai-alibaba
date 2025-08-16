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

package com.alibaba.cloud.ai.studio.core.model.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Utility class for managing embedding model dimensions. Provides methods to retrieve
 * dimension information for different embedding models.
 *
 * @since 1.0.0.3
 */
public class EmbeddingModelDimension {

	/** Resource file containing known embedding model dimensions */
	private static final Resource EMBEDDING_MODEL_DIMENSIONS_PROPERTIES = new ClassPathResource(
			"/embedding/embedding-model-dimensions.properties");

	/** Cache of known embedding model dimensions loaded from properties file */
	private static final Map<String, Integer> KNOWN_EMBEDDING_DIMENSIONS = loadKnownModelDimensions();

	/**
	 * Loads embedding model dimensions from the properties file.
	 * @return Map of model names to their dimensions
	 */
	private static Map<String, Integer> loadKnownModelDimensions() {
		try {
			var resource = EMBEDDING_MODEL_DIMENSIONS_PROPERTIES;
			Assert.notNull(resource, "the embedding dimensions must be non-null");
			Assert.state(resource.exists(), "the embedding dimensions properties file must exist");
			var properties = new Properties();
			try (var in = resource.getInputStream()) {
				properties.load(in);
			}
			return properties.entrySet()
				.stream()
				.collect(Collectors.toMap(e -> e.getKey().toString(), e -> Integer.parseInt(e.getValue().toString())));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the dimension for a specific model name, falling back to default if not found.
	 * @param modelName Name of the embedding model
	 * @param defaultDimension Default dimension to return if model not found
	 * @return The dimension of the model or default value
	 */
	public static Integer getDimension(String modelName, int defaultDimension) {
		var dimension = KNOWN_EMBEDDING_DIMENSIONS.get(modelName);
		if (dimension == null) {
			return defaultDimension;
		}

		return dimension;
	}

	/**
	 * Gets the dimension for a specific model name, falling back to model's default if
	 * not found.
	 * @param modelName Name of the embedding model
	 * @param embeddingModel The embedding model instance
	 * @return The dimension of the model or model's default dimension
	 */
	public static Integer getDimension(String modelName, EmbeddingModel embeddingModel) {
		var dimension = KNOWN_EMBEDDING_DIMENSIONS.get(modelName);
		if (dimension == null) {
			return embeddingModel.dimensions();
		}

		return dimension;
	}

}
