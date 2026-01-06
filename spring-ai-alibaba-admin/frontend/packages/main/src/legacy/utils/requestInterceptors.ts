import { requestInterceptors } from './request';
import { notifyError, notifyWarning } from './notification';

// 全局错误处理拦截器
export const setupGlobalErrorHandling = () => {
  // 添加全局错误处理拦截器
  requestInterceptors.use({
    responseError: (error) => {
      // 根据错误状态码进行不同处理
      if (error.code === 401) {
        notifyWarning({
          message: '身份验证失败',
          description: '请重新登录后继续操作',
          duration: 5,
        });
        
        // 可以在这里添加重定向到登录页的逻辑
        // window.location.href = '/login';
      } else if (error.code === 403) {
        notifyError({
          message: '访问被拒绝',
          description: '您没有权限执行此操作',
          duration: 5,
        });
      } else if (error.code === 404) {
        notifyError({
          message: '资源不存在',
          description: '请求的资源未找到',
        });
      } else if (error.code >= 500) {
        notifyError({
          message: '服务器内部错误',
          description: '服务器遇到了一个错误，请稍后重试',
          duration: 6,
        });
      } else if (error.code === 0 || !error.code) {
        // 网络错误
        notifyError({
          message: '网络连接失败',
          description: '请检查网络连接后重试',
          duration: 6,
        });
      }

      // 继续抛出错误，让具体的组件处理
      throw error;
    },

    request: async (config) => {
      // 可以在这里添加全局请求头
      return config;
    },
  });
};

// 初始化全局错误处理
setupGlobalErrorHandling();