/**
 * Legacy 路径工具函数
 * 统一处理 legacy 页面的路径，自动添加 /admin 前缀
 */

/**
 * 获取完整的 legacy 路径（自动添加 /admin 前缀）
 * @param path 原始路径，如 '/prompts', '/prompt-detail' 等
 * @returns 完整的路径，如 '/admin/prompts', '/admin/prompt-detail' 等
 */
export const getLegacyPath = (path: string): string => {
  // 如果路径已经以 /admin 开头，直接返回
  if (path.startsWith('/admin')) {
    return path;
  }
  
  // 如果路径以 / 开头，添加 /admin 前缀
  if (path.startsWith('/')) {
    return `/admin${path}`;
  }
  
  // 如果路径不以 / 开头，添加 /admin/
  return `/admin/${path}`;
};

/**
 * 构建带查询参数的完整路径
 * @param path 基础路径
 * @param params 查询参数对象
 * @returns 完整的路径，如 '/admin/prompt-detail?promptKey=xxx'
 */
export const buildLegacyPath = (path: string, params?: Record<string, string | number | null | undefined>): string => {
  const fullPath = getLegacyPath(path);
  
  if (!params || Object.keys(params).length === 0) {
    return fullPath;
  }
  
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined) {
      searchParams.append(key, String(value));
    }
  });
  
  const queryString = searchParams.toString();
  return queryString ? `${fullPath}?${queryString}` : fullPath;
};

