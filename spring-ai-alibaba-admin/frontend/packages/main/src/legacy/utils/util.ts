export const extractParametersFromDoubleBrace = (content: string) => {
  const regex = /\{\{(\w+)\}\}/g;
  const parameters: string[] = [];
  let match;
  while ((match = regex.exec(content)) !== null) {
    if (!parameters.includes(match[1])) {
      parameters.push(match[1]);
    }
  }
  return parameters;
};

export const safeJSONStringify = <T>(obj: any, fallback: () => T = () => "" as T, replacer?: (this: any, key: string, value: any) => any, space?: number) => {
  try {
    return JSON.stringify(obj, replacer, space);
  } catch (error) {
    return fallback();
  }
};

export const safeJSONParse = <T>(jsonString: string, fallback: () => T = () => ({} as T), reviver?: (this: any, key: string, value: any) => any) => {
  try {
    return JSON.parse(jsonString, reviver);
  } catch (error) {
    return fallback();
  }
};

export function copyToClipboard(text: string) {
  // 返回一个 Promise 对象
  return new Promise((resolve, reject) => {
    if (navigator.clipboard && window.isSecureContext) {
      // 使用 Clipboard API 写入剪切板
      navigator.clipboard.writeText(text).then(resolve, reject);
    } else {
      // 非安全环境下或不支持 Clipboard API 的浏览器的回退方法
      const textArea = document.createElement('textarea');
      textArea.value = text;

      // 避免出现滚动条
      textArea.style.position = 'fixed';
      textArea.style.top = '0';
      textArea.style.left = '0';
      textArea.style.width = '2em';
      textArea.style.height = '2em';
      textArea.style.padding = '0';
      textArea.style.border = 'none';
      textArea.style.outline = 'none';
      textArea.style.boxShadow = 'none';
      textArea.style.background = 'transparent';

      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();

      try {
        const successful = document.execCommand('copy');
        successful ? resolve(void 0) : reject();
      } catch (err) {
        reject(err); // 如果执行失败，调用 reject
      }
      document.body.removeChild(textArea);
    }
  });
}
