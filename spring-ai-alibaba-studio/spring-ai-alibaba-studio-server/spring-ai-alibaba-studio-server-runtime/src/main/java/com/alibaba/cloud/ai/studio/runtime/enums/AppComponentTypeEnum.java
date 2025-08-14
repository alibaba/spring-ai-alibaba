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

import lombok.Getter;

/**
 * Enum representing different types of application components.
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@Getter
public enum AppComponentTypeEnum {

	/** Basic agent component */
	Agent(1, "basic"),

	/** Workflow component */
	Workflow(2, "workflow");

	/** Component type code */
	private final Integer code;

	/** Component type value */
	private final String value;

	AppComponentTypeEnum(Integer type, String value) {
		this.code = type;
		this.value = value;
	}

}
