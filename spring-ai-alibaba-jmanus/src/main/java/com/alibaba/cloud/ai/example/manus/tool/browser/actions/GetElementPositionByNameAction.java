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
package com.alibaba.cloud.ai.example.manus.tool.browser.actions;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.WebElementWrapper;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.fastjson.JSON;

public class GetElementPositionByNameAction extends BrowserAction {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetElementPositionByNameAction.class);

    public GetElementPositionByNameAction(BrowserUseTool browserUseTool) {
        super(browserUseTool);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
        String elementName = request.getElementName();
        if (elementName == null || elementName.isEmpty()) {
            return new ToolExecuteResult("Element name is required for 'get_element_position' action");
        }
        
        WebDriver driver = browserUseTool.getDriver();
        List<WebElementWrapper> interactiveElements = getInteractiveElements(driver);
        
        // 查找匹配元素名称的元素
        WebElement targetElement = null;
        for (WebElementWrapper wrapper : interactiveElements) {
            WebElement element = wrapper.getElement();
            String elementText = element.getText().trim();
            String elementInfo = wrapper.getElementInfoString();
            
            // 匹配元素名称（文本内容或元素信息中包含指定名称）
            if (elementText.contains(elementName) || elementInfo.contains(elementName)) {
                targetElement = element;
                // 准备互动（处理可能的iframe切换）
                wrapper.prepareForInteraction(driver);
                break;
            }
        }
        
        if (targetElement == null) {
            return new ToolExecuteResult("Element with name '" + elementName + "' not found");
        }
        
        // 使用JavaScript获取元素位置
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Map<String, Object> position = new HashMap<>();
        
        // 获取元素在页面上的坐标
        Map<String, Long> rect = (Map<String, Long>) js.executeScript(
            "var rect = arguments[0].getBoundingClientRect();" +
            "return {" +
            "   left: rect.left + window.scrollX," +
            "   top: rect.top + window.scrollY," +
            "   width: rect.width," +
            "   height: rect.height" +
            "};", targetElement);
        
        if (rect != null) {
            // 计算元素中心点坐标
            long centerX = rect.get("left") + rect.get("width") / 2;
            long centerY = rect.get("top") + rect.get("height") / 2;
            
            position.put("x", centerX);
            position.put("y", centerY);
            position.put("width", rect.get("width"));
            position.put("height", rect.get("height"));
            position.put("element_name", elementName);
            
            // 返回JSON格式的位置信息
            return new ToolExecuteResult(JSON.toJSONString(position));
        }
        
        return new ToolExecuteResult("Failed to get position for element with name '" + elementName + "'");
    }
}
