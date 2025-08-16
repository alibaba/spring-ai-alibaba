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

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.CommonParam;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 异常处理配置
 *
 * @since 1.0.0.3
 */
@Data
public class TryCatchConfig implements Serializable {

	/**
	 * @see StrategyEnum
	 */
	@JsonProperty("strategy")
	private String strategy;

	@JsonProperty("default_values")
	private List<CommonParam> defaultValues;

	public enum StrategyEnum {

		noop, defaultValue, failBranch

	}

}
