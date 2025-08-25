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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;

import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.connector.accessor.Accessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 基于AI服务的时间处理器测试类.
 * 
 * @author huangzhen
 */
@ExtendWith(MockitoExtension.class)
public class AiTimeProcessorTest {

	@Mock
	private BaseVectorStoreService vectorStoreService;

	@Mock
	private BaseSchemaService schemaService;

	@Mock
	private LlmService llmService;

	@Mock
	private Accessor dbAccessor;

	@Mock
	private DbConfig dbConfig;

	private BaseNl2SqlService baseNl2SqlService;

	@BeforeEach
	void setUp() {
		// 创建服务实例
		baseNl2SqlService = new BaseNl2SqlService(vectorStoreService, schemaService, llmService, dbAccessor, dbConfig);
	}

	@Test
	void testProcessTimeExpressions_Today() {
		String query = "查询今天的销售数据";
		String mockResponse = "查询2025-8-23日的销售数据";

		when(llmService.call(anyString())).thenReturn(mockResponse);

		String result = baseNl2SqlService.processTimeExpressions(query);
		assertEquals(mockResponse, result);
	}

	@Test
	void testProcessTimeExpressions_Yesterday() {
		String query = "统计昨天的订单数量";
		String mockResponse = "统计2025-8-22日的订单数量";

		when(llmService.call(anyString())).thenReturn(mockResponse);

		String result = baseNl2SqlService.processTimeExpressions(query);
		assertEquals(mockResponse, result);
	}

	@Test
	void testProcessTimeExpressions_LastMonth() {
		String query = "分析上个月的用户增长";
		String mockResponse = "分析2025-07-01至2025-07-31的用户增长";

		when(llmService.call(anyString())).thenReturn(mockResponse);

		String result = baseNl2SqlService.processTimeExpressions(query);
		assertEquals(mockResponse, result);
	}

	@Test
	void testProcessTimeExpressions_WithPrefix() {
		String query = "显示今年的营收报告";
		String mockResponse = "显示2025年的营收报告";

		when(llmService.call(anyString())).thenReturn(mockResponse);

		String result = baseNl2SqlService.processTimeExpressions(query);
		assertEquals("显示2025年的营收报告", result);
	}

	@Test
	void testProcessTimeExpressions_WithQuotes() {
		String query = "查看近7天的访问量";
		String mockResponse = "查看2025-8-17日至2025-8-23日的访问量";

		when(llmService.call(anyString())).thenReturn(mockResponse);

		String result = baseNl2SqlService.processTimeExpressions(query);
		assertEquals("查看2025-8-17日至2025-8-23日的访问量", result);
	}

	@Test
	void testProcessTimeExpressions_NoTimeExpression() {
		String query = "查询用户信息";
		String mockResponse = "查询用户信息";

		when(llmService.call(anyString())).thenReturn(mockResponse);

		String result = baseNl2SqlService.processTimeExpressions(query);
		assertEquals(query, result);
	}

	@Test
	void testProcessTimeExpressions_ExceptionHandling() {
		String query = "测试查询";

		when(llmService.call(anyString())).thenThrow(new RuntimeException("AI服务异常"));

		String result = baseNl2SqlService.processTimeExpressions(query);
		assertEquals(query, result);
	}

}
