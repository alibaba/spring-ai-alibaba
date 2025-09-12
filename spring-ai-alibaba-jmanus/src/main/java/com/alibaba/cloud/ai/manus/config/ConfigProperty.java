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

package com.alibaba.cloud.ai.manus.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.alibaba.cloud.ai.manus.config.entity.ConfigInputType;

/**
 * Configuration property annotation, supporting three-level configuration structure:
 * group.subgroup.key
 * <p>
 * Configuration hierarchy:
 * <ul>
 * <li>group: Top-level group, such as browser, network, security, etc.</li>
 * <li>subGroup: Secondary group, such as browser.settings, browser.proxy, etc.</li>
 * <li>key: Specific configuration item</li>
 * </ul>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigProperty {

	/**
	 * Top-level group
	 */
	String group();

	/**
	 * Secondary group
	 */
	String subGroup();

	/**
	 * Configuration item key
	 */
	String key();

	/**
	 * Configuration item YAML full path, used to find specific configuration items as
	 * default values in the database
	 */
	String path();

	/**
	 * Configuration item description
	 * <p>
	 * Supports internationalization key format: config.desc.{group}.{subGroup}.{key}
	 */
	String description() default "";

	/**
	 * Configuration item default value
	 */
	String defaultValue() default "";

	/**
	 * Configuration item input type
	 * <p>
	 * Default is text input box
	 */
	ConfigInputType inputType() default ConfigInputType.TEXT;

	/**
	 * Dropdown box options
	 * <p>
	 * Only effective when inputType = SELECT
	 */
	ConfigOption[] options() default {};

}
