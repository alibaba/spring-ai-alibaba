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
package com.alibaba.cloud.ai.example.manus.config.entity;

public enum ConfigInputType {

	/**
	 * 文本输入框
	 */
	TEXT,

	/**
	 * 下拉选择框
	 */
	SELECT,

	/**
	 * 多选框
	 */
	CHECKBOX,

	/**
	 * 单选框（是/否）
	 */
	BOOLEAN,

	/**
	 * 数字输入框
	 */
	NUMBER

}
