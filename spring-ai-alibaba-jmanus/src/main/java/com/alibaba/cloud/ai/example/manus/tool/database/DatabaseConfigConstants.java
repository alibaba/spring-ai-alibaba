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

package com.alibaba.cloud.ai.example.manus.tool.database;

/**
 * 数据库配置常量
 */
public class DatabaseConfigConstants {

	// 配置前缀
	public static final String CONFIG_PREFIX = "database_use.datasource.";

	// 配置属性名
	public static final String PROP_TYPE = "type";

	public static final String PROP_ENABLE = "enable";

	public static final String PROP_URL = "url";

	public static final String PROP_DRIVER_CLASS_NAME = "driver-class-name";

	public static final String PROP_USERNAME = "username";

	public static final String PROP_PASSWORD = "password";

	// 配置值
	public static final String ENABLE_TRUE = "true";

	public static final String ENABLE_FALSE = "false";

	private DatabaseConfigConstants() {
		// 工具类，禁止实例化
	}

}
