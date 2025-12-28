import React from 'react';
import { Result, Button, Alert } from 'antd';
import { ExclamationCircleOutlined, ReloadOutlined } from '@ant-design/icons';

interface ErrorDisplayProps {
  error: string | Error | null;
  title?: string;
  showRetry?: boolean;
  onRetry?: () => void;
  type?: 'inline' | 'page' | 'alert';
  className?: string;
}

const ErrorDisplay: React.FC<ErrorDisplayProps> = ({
  error,
  title = '操作失败',
  showRetry = true,
  onRetry,
  type = 'page',
  className = ''
}) => {
  if (!error) return null;

  const errorMessage = typeof error === 'string' ? error : error.message || '未知错误';

  if (type === 'alert') {
    return (
      <Alert
        message={title}
        description={errorMessage}
        type="error"
        showIcon
        className={className}
        action={
          showRetry && onRetry ? (
            <Button
              size="small"
              icon={<ReloadOutlined />}
              onClick={onRetry}
            >
              重试
            </Button>
          ) : undefined
        }
      />
    );
  }

  if (type === 'inline') {
    return (
      <div className={`bg-red-50 border border-red-200 rounded-lg p-4 ${className}`}>
        <div className="flex items-start">
          <ExclamationCircleOutlined className="text-red-500 text-lg mr-3 mt-0.5" />
          <div className="flex-1">
            <h3 className="text-sm font-medium text-red-900 mb-1">{title}</h3>
            <p className="text-sm text-red-700">{errorMessage}</p>
            {showRetry && onRetry && (
              <Button
                type="link"
                size="small"
                icon={<ReloadOutlined />}
                onClick={onRetry}
                className="text-red-600 hover:text-red-800 px-0 mt-2"
              >
                重试
              </Button>
            )}
          </div>
        </div>
      </div>
    );
  }

  // Default page type
  return (
    <div className={`py-8 ${className}`}>
      <Result
        status="error"
        title={title}
        subTitle={errorMessage}
        extra={
          showRetry && onRetry ? (
            <Button
              type="primary"
              icon={<ReloadOutlined />}
              onClick={onRetry}
            >
              重试
            </Button>
          ) : undefined
        }
      />
    </div>
  );
};

export default ErrorDisplay;