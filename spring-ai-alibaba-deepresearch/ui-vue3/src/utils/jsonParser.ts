/**
 * JSON解析工具类
 * 用于解析类似 { "node": "__start__" } { "node": "__end__" } 这样的文本
 * 并返回对应的JSON数组
 */

/**
 * 解析类JSON文本为JSON数组
 * @param text 包含多个JSON对象的文本字符串
 * @returns 解析后的JSON对象数组
 */
export function parseJsonText(text: string): any[] {
  if (!text || typeof text !== 'string') {
    return [];
  }

  const result: any[] = [];
  const trimmedText = text.trim();
  
  if (!trimmedText) {
    return result;
  }

  // 使用正则表达式匹配所有的JSON对象
  // 匹配以 { 开始，以 } 结束的完整JSON对象
  const jsonRegex = /\{[^{}]*\}/g;
  const matches = trimmedText.match(jsonRegex);

  if (!matches) {
    return result;
  }

  // 解析每个匹配到的JSON字符串
  for (const match of matches) {
    try {
      const jsonObj = JSON.parse(match.trim());
      result.push(jsonObj);
    } catch (error) {
      console.warn('解析JSON对象失败:', match, error);
      // 继续处理其他JSON对象，不中断整个解析过程
    }
  }

  return result;
}

/**
 * 解析类JSON文本为JSON数组（更严格的版本）
 * 支持嵌套的JSON对象
 * @param text 包含多个JSON对象的文本字符串
 * @returns 解析后的JSON对象数组
 */
export function parseJsonTextStrict(text: string): any[] {
  if (!text || typeof text !== 'string') {
    return [];
  }

  const result: any[] = [];
  const trimmedText = text.trim();
  
  if (!trimmedText) {
    return result;
  }

  let currentIndex = 0;
  
  while (currentIndex < trimmedText.length) {
    // 跳过空白字符
    while (currentIndex < trimmedText.length && /\s/.test(trimmedText[currentIndex])) {
      currentIndex++;
    }
    
    if (currentIndex >= trimmedText.length) {
      break;
    }
    
    // 查找JSON对象的开始
    if (trimmedText[currentIndex] === '{') {
      let braceCount = 0;
      let startIndex = currentIndex;
      
      // 找到完整的JSON对象
      while (currentIndex < trimmedText.length) {
        if (trimmedText[currentIndex] === '{') {
          braceCount++;
        } else if (trimmedText[currentIndex] === '}') {
          braceCount--;
          if (braceCount === 0) {
            // 找到完整的JSON对象
            const jsonStr = trimmedText.substring(startIndex, currentIndex + 1);
            try {
              const jsonObj = JSON.parse(jsonStr);
              result.push(jsonObj);
            } catch (error) {
              console.warn('解析JSON对象失败:', jsonStr, error);
            }
            currentIndex++;
            break;
          }
        }
        currentIndex++;
      }
    } else {
      // 跳过非JSON字符
      currentIndex++;
    }
  }

  return result;
}

/**
 * 默认导出解析函数
 */
export default parseJsonText;