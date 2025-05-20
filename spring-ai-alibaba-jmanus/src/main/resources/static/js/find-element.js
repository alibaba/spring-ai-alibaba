// 要查找的文本
const searchText = "APP登录";

// 直接执行而不使用timeout
(function() {
  // 检测是否存在iframe并尝试在iframe中查找元素
  console.log("%c检查iframe中的元素", "font-weight: bold; font-size: 14px; color: orange;");
  const iframes = document.querySelectorAll('iframe');
  console.log(`页面中共有 ${iframes.length} 个iframe`);
  
  if (iframes.length > 0) {
    iframes.forEach((iframe, index) => {
      console.log(`--- iframe ${index+1} ---`);
      console.log("iframe元素:", iframe);
      console.log("iframe属性:");
      console.log("  - src:", iframe.src);
      console.log("  - id:", iframe.id);
      console.log("  - name:", iframe.name);
      
      // 尝试访问iframe内容
      try {
        const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
        console.log("成功访问iframe内容");
        
        // 在iframe中查找元素
        searchInDocument(iframeDoc, true);
      } catch (e) {
        console.error(`无法访问iframe内容 (可能是跨域限制): ${e.message}`);
        console.log("提示: 要访问跨域iframe的内容，您需要在iframe页面内执行脚本");
        
        // 提供在控制台中针对iframe执行脚本的方法
        if (iframe.name) {
          console.log(`%c要在这个iframe中执行查找，请在控制台中输入:`, "font-weight: bold; color: red;");
          console.log(`%cframes['${iframe.name}'].document.querySelectorAll('span.tabs-active')`, "color: blue; font-weight: bold;");
          console.log(`%c或者使用:`, "font-weight: bold;");
          console.log(`%c以下代码复制到控制台并回车执行:`, "font-weight: bold; color: red;");
          
          // 生成可复制到控制台的代码
          const searchCode = `
(function() {
  const searchText = "${searchText}";
  const tabsActive = document.querySelectorAll('span.tabs-active');
  console.log(\`找到 \${tabsActive.length} 个tabs-active元素:\`, tabsActive);
  tabsActive.forEach(el => {
    console.log("文本内容:", el.textContent);
    console.log("位置:", el.getBoundingClientRect());
  });
  
  // 查找所有包含文本的元素
  const elements = Array.from(document.querySelectorAll('*')).filter(el => {
    try {
      return el.textContent.trim().includes(searchText);
    } catch(e) { return false; }
  });
  console.log(\`找到 \${elements.length} 个包含文本的元素\`);
  elements.forEach(el => {
    console.log("元素:", el);
    console.log("文本:", el.textContent.trim());
    console.log("位置:", el.getBoundingClientRect());
  });
})();`;
          console.log(`%c${searchCode}`, "background-color: #f0f0f0; padding: 5px; border: 1px solid #ccc;");
        }
      }
    });
  }
  
  // 在当前文档中查找元素
  searchInDocument(document, false);
  
  // 搜索文档中的元素
  function searchInDocument(doc, isIframe) {
    const prefix = isIframe ? "Iframe中" : "主文档中";
    
    // 方法1：使用querySelector
    console.log(`%c${prefix}方法1：使用querySelector查找tabs-active元素`, "font-weight: bold; font-size: 14px; color: blue;");
    const tabsActive = doc.querySelectorAll('span.tabs-active');
    console.log(`找到 ${tabsActive.length} 个tabs-active元素:`, tabsActive);
    tabsActive.forEach((el, index) => {
      console.log(`--- tabs-active元素 ${index+1} ---`);
      console.log("元素:", el);
      console.log("文本内容:", el.textContent);
      console.log("HTML内容:", el.innerHTML);
      
      const rect = el.getBoundingClientRect();
      console.log("位置信息:");
      console.log(`  - x坐标: ${Math.round(rect.left + rect.width/2)}`);
      console.log(`  - y坐标: ${Math.round(rect.top + rect.height/2)}`);
      
      if (!isIframe) {
        highlightElement(el, 'blue');
      }
    });
    
    // 方法2：使用XPath
    console.log(`%c\n${prefix}方法2：使用XPath查找包含文本的元素`, "font-weight: bold; font-size: 14px; color: green;");
    try {
      const xpathResult = doc.evaluate(
        `//*[contains(text(), "${searchText}")]`,
        doc,
        null,
        XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,
        null
      );
      console.log(`找到 ${xpathResult.snapshotLength} 个XPath元素`);
      for (let i = 0; i < xpathResult.snapshotLength; i++) {
        const node = xpathResult.snapshotItem(i);
        console.log(`--- XPath元素 ${i+1} ---`);
        console.log("元素:", node);
        console.log("文本:", node.textContent);
        
        const rect = node.getBoundingClientRect();
        console.log("位置信息:");
        console.log(`  - x坐标: ${Math.round(rect.left + rect.width/2)}`);
        console.log(`  - y坐标: ${Math.round(rect.top + rect.height/2)}`);
        
        if (!isIframe) {
          highlightElement(node, 'green');
        }
      }
    } catch (e) {
      console.error("XPath查询出错:", e);
    }
    
    // 方法3：遍历所有元素
    console.log(`%c\n${prefix}方法3：遍历所有元素`, "font-weight: bold; font-size: 14px; color: red;");
    const allElements = Array.from(doc.querySelectorAll('*')).filter(el => {
      try {
        const text = (el.textContent || '').trim();
        return text.includes(searchText);
      } catch (e) {
        return false;
      }
    });
    console.log(`找到 ${allElements.length} 个包含文本的元素`);
    allElements.forEach((el, index) => {
      console.log(`--- 包含文本的元素 ${index+1} ---`);
      console.log("元素:", el);
      console.log("文本内容:", el.textContent.trim());
      
      const rect = el.getBoundingClientRect();
      console.log("位置信息:");
      console.log(`  - x坐标: ${Math.round(rect.left + rect.width/2)}`);
      console.log(`  - y坐标: ${Math.round(rect.top + rect.height/2)}`);
      
      if (!isIframe) {
        highlightElement(el, 'red');
      }
    });
    
    // 查找可能的特殊元素
    console.log(`%c\n${prefix}补充方法：查找可能的隐藏或特殊元素`, "font-weight: bold; font-size: 14px; color: purple;");
    try {
      const possibleElements = Array.from(doc.querySelectorAll('*')).filter(el => {
        try {
          // 检查元素或其子元素的文本内容
          if (el.innerHTML && el.innerHTML.includes(searchText)) return true;
          return false;
        } catch (e) {
          return false;
        }
      });
      
      console.log(`找到 ${possibleElements.length} 个可能包含文本的特殊元素`);
      possibleElements.forEach((el, index) => {
        if (!allElements.includes(el)) {
          console.log(`--- 特殊元素 ${index+1} ---`);
          console.log("元素:", el);
          console.log("HTML内容:", el.innerHTML);
          
          if (!isIframe) {
            highlightElement(el, 'purple');
          }
        }
      });
    } catch (e) {
      console.error("查找特殊元素时出错:", e);
    }
  }
  
  // 高亮函数
  function highlightElement(el, color) {
    if (!el) return;
    try {
      const rect = el.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) {
        console.warn("警告: 元素没有宽度或高度!", el);
        return;
      }
      
      const highlight = document.createElement('div');
      highlight.style.position = 'absolute';
      highlight.style.border = `3px solid ${color}`;
      highlight.style.backgroundColor = `rgba(${color === 'red' ? '255,0,0' : color === 'green' ? '0,255,0' : color === 'purple' ? '128,0,128' : '0,0,255'},0.2)`;
      highlight.style.zIndex = '10000';
      highlight.style.pointerEvents = 'none';
      highlight.style.left = (rect.left + window.scrollX) + 'px';
      highlight.style.top = (rect.top + window.scrollY) + 'px';
      highlight.style.width = rect.width + 'px';
      highlight.style.height = rect.height + 'px';
      
      // 添加元素中心点标记
      const centerMark = document.createElement('div');
      centerMark.style.position = 'absolute';
      centerMark.style.width = '6px';
      centerMark.style.height = '6px';
      centerMark.style.borderRadius = '50%';
      centerMark.style.backgroundColor = color;
      centerMark.style.top = (rect.top + rect.height/2 + window.scrollY - 3) + 'px';
      centerMark.style.left = (rect.left + rect.width/2 + window.scrollX - 3) + 'px';
      centerMark.style.zIndex = '10001';
      
      document.body.appendChild(highlight);
      document.body.appendChild(centerMark);
      
      // 6秒后移除高亮
      setTimeout(() => {
        try { 
          document.body.removeChild(highlight);
          document.body.removeChild(centerMark);
        } catch(e) {}
      }, 6000);
    } catch (e) {
      console.error("高亮元素时出错:", e);
    }
  }
})();
