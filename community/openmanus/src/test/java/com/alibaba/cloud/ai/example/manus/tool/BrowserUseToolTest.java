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
package com.alibaba.cloud.ai.example.manus.tool;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.tool.support.ToolExecuteResult;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * BrowserUseTool 的 JUnit 测试类 实现一个简单的 Hello World 测试场景
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserUseToolTest {

	private static final Logger log = LoggerFactory.getLogger(BrowserUseToolTest.class);

	private MockChromeDriverService mockService;

	private BrowserUseTool browserUseTool;

	@BeforeAll
	void setUp() {
		// 初始化测试环境
		mockService = new MockChromeDriverService();
		browserUseTool = new BrowserUseTool(mockService);
		browserUseTool.setAgent(createMockAgent());
	}

	@AfterAll
	void tearDown() {
		// 清理资源
		if (mockService != null) {
			mockService.close();
		}
	}

	@Test
	@DisplayName("测试浏览器搜索'Hello World'的基本功能")
	void testHelloWorldSearch() {
		try {
			// 步骤1: 导航到百度
			log.info("步骤1: 导航到百度");
			ToolExecuteResult navigateResult = executeAction("navigate", "https://www.baidu.com");
			Assertions.assertTrue(navigateResult.getResult().contains("Navigated to"), "导航到百度失败");

			// 步骤2: 在搜索框中输入 "Hello World"
			log.info("步骤2: 在搜索框中输入 'Hello World'");
			ToolExecuteResult inputResult = executeAction("input_text", null, 0, "Hello World");
			Assertions.assertTrue(inputResult.getResult().contains("Successfully"), "输入文本失败");

			// 步骤3: 点击搜索按钮
			log.info("步骤3: 点击搜索按钮");
			ToolExecuteResult clickResult = executeAction("click", null, 1, null);
			Assertions.assertTrue(clickResult.getResult().contains("Clicked"), "点击搜索按钮失败");

			// 步骤4: 等待并获取搜索结果
			Thread.sleep(2000); // 等待页面加载
			ToolExecuteResult textResult = executeAction("get_text", null);
			String searchResults = textResult.getResult();

			// 验证搜索结果包含 "Hello World"
			Assertions.assertTrue(searchResults.contains("Hello World"), "搜索结果中未找到 'Hello World'");

			log.info("测试成功完成！");

		}
		catch (Exception e) {
			log.error("测试过程中发生错误", e);
			Assertions.fail("测试执行失败: " + e.getMessage());
		}
	}

	@Test
	@DisplayName("测试获取浏览器当前状态")
	void testGetBrowserState() {
		String currentState = browserUseTool.getCurrentToolStateString();
		Assertions.assertNotNull(currentState, "获取浏览器状态失败");
		Assertions.assertTrue(currentState.contains("Current URL"), "浏览器状态信息不完整");
	}

	// 创建模拟的BaseAgent
	private BaseAgent createMockAgent() {
		return new BaseAgent() {
			@Override
			public String getPlanId() {
				return "test-plan-id";
			}

			@Override
			public String getName() {
				return "MockAgent";
			}

			@Override
			public String getDescription() {
				return "MockAgent for testing";
			}
		};
	}

	// 辅助方法：执行浏览器操作
	private ToolExecuteResult executeAction(String action, String url) {
		return executeAction(action, url, null, null);
	}

	// 辅助方法：执行浏览器操作（带索引和文本）
	private ToolExecuteResult executeAction(String action, String url, Integer index, String text) {

		Map<String, Object> params = new HashMap<>();
		params.put("action", action);

		if (url != null) {
			params.put("url", url);
		}

		if (index != null) {
			params.put("index", index);
		}

		if (text != null) {
			params.put("text", text);
		}

		String toolInput = JSON.toJSONString(params);
		return browserUseTool.run(toolInput);
	}

}
