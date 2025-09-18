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
package com.alibaba.cloud.ai.manus.model.model.enums;

/**
 * @author lizhenning
 * @since 2025/7/8
 */
public enum ModelType {

	/**
	 * General model: Has multiple capabilities and can handle various tasks (e.g., text
	 * generation, reasoning, code)
	 */
	GENERAL,

	/**
	 * Reasoning model: Used for logical reasoning and decision-making scenarios
	 */
	REASONING,

	/**
	 * Planner model: Used for task decomposition and plan formulation
	 */
	PLANNER,

	/**
	 * Vision model: Used for visual tasks such as image recognition, OCR, and object
	 * detection
	 */
	VISION,

	/**
	 * Code model: Used for code generation, understanding, and repair
	 */
	CODE,

	/**
	 * Text generation model: Used for natural language text generation (e.g., dialogue,
	 * article generation)
	 */
	TEXT_GENERATION,

	/**
	 * Embedding model: Used for text vectorization and semantic encoding
	 */
	EMBEDDING,

	/**
	 * Classification model: Used for text classification and sentiment analysis
	 */
	CLASSIFICATION,

	/**
	 * Summarization model: Used for generating summaries of long texts
	 */
	SUMMARIZATION,

	/**
	 * Multimodal model: Processes multiple modalities such as text and images together
	 */
	MULTIMODAL,

	/**
	 * Speech model: Used for speech recognition and synthesis
	 */
	SPEECH,

	/**
	 * Translation model: Used for cross-language translation
	 */
	TRANSLATION;

}
