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

	public static final String CONFIG_PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX
			+ ".nationalstatistics";

	public static final String TOOL_NAME = "getNationalStatisticsService";

	public static final String BASE_URL = "https://www.stats.gov.cn";

	public static final String DATA_API_URL = "https://data.stats.gov.cn";

	public static final String TJSJ_URL = BASE_URL + "/tjsj";

	// 统计数据类型
	public static final String ZXFB = "zxfb"; // 最新发布

	public static final String TJGB = "tjgb"; // 统计公报

	public static final String NDSJ = "ndsj"; // 年度数据

	public static final String YDSJ = "ydsj"; // 月度数据

	public static final String JDSJ = "jdsj"; // 季度数据

}
