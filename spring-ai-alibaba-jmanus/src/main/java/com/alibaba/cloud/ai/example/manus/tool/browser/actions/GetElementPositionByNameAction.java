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

    @SuppressWarnings("unchecked")
    @Override
    public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
        String elementName = request.getElementName();
        if (elementName == null || elementName.isEmpty()) {
            return new ToolExecuteResult("Element name is required for 'get_element_position' action");
        }
        // 获取当前浏览器驱动
        WebDriver driver = browserUseTool.getDriver();
        
        // 结果列表，存储所有匹配的元素位置
        List<Map<String, Object>> positionResults = new ArrayList<>();
        
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
            String elementName, List<Map<String, Object>> results, String iframePath) {
        
        // 执行JavaScript来查找元素并获取位置信息
        List<Map<String, Object>> elements = (List<Map<String, Object>>) js.executeScript(
            """
            function findElementsByText(searchText) {
              const elements = Array.from(document.querySelectorAll('*'));
              const matchedElements = [];
              
              elements.filter(el => {
                const text = (el.textContent || '').trim();
                if (text.includes(searchText)) {
                  const rect = el.getBoundingClientRect();
                  matchedElements.push({
                    x: Math.round(rect.left + rect.width / 2),
                    y: Math.round(rect.top + rect.height / 2),
                    width: rect.width,
                    height: rect.height,
                    left: rect.left,
                    top: rect.top,
                    elementInfo: el.outerHTML,
                    elementText: text
                  });
                  return true;
                }
                
                const id = (el.id || '').toString();
                const className = String(el.className || '');
                const name = (el.getAttribute('name') || '').toString();
                const alt = (el.getAttribute('alt') || '').toString();
                const title = (el.getAttribute('title') || '').toString();
                const ariaLabel = (el.getAttribute('aria-label') || '').toString();
                
                if (id.includes(searchText) || 
                    className.includes(searchText) || 
                    name.includes(searchText) || 
                    alt.includes(searchText) || 
                    title.includes(searchText) || 
                    ariaLabel.includes(searchText)) {
                  const rect = el.getBoundingClientRect();
                  matchedElements.push({
                    x: Math.round(rect.left + rect.width / 2),
                    y: Math.round(rect.top + rect.height / 2),
                    width: rect.width,
                    height: rect.height,
                    left: rect.left,
                    top: rect.top,
                    elementInfo: el.outerHTML,
                    elementText: text
                  });
                  return true;
                }
                
                return false;
              });
              
              return matchedElements;
            }
            return findElementsByText(arguments[0]);
            """,
            elementName);
        
        if (elements != null && !elements.isEmpty()) {
            for (Map<String, Object> element : elements) {
                // 添加iframe信息
                element.put("iframePath", iframePath);
                element.put("elementName", elementName);
                
                // 添加到结果列表
                results.add(element);
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
            String elementName, List<Map<String, Object>> results) {
        
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
                // 获取iframe的HTML内容和属性进行调试
                String iframeSrc = iframe.getAttribute("src") != null ? iframe.getAttribute("src") : "no-src";
                String iframeId = iframe.getAttribute("id") != null ? iframe.getAttribute("id") : "no-id";
                String iframeClass = iframe.getAttribute("class") != null ? iframe.getAttribute("class") : "no-class";
                log.info("处理iframe: path={}, src={}, id={}, class={}", currentPath, iframeSrc, iframeId, iframeClass);
                
                // 切换到iframe
                driver.switchTo().frame(iframe);
                
                // 记录iframe的document信息
                JavascriptExecutor js = (JavascriptExecutor) driver;
                String iframeUrl = (String) js.executeScript("return document.location.href");
                String iframeTitle = (String) js.executeScript("return document.title");
                log.info("iframe内部信息: url={}, title={}", iframeUrl, iframeTitle);
                
                // 获取iframe的HTML大小
                String iframeHtmlSize = (String) js.executeScript(
                    "return 'HTML长度: ' + document.documentElement.outerHTML.length + ' 字节';"
                );
                log.info("iframe HTML大小: {}", iframeHtmlSize);
                
                // 方法2：使用JavaScript查找元素，无论是否是交互式的
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
