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
package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

/**
 * Enum defining the source of values in workflow processing.
 */
public enum ValueFromEnum {

	/**
	 * Value is referenced from another source or variable.
	 */
	refer,

	/**
	 * Value is directly input by the user.
	 */
	input,

	/**
	 * Value should be cleared or reset.
	 */
	clear

}
