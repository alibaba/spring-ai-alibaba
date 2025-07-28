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
package com.alibaba.cloud.ai.toolcalling.nationalstatistics;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;

/**
 * 国家统计局工具常量定义
 *
 * @author Makoto
 */
public final class NationalStatisticsConstants {

	/**
	 * 配置前缀
	 */
	public static final String CONFIG_PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX + ".nationalstatistics";

	/**
	 * 工具名称
	 */
	public static final String TOOL_NAME = "nationalStatistics";

	/**
	 * 国家统计局API基础URL（使用HTTP避免SSL证书问题）
	 */
	public static final String BASE_URL = "http://data.stats.gov.cn";

}