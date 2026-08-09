import { notification } from 'antd';

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

const failedMessage = (context: string, suffix = '') => {
  const base = context?.trim() ? context.trim() : 'Operation';
  // If caller already passed a full English sentence, keep it.
  if (/failed|error|unable|cannot/i.test(base)) {
    return suffix ? `${base}${suffix}` : base;
  }
  return `${base} failed${suffix}`;
};

export const handleApiError = (error: any, context: string = 'Operation') => {
  let message = failedMessage(context);
  let description = 'Please try again later';

  if (error && typeof error === 'object') {
    if (error.message) {
      message = failedMessage(context);
      description = error.message;
    } else if (error.code && error.code !== 200) {
      message = failedMessage(context, ` (code: ${error.code})`);
      description = error.message || 'Unexpected server response';
    } else if (typeof error === 'string') {
      message = failedMessage(context);
      description = error;
    }
  } else if (typeof error === 'string') {
    message = failedMessage(context);
    description = error;
  }

  notifyError({ message, description });
};

export const handleNetworkError = (context: string = 'Operation') => {
  notifyError({
    message: failedMessage(context),
    description: 'Network error. Check your connection and try again.',
    duration: 6,
  });
};

export const handleValidationError = (message: string, description?: string) => {
  notifyWarning({
    message: 'Validation failed',
    description: description || message,
  });
};
