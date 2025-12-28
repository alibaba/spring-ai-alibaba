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

package com.alibaba.cloud.ai.studio.runtime.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum representing the test status of a tool.
 *
 * @since 1.0.0.3
 */

@Getter
@AllArgsConstructor
public enum ToolTestStatus {

	@JsonProperty("not_test")
	NOT_TEST(1, "not_test"),

	@JsonProperty("passed")
	PASSED(2, "passed"),

	@JsonProperty("failed")
	FAILED(3, "failed"),;

	@EnumValue
	private final Integer status;

	/** String representation of the status */
	private final String value;

}
