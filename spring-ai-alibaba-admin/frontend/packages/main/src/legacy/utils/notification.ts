import { notification } from 'antd';
import $i18n from '@/i18n';

// Configure global notification style
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

export const notifySuccess = (options: NotificationOptions) => {
  notification.success({
    message: options.message,
    description: options.description,
    duration: options.duration || 3,
    placement: options.placement || 'topRight',
  });
};

export const notifyError = (options: NotificationOptions) => {
  notification.error({
    message: options.message,
    description: options.description,
    duration: options.duration || 5,
    placement: options.placement || 'topRight',
  });
};

export const notifyWarning = (options: NotificationOptions) => {
  notification.warning({
    message: options.message,
    description: options.description,
    duration: options.duration || 4,
    placement: options.placement || 'topRight',
  });
};

export const notifyInfo = (options: NotificationOptions) => {
  notification.info({
    message: options.message,
    description: options.description,
    duration: options.duration || 3,
    placement: options.placement || 'topRight',
  });
};

export const handleApiError = (
  error: any,
  context: string = $i18n.get({
    id: 'legacy.notification.operation',
    dm: '操作',
  }),
) => {
  let message = $i18n.get({
    id: 'legacy.notification.operationFailed',
    dm: '操作失败',
  });
  let description = $i18n.get({
    id: 'legacy.notification.retryLater',
    dm: '请稍后重试',
  });

  if (error && typeof error === 'object') {
    if (error.message) {
      message = $i18n.get(
        {
          id: 'legacy.notification.contextFailed',
          dm: '{context}失败',
        },
        { context },
      );
      description = error.message;
    } else if (error.code && error.code !== 200) {
      message = $i18n.get(
        {
          id: 'legacy.notification.contextFailedWithCode',
          dm: '{context}失败 (错误码: {code})',
        },
        { context, code: error.code },
      );
      description =
        error.message ||
        $i18n.get({
          id: 'legacy.notification.serverException',
          dm: '服务器返回异常',
        });
    } else if (typeof error === 'string') {
      message = $i18n.get(
        {
          id: 'legacy.notification.contextFailed',
          dm: '{context}失败',
        },
        { context },
      );
      description = error;
    }
  } else if (typeof error === 'string') {
    message = $i18n.get(
      {
        id: 'legacy.notification.contextFailed',
        dm: '{context}失败',
      },
      { context },
    );
    description = error;
  }

  notifyError({ message, description });
};

export const handleNetworkError = (
  context: string = $i18n.get({
    id: 'legacy.notification.operation',
    dm: '操作',
  }),
) => {
  notifyError({
    message: $i18n.get(
      {
        id: 'legacy.notification.contextFailed',
        dm: '{context}失败',
      },
      { context },
    ),
    description: $i18n.get({
      id: 'legacy.notification.networkError',
      dm: '网络连接异常，请检查网络后重试',
    }),
    duration: 6,
  });
};

export const handleValidationError = (message: string, description?: string) => {
  notifyWarning({
    message: $i18n.get({
      id: 'legacy.notification.validationFailed',
      dm: '输入验证失败',
    }),
    description: description || message,
  });
};
