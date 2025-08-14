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
package com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 模型配置信息
 *
 * @since 1.0.0.3
 */
@Data
public class ModelConfig implements Serializable {

	/**
	 * 模型ID，对应model下的name
	 */
	@JsonProperty("model_id")
	private String modelId;

	/**
	 * 模型名称
	 */
	@JsonProperty("model_name")
	private String modelName;

	/**
	 * 模型提供商
	 */
	private String provider;

	/**
	 * 模型参数
	 */
	private List<ModelParam> params;

	/**
	 * 模型模式：chat或completion
	 */
	private String mode;

	/**
	 * 视觉参数列表
	 */
	@JsonProperty("vision_config")
	private SkillConfig visionConfig;

	@Data
	public static class SkillConfig implements Serializable {

		@JsonProperty("enable")
		private Boolean enable;

		private List<Node.InputParam> params;

	}

	/**
	 * 模型参数配置
	 */
	@Data
	public static class ModelParam implements Serializable {

		/**
		 * 参数键名
		 */
		private String key;

		/**
		 * 参数类型
		 */
		private String type;

		/**
		 * 默认值
		 */
		@JsonProperty("default_value")
		private Object defaultValue;

		/**
		 * 参数值
		 */
		private Object value;

		/**
		 * 参数开关
		 */
		@JsonProperty("enable")
		private Boolean enable;

	}

}
