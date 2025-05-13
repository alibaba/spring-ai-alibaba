package com.alibaba.cloud.ai.example.manus.tool.browser.actions;

import java.util.List;
import java.util.ArrayList;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Frame;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.fastjson.JSON;

public class GetElementPositionByNameAction extends BrowserAction {

    public GetElementPositionByNameAction(BrowserUseTool browserUseTool) {
        super(browserUseTool);
    }

    /**
     * 元素位置信息类，用于存储每个匹配元素的全局位置和文本信息
     */
    public static class ElementPosition {
        private int x;                  // It holds the absolute x coordinate
        private int y;                  // It holds the absolute y coordinate
        private String elementText;     // Element text content

        // 构造函数
        public ElementPosition() {}
        
        // 构造函数，只包含必要字段
        public ElementPosition(int x, int y, String elementText) {
            this.x = x;
            this.y = y;
            this.elementText = elementText;
        }

        // Getters and Setters
        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public String getElementText() {
            return elementText;
        }

        public void setElementText(String elementText) {
            this.elementText = elementText;
        }
    }

    @Override
    public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
        String elementName = request.getElementName();
        if (elementName == null || elementName.isEmpty()) {
            return new ToolExecuteResult("Element name is required for 'get_element_position' action");
        }

        Page page = browserUseTool.getDriver().newPage(); // 获取 Playwright 的 Page 实例

        // 结果列表，存储所有匹配的元素位置
        List<ElementPosition> positionResults = new ArrayList<>();

        // 在主文档中查找和处理元素
        findAndProcessElements(page, elementName, positionResults);

        // 处理所有 iframe 中的元素
        for (Frame frame : page.frames()) {
            findAndProcessElements(frame, elementName, positionResults);
        }

        // 返回结果
        String resultJson = JSON.toJSONString(positionResults);
        return new ToolExecuteResult(resultJson);
    }

    private void findAndProcessElements(Page pageOrFrame, String elementName, List<ElementPosition> results) {
        List<ElementHandle> elements = pageOrFrame.querySelectorAll("*");
        for (ElementHandle element : elements) {
            String text = element.textContent();
            if (text != null && text.toLowerCase().contains(elementName.toLowerCase())) {
                // 获取元素的位置信息
                com.microsoft.playwright.options.BoundingBox box = element.boundingBox();
                if (box != null) {
                    ElementPosition position = new ElementPosition();
                    position.setX((int) (box.x + box.width / 2));
                    position.setY((int) (box.y + box.height / 2));
                    position.setElementText(text.trim());
                    results.add(position);
                }
            }
        }
    }

    private void findAndProcessElements(Frame frame, String elementName, List<ElementPosition> results) {
        List<ElementHandle> elements = frame.querySelectorAll("*");
        for (ElementHandle element : elements) {
            String text = element.textContent();
            if (text != null && text.toLowerCase().contains(elementName.toLowerCase())) {
                // 获取元素的位置信息
                com.microsoft.playwright.options.BoundingBox box = element.boundingBox();
                if (box != null) {
                    ElementPosition position = new ElementPosition();
                    position.setX((int) (box.x + box.width / 2));
                    position.setY((int) (box.y + box.height / 2));
                    position.setElementText(text.trim());
                    results.add(position);
                }
            }
        }
    }
}
