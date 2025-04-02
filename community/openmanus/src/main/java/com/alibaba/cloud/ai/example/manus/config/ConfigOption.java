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
package com.alibaba.cloud.ai.example.manus.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置选项注解
 * <p>
 * 用于定义下拉框、单选框等的选项
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)

public @interface ConfigOption {

	/**
	 * 选项值
	 */
	String value();

	/**
	 * 选项标签
	 * <p>
	 * 支持国际化key格式：config.option.{group}.{subGroup}.{key}.{value}
	 */
	String label() default "";

	/**
	 * 选项描述
	 * <p>
	 * 支持国际化key格式：config.option.desc.{group}.{subGroup}.{key}.{value}
	 */
	String description() default "";

	/**
	 * 选项图标（可选）
	 */
	String icon() default "";

	/**
	 * 选项是否禁用
	 */
	boolean disabled() default false;

}
