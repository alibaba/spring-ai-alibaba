/**
 * JSON解析工具类
 * 用于解析类似 { "node": "__start__" } { "node": "__end__" } 这样的文本
 * 并返回对应的JSON数组
 */

/**
 * 解析类JSON文本为JSON数组（更严格的版本）
 * 支持嵌套的JSON对象和字符串内的转义字符
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
      const startIndex = currentIndex;
      const jsonEndIndex = findJsonObjectEnd(trimmedText, currentIndex);

      if (jsonEndIndex !== -1) {
        const jsonStr = trimmedText.substring(startIndex, jsonEndIndex + 1);
        try {
          const jsonObj = JSON.parse(jsonStr);
          result.push(jsonObj);
        } catch (error) {
          console.warn('解析JSON对象失败:', jsonStr, error);
        }
        currentIndex = jsonEndIndex + 1;
      } else {
        // 如果找不到完整的JSON对象，跳过当前字符
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
 * 查找JSON对象的结束位置，正确处理字符串内的转义字符
 * @param text 文本字符串
 * @param startIndex 开始位置（应该是'{'的位置）
 * @returns JSON对象结束位置的索引，如果找不到返回-1
 */
function findJsonObjectEnd(text: string, startIndex: number): number {
  let braceCount = 0;
  let inString = false;
  let escapeNext = false;

  for (let i = startIndex; i < text.length; i++) {
    const char = text[i];

    if (escapeNext) {
      // 如果前一个字符是转义符，跳过当前字符
      escapeNext = false;
      continue;
    }

    if (char === '\\' && inString) {
      // 遇到转义符
      escapeNext = true;
      continue;
    }

    if (char === '"' && !escapeNext) {
      // 遇到引号，切换字符串状态
      inString = !inString;
      continue;
    }

    if (!inString) {
      if (char === '{') {
        braceCount++;
      } else if (char === '}') {
        braceCount--;
        if (braceCount === 0) {
          return i;
        }
      }
    }
  }

  return -1; // 没有找到完整的JSON对象
}



/**
 * 默认导出解析函数
 */
export default parseJsonTextStrict;
