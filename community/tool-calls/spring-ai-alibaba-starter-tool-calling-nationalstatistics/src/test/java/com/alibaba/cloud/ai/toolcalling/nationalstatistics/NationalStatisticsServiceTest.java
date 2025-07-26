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

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 国家统计局服务测试类
 *
 * @author makoto
 */
class NationalStatisticsServiceTest {

	@Mock
	private WebClientTool webClientTool;

	@Mock
	private JsonParseTool jsonParseTool;

	private NationalStatisticsProperties properties;

	private NationalStatisticsService nationalStatisticsService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		properties = new NationalStatisticsProperties();
		nationalStatisticsService = new NationalStatisticsService(webClientTool, jsonParseTool, properties);
	}

	@Test
	void testApplyWithValidRequest() {
		// 模拟HTML响应
		String mockHtml = """
				<html>
				<body>
				    <ul class="center_list_contlist">
				        <li>
				            <a href="/tjsj/zxfb/202412/t20241227_1947321.html">2024年1-11月份全国规模以上工业企业利润下降4.7%</a>
				            <span class="date">2024-12-27</span>
				        </li>
				        <li>
				            <a href="/tjsj/zxfb/202412/t20241226_1947280.html">2024年11月份规模以上工业企业利润同比下降7.3%</a>
				            <span class="date">2024-12-26</span>
				        </li>
				    </ul>
				</body>
				</html>
				""";

		when(webClientTool.get(anyString(), any())).thenReturn(Mono.just(mockHtml));

		NationalStatisticsService.Request request = new NationalStatisticsService.Request("zxfb", null, 10);
		NationalStatisticsService.Response response = nationalStatisticsService.apply(request);

		assertNotNull(response);
		assertEquals("success", response.status());
		assertNotNull(response.data());
		assertFalse(response.data().isEmpty());
	}

	@Test
	void testApplyWithNullRequest() {
		NationalStatisticsService.Response response = nationalStatisticsService.apply(null);

		assertNotNull(response);
		assertEquals("error", response.status());
		assertEquals("数据类型不能为空", response.message());
		assertNull(response.data());
	}

	@Test
	void testApplyWithEmptyDataType() {
		NationalStatisticsService.Request request = new NationalStatisticsService.Request("", null, 10);
		NationalStatisticsService.Response response = nationalStatisticsService.apply(request);

		assertNotNull(response);
		assertEquals("error", response.status());
		assertEquals("数据类型不能为空", response.message());
		assertNull(response.data());
	}

	@Test
	void testApplyWithKeywordFilter() {
		String mockHtml = """
				<html>
				<body>
				    <ul class="center_list_contlist">
				        <li>
				            <a href="/tjsj/zxfb/202412/t20241227_1947321.html">2024年GDP增长数据发布</a>
				            <span class="date">2024-12-27</span>
				        </li>
				        <li>
				            <a href="/tjsj/zxfb/202412/t20241226_1947280.html">2024年11月份CPI数据公布</a>
				            <span class="date">2024-12-26</span>
				        </li>
				    </ul>
				</body>
				</html>
				""";

		when(webClientTool.get(anyString(), any())).thenReturn(Mono.just(mockHtml));

		NationalStatisticsService.Request request = new NationalStatisticsService.Request("zxfb", "GDP", 10);
		NationalStatisticsService.Response response = nationalStatisticsService.apply(request);

		assertNotNull(response);
		assertEquals("success", response.status());
		assertNotNull(response.data());
		// 验证过滤效果：只有包含"GDP"的项目被返回
		assertTrue(response.data().stream().allMatch(item -> item.title().contains("GDP")));
	}

	@Test
    void testApplyWithEmptyResponse() {
        when(webClientTool.get(anyString(), any())).thenReturn(Mono.just(""));

        NationalStatisticsService.Request request = new NationalStatisticsService.Request("zxfb", null, 10);
        NationalStatisticsService.Response response = nationalStatisticsService.apply(request);

        assertNotNull(response);
        assertEquals("no_data", response.status());
        assertEquals("未找到相关统计数据", response.message());
        assertNotNull(response.data());
        assertTrue(response.data().isEmpty());
    }

	@Test
	void testRequestDefaultLimit() {
		NationalStatisticsService.Request request = new NationalStatisticsService.Request("zxfb", "GDP", 0);

		// 验证默认limit值被正确设置
		assertEquals(10, request.limit());
	}

	@Test
	void testRequestValidLimit() {
		NationalStatisticsService.Request request = new NationalStatisticsService.Request("zxfb", "GDP", 5);

		assertEquals(5, request.limit());
	}

}
