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

import com.alibaba.cloud.ai.example.manus.OpenManusSpringBootApplication;
import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.agent.ToolCallAgent;
import com.alibaba.cloud.ai.example.manus.service.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.support.ToolExecuteResult;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

/**
 * BrowserUseTool的Spring集成测试类
 * 使用真实的Spring上下文来测试BrowserUseTool的功能
 */
@SpringBootTest(classes = OpenManusSpringBootApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BrowserUseToolSpringTest {

    private static final Logger log = LoggerFactory.getLogger(BrowserUseToolSpringTest.class);

    @Autowired
    private ChromeDriverService chromeDriverService;

    private BrowserUseTool browserUseTool;

    @BeforeEach
    void setUp() {
        browserUseTool = new BrowserUseTool(chromeDriverService);
        browserUseTool.setAgent(null);
    }

    @Test
    @Order(1)
    @DisplayName("测试浏览器搜索'Hello World'")
    void testHelloWorldSearch() {
        try {
            // 步骤1: 导航到百度
            log.info("步骤1: 导航到百度");
            ToolExecuteResult navigateResult = executeAction("navigate", "https://www.baidu.com");
            Assertions.assertEquals(
                "Navigated to https://www.baidu.com",
                navigateResult.getOutput(),
                "导航到百度失败"
            );

            // 步骤2: 在搜索框中输入 "Hello World"
            log.info("步骤2: 在搜索框中输入 'Hello World'");
            ToolExecuteResult inputResult = executeAction("input_text", null, 0, "Hello World");
            Assertions.assertEquals(
                "Successfully input 'Hello World' into element at index 0",
                inputResult.getOutput(),
                "在搜索框输入文本失败"
            );

            // 步骤3: 点击搜索按钮
            log.info("步骤3: 点击搜索按钮");
            ToolExecuteResult clickResult = executeAction("click", null, 1, null);
            Assertions.assertTrue(
                clickResult.getOutput().contains("Clicked"),
                "点击搜索按钮失败"
            );

            // 步骤4: 等待并获取搜索结果
            log.info("步骤4: 等待页面加载并获取搜索结果");
            Thread.sleep(2000); // 等待页面加载
            ToolExecuteResult textResult = executeAction("get_text", null);
            String searchResults = textResult.getOutput();

            // 验证搜索结果
            Assertions.assertTrue(
                searchResults.contains("Hello World"),
                "搜索结果中未找到 'Hello World'"
            );

            // 步骤5: 获取截图
            log.info("步骤5: 获取页面截图");
            ToolExecuteResult screenshotResult = executeAction("screenshot", null);
            Assertions.assertTrue(
                screenshotResult.getOutput().contains("Screenshot captured"),
                "获取截图失败"
            );

            // 步骤6: 获取页面HTML
            log.info("步骤6: 获取页面HTML");
            ToolExecuteResult htmlResult = executeAction("get_html", null);
            Assertions.assertNotNull(
                htmlResult.getOutput(),
                "获取页面HTML失败"
            );

            // 步骤7: 执行页面滚动
            log.info("步骤7: 执行页面滚动");
            Map<String, Object> scrollParams = new HashMap<>();
            scrollParams.put("action", "scroll");
            scrollParams.put("scroll_amount", 500);
            String scrollInput = JSON.toJSONString(scrollParams);
            ToolExecuteResult scrollDownResult = browserUseTool.run(scrollInput);
            Assertions.assertTrue(
                scrollDownResult.getOutput().contains("Scrolled down"),
                "页面滚动失败"
            );

            log.info("测试成功完成！");

        } catch (Exception e) {
            log.error("测试过程中发生错误", e);
            Assertions.fail("测试执行失败: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("测试浏览器标签页操作")
    void testBrowserTabs() {
        try {
            // 步骤1: 打开新标签页
            log.info("步骤1: 打开新标签页访问必应");
            ToolExecuteResult newTabResult = executeAction("new_tab", "https://www.bing.com");
            Assertions.assertTrue(
                newTabResult.getOutput().contains("Opened new tab"),
                "打开新标签页失败"
            );

            // 步骤2: 切换到新标签页
            log.info("步骤2: 切换到新标签页");
            ToolExecuteResult switchResult = executeAction("switch_tab", null, 1, null);
            Assertions.assertTrue(
                switchResult.getOutput().contains("Switched to tab"),
                "切换标签页失败"
            );

            // 步骤3: 刷新页面
            log.info("步骤3: 刷新页面");
            ToolExecuteResult refreshResult = executeAction("refresh", null);
            Assertions.assertEquals(
                "Refreshed current page",
                refreshResult.getOutput(),
                "刷新页面失败"
            );

            // 步骤4: 关闭当前标签页
            log.info("步骤4: 关闭当前标签页");
            ToolExecuteResult closeResult = executeAction("close_tab", null);
            Assertions.assertEquals(
                "Closed current tab",
                closeResult.getOutput(),
                "关闭标签页失败"
            );

            log.info("标签页操作测试成功完成！");

        } catch (Exception e) {
            log.error("测试过程中发生错误", e);
            Assertions.fail("测试执行失败: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("测试浏览器状态获取")
    void testBrowserState() {
        // 获取浏览器当前状态
        String currentState = browserUseTool.getCurrentToolStateString();
        
        // 验证状态信息完整性
        Assertions.assertNotNull(currentState, "获取浏览器状态失败");
        Assertions.assertTrue(
            currentState.contains("Current URL"),
            "状态信息中缺少URL信息"
        );
        Assertions.assertTrue(
            currentState.contains("Available tabs"),
            "状态信息中缺少标签页信息"
        );
        Assertions.assertTrue(
            currentState.contains("Interactive elements"),
            "状态信息中缺少可交互元素信息"
        );
    }


    // 辅助方法：执行浏览器操作
    private ToolExecuteResult executeAction(
            String action,
            String url) {
        return executeAction(action, url, null, null);
    }

    // 辅助方法：执行浏览器操作（带索引和文本）
    private ToolExecuteResult executeAction(
            String action,
            String url,
            Integer index,
            String text) {

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
