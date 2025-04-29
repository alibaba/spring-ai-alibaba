package com.alibaba.cloud.ai.example.manus.tool.browser.actions;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.fastjson.JSON;

public class GetElementPositionByNameAction extends BrowserAction {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetElementPositionByNameAction.class);

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
        // 获取当前浏览器驱动
        WebDriver driver = browserUseTool.getDriver();
        
        // 结果列表，存储所有匹配的元素位置
        List<ElementPosition> positionResults = new ArrayList<>();
        
        // 方法2：使用XPath查找元素，无论元素是否是交互式的
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        // 在主文档中查找和处理元素
        findAndProcessElementsByJS(driver, js, elementName, positionResults, "");
        
        // 处理所有iframe中的元素
        processIframesForPosition(driver, "", null, elementName, positionResults);
        
        // 返回结果
        String resultJson = JSON.toJSONString(positionResults);
        return new ToolExecuteResult(resultJson);
    }
    
    /**
     * 使用JavaScript查找并处理元素
     * @param driver WebDriver实例
     * @param js JavascriptExecutor实例
     * @param elementName 要查找的元素名称
     * @param results 结果列表
     * @param iframePath iframe路径（如果在主文档中则为空字符串）
     */
    @SuppressWarnings("unchecked")
    private void findAndProcessElementsByJS(WebDriver driver, JavascriptExecutor js, 
            String elementName, List<ElementPosition> results, String iframePath) {
        
        // 获取iframe的位置信息(如果在iframe中)
        int iframeOffsetX = 0;
        int iframeOffsetY = 0;
        
        if (!iframePath.isEmpty()) {
            // 切换到默认内容获取iframe位置
            driver.switchTo().defaultContent();
            
            // 解析iframe路径并依次遍历查找
            String[] pathParts = iframePath.split("/");
            
            for (int i = 0; i < pathParts.length; i++) {
                try {
                    int index = Integer.parseInt(pathParts[i]);
                    List<WebElement> frames = driver.findElements(By.tagName("iframe"));
                    
                    if (index < frames.size()) {
                        WebElement frame = frames.get(index);
                        // 获取iframe的位置
                        org.openqa.selenium.Point framePosition = frame.getLocation();
                        iframeOffsetX += framePosition.getX();
                        iframeOffsetY += framePosition.getY();
                        
                        // 切换到该iframe
                        driver.switchTo().frame(index);
                    }
                } catch (Exception e) {
                    log.warn("Error calculating iframe position for path {}: {}", iframePath, e.getMessage());
                }
            }
        }
        
        // 执行JavaScript来查找元素并获取位置信息
        List<Map<String, Object>> elements = (List<Map<String, Object>>) js.executeScript(
            """
            function findElementsByText(searchText) {
              const elements = Array.from(document.querySelectorAll('*'));
              const matchedElements = [];
              const processedElements = new Set(); // 用于避免重复处理相同元素
              
              // 按照元素DOM树深度排序，优先处理最内层的元素
              elements.sort((a, b) => {
                const depthA = getElementDepth(a);
                const depthB = getElementDepth(b);
                return depthB - depthA; // 深度大的排在前面
              });
              
              function getElementDepth(el) {
                let depth = 0;
                let parent = el.parentNode;
                while (parent) {
                  depth++;
                  parent = parent.parentNode;
                }
                return depth;
              }
              
              // 只保留可能是交互目标的元素类型
              const filteredElements = elements.filter(el => {
                const tagName = el.tagName.toLowerCase();
                return tagName === 'a' || tagName === 'button' || tagName === 'input' || 
                       tagName === 'select' || tagName === 'textarea' || tagName === 'label' ||
                       tagName === 'div' || tagName === 'span' || tagName === 'li' || 
                       el.getAttribute('role') === 'button' || 
                       el.getAttribute('tabindex') === '0';
              });
              
              for (const el of filteredElements) {
                // 跳过已处理的元素
                if (processedElements.has(el)) continue;
                
                const text = (el.textContent || '').trim();
                let matched = false;
                
                // 精确匹配优先
                if (text === searchText) {
                  matched = true;
                }
                // 然后考虑包含关系
                else if (text.includes(searchText)) {
                  matched = true;
                }
                // 如果文本内容不匹配，检查其他属性
                else {
                  const id = (el.id || '').toString();
                  const className = String(el.className || '');
                  const name = (el.getAttribute('name') || '').toString();
                  const alt = (el.getAttribute('alt') || '').toString();
                  const title = (el.getAttribute('title') || '').toString();
                  const ariaLabel = (el.getAttribute('aria-label') || '').toString();
                  
                  if (id === searchText || name === searchText || alt === searchText || 
                      title === searchText || ariaLabel === searchText) {
                    matched = true;
                  }
                  else if (id.includes(searchText) || className.includes(searchText) || 
                      name.includes(searchText) || alt.includes(searchText) || 
                      title.includes(searchText) || ariaLabel.includes(searchText)) {
                    matched = true;
                  }
                }
                
                if (matched) {
                  // 将匹配元素及其所有父元素标记为已处理
                  let current = el;
                  while (current && current !== document) {
                    processedElements.add(current);
                    current = current.parentNode;
                  }
                  
                  // 获取元素位置信息
                  const rect = el.getBoundingClientRect();
                  // 过滤掉尺寸为0或区域不在可视范围内的元素
                  if (rect.width > 0 && rect.height > 0 && 
                      rect.top >= 0 && rect.left >= 0 && 
                      rect.bottom <= window.innerHeight && 
                      rect.right <= window.innerWidth) {
                    matchedElements.push({
                      x: Math.round(rect.left + rect.width / 2),
                      y: Math.round(rect.top + rect.height / 2),
                      elementText: text
                    });
                  }
                }
              }
              
              return matchedElements;
            }
            return findElementsByText(arguments[0]);
            """,
            elementName);
        
        if (elements != null && !elements.isEmpty()) {
            for (Map<String, Object> element : elements) {
                // 创建新的ElementPosition对象
                ElementPosition position = new ElementPosition();
                
                // 计算全局坐标（iframe偏移量 + 元素在iframe内的坐标）
                int globalX = ((Number) element.get("x")).intValue() + iframeOffsetX;
                int globalY = ((Number) element.get("y")).intValue() + iframeOffsetY;
                
                // 设置全局坐标
                position.setX(globalX);
                position.setY(globalY);
                
                // 设置元素文本
                position.setElementText((String) element.get("elementText"));
                
                // 添加到结果列表
                results.add(position);
            }
        }
        
        // 如果在iframe中，恢复到当前iframe的上下文
        if (!iframePath.isEmpty()) {
            driver.switchTo().defaultContent();
            String[] pathParts = iframePath.split("/");
            for (int i = 0; i < pathParts.length; i++) {
                try {
                    int index = Integer.parseInt(pathParts[i]);
                    driver.switchTo().frame(index);
                } catch (Exception e) {
                    log.warn("Error switching back to iframe {}: {}", iframePath, e.getMessage());
                }
            }
        }
    }
    
    /**
     * 递归处理页面中的所有iframe元素，查找匹配的元素并获取位置
     * @param driver WebDriver实例
     * @param parentPath 父iframe的路径
     * @param parentIframe 父iframe元素
     * @param elementName 要查找的元素名称
     * @param results 结果列表
     */
    private void processIframesForPosition(WebDriver driver, String parentPath, WebElement parentIframe, 
            String elementName, List<ElementPosition> results) {
        
        // 查找当前上下文中的所有iframe
        List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
        log.info("Found {} iframes", iframes.size());
        
        for (int i = 0; i < iframes.size(); i++) {
            WebElement iframe = iframes.get(i);
            
            // 确保iframe已加载完成
            boolean isIframeLoaded = false;
            int maxAttempts = 20; // 最大尝试次数
            int attempts = 0;
            
            while (!isIframeLoaded && attempts < maxAttempts) {
                try {
                    // 尝试获取iframe的document.readyState
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    driver.switchTo().frame(iframe);
                    String readyState = (String) js.executeScript("return document.readyState");
                    log.info("iframe readyState: {}, attempts: {}", readyState, attempts);
                    
                    if ("complete".equals(readyState)) {
                        isIframeLoaded = true;
                        driver.switchTo().parentFrame(); // 切回父frame
                        log.info("iframe loaded successfully");
                        break;
                    }
                    
                    driver.switchTo().parentFrame(); // 切回父frame
                    attempts++;
                    Thread.sleep(500); // 等待500毫秒
                }
                catch (Exception e) {
                    log.info("Wait for iframe loading failed at attempt {}: {}", attempts, e.getMessage());
                    attempts++;
                    try {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            if (!isIframeLoaded) {
                log.warn("iframe loading timed out, skipping this iframe");
                continue;
            }
            
            // 构建iframe路径
            String currentPath = parentPath.isEmpty() ? String.valueOf(i) : parentPath + "/" + i;
            
            try {
                // 获取iframe的属性进行调试
                String iframeSrc = iframe.getAttribute("src") != null ? iframe.getAttribute("src") : "no-src";
                String iframeId = iframe.getAttribute("id") != null ? iframe.getAttribute("id") : "no-id";
                String iframeClass = iframe.getAttribute("class") != null ? iframe.getAttribute("class") : "no-class";
                log.info("处理iframe: path={}, src={}, id={}, class={}", currentPath, iframeSrc, iframeId, iframeClass);
                
                // 切换到iframe
                driver.switchTo().frame(iframe);
                
                // 记录iframe的文档信息
                JavascriptExecutor js = (JavascriptExecutor) driver;
                String iframeUrl = (String) js.executeScript("return document.location.href");
                String iframeTitle = (String) js.executeScript("return document.title");
                log.info("iframe内部信息: url={}, title={}", iframeUrl, iframeTitle);
                
                // 使用JavaScript查找元素（此方法内会处理全局坐标）
                findAndProcessElementsByJS(driver, js, elementName, results, currentPath);
                
                // 递归处理嵌套iframe
                processIframesForPosition(driver, currentPath, iframe, elementName, results);
                
                // 切回父级上下文
                if (parentIframe == null) {
                    driver.switchTo().defaultContent();
                }
                else {
                    driver.switchTo().parentFrame();
                }
            }
            catch (Exception e) {
                log.warn("Processing iframe failed, path: " + currentPath + ", error: " + e.getMessage());
                // 确保即使处理某个iframe失败，也回到正确的上下文
                if (parentIframe == null) {
                    driver.switchTo().defaultContent();
                }
                else {
                    driver.switchTo().parentFrame();
                }
            }
        }
    }
}
