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
package com.alibaba.cloud.ai.studio.core.model.llm.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Configuration information for a model
 */
@Data
public class ModelConfigInfo implements Serializable {

	/** Model icon */
	private String icon;

	/** Unique identifier for the model */
	private String modelId;

	/** Model provider */
	private String provider;

	/** Model name */
	private String name;

	/** Model tags */
	private List<String> tags;

	/** Whether the model is enabled */
	private Boolean enable;

	/** Source type: custom or preset */
	private String source;

	/** Model type */
	private String type = ModelTypeEnum.llm.name();

	/** Model mode: chat or completion */
	private String mode;

	/** Model modes */
	public enum ModeEnum {

		chat, completion

	}

	/** Model types */
	public enum ModelTypeEnum {

		llm, rerank, text_embedding, tts, stt

	}

}
