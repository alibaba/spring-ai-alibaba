import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import './index.css';

const Message = ({ type, content, duration, onClose }) => {
  // 自动关闭逻辑
  useEffect(() => {
    if (duration > 0) {
      const timer = setTimeout(() => {
        onClose();
      }, duration);
      return () => clearTimeout(timer); // 清理定时器
    }
  }, [duration, onClose]);

  // 根据类型设置样式
  const getClassName = () => {
    switch (type) {
      case 'success':
        return 'message-success';
      case 'error':
        return 'message-error';
      case 'warning':
        return 'message-warning';
      case 'info':
        return 'message-info';
      default:
        return 'message-default';
    }
  };

  return (
    <div className={`message ${getClassName()}`}>
      <span>{content}</span>
    </div>
  );
};

// 定义 Prop 类型
Message.propTypes = {
  type: PropTypes.oneOf(['success', 'error', 'warning', 'info']),
  content: PropTypes.string.isRequired,
  duration: PropTypes.number,
  onClose: PropTypes.func.isRequired,
};

Message.defaultProps = {
  type: 'info',
  duration: 100, // 默认 2 秒后自动关闭
};

export default Message;
