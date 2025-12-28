import { notification } from 'antd';

// 配置全局通知样式
notification.config({
  placement: 'topRight',
  top: 50,
  duration: 4.5,
  rtl: false,
});

export interface NotificationOptions {
  message: string;
  description?: string;
  duration?: number;
  placement?: 'top' | 'topLeft' | 'topRight' | 'bottom' | 'bottomLeft' | 'bottomRight';
}

// 成功通知
export const notifySuccess = (options: NotificationOptions) => {
  notification.success({
    message: options.message,
    description: options.description,
    duration: options.duration || 3,
    placement: options.placement || 'topRight',
  });
};

// 错误通知
export const notifyError = (options: NotificationOptions) => {
  notification.error({
    message: options.message,
    description: options.description,
    duration: options.duration || 5,
    placement: options.placement || 'topRight',
  });
};

// 警告通知
export const notifyWarning = (options: NotificationOptions) => {
  notification.warning({
    message: options.message,
    description: options.description,
    duration: options.duration || 4,
    placement: options.placement || 'topRight',
  });
};

// 信息通知
export const notifyInfo = (options: NotificationOptions) => {
  notification.info({
    message: options.message,
    description: options.description,
    duration: options.duration || 3,
    placement: options.placement || 'topRight',
  });
};

// API 错误处理
export const handleApiError = (error: any, context: string = '操作') => {
  let message = '操作失败';
  let description = '请稍后重试';

  if (error && typeof error === 'object') {
    // 处理不同类型的错误
    if (error.message) {
      message = `${context}失败`;
      description = error.message;
    } else if (error.code && error.code !== 200) {
      message = `${context}失败 (错误码: ${error.code})`;
      description = error.message || '服务器返回异常';
    } else if (typeof error === 'string') {
      message = `${context}失败`;
      description = error;
    }
  } else if (typeof error === 'string') {
    message = `${context}失败`;
    description = error;
  }

  notifyError({ message, description });
};

// 网络错误处理
export const handleNetworkError = (context: string = '操作') => {
  notifyError({
    message: `${context}失败`,
    description: '网络连接异常，请检查网络后重试',
    duration: 6,
  });
};

// 表单验证错误处理
export const handleValidationError = (message: string, description?: string) => {
  notifyWarning({
    message: '输入验证失败',
    description: description || message,
  });
};