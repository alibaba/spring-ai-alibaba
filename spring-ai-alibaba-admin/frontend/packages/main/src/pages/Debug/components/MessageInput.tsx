import React, { useState, useRef, KeyboardEvent } from 'react';
import { Button, Upload, message } from 'antd';
import { SendOutlined, PaperClipOutlined, LoadingOutlined } from '@ant-design/icons';
import { useChatContext } from '../contexts/ChatContext';
import styles from '../index.module.less';

const MessageInput: React.FC = () => {
  const [inputValue, setInputValue] = useState('');
  const [attachedFiles, setAttachedFiles] = useState<File[]>([]);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const { sendMessage, state } = useChatContext();

  const handleSend = async () => {
    const content = inputValue.trim();
    if (!content && attachedFiles.length === 0) return;

    try {
      await sendMessage(content, attachedFiles);
      setInputValue('');
      setAttachedFiles([]);

      // Reset textarea height
      if (textareaRef.current) {
        textareaRef.current.style.height = '40px';
      }
    } catch (error) {
      message.error('发送消息失败');
    }
  };

  const handleKeyPress = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter') {
      if (e.shiftKey) {
        // Allow new line with Shift+Enter
        return;
      }
      e.preventDefault();
      handleSend();
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInputValue(e.target.value);

    // Auto-resize textarea
    const textarea = e.target;
    textarea.style.height = '40px';
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
  };

  const handleFileUpload = (file: File) => {
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      message.error('文件大小不能超过 10MB');
      return false;
    }

    const allowedTypes = [
      'text/plain',
      'text/markdown',
      'application/pdf',
      'image/jpeg',
      'image/png',
      'image/gif',
    ];

    if (!allowedTypes.includes(file.type)) {
      message.error('不支持的文件类型');
      return false;
    }

    setAttachedFiles(prev => [...prev, file]);
    return false; // Prevent auto upload
  };

  const removeFile = (index: number) => {
    setAttachedFiles(prev => prev.filter((_, i) => i !== index));
  };

  const isDisabled = state.isStreaming || state.isLoading;

  return (
    <div className={styles.inputContainer}>
      {attachedFiles.length > 0 && (
        <div style={{ marginBottom: 8 }}>
          {attachedFiles.map((file, index) => (
            <div key={index} className={styles.fileAttachment}>
              <span>{file.name} ({(file.size / 1024).toFixed(1)}KB)</span>
              <Button
                type="text"
                size="small"
                onClick={() => removeFile(index)}
                style={{ marginLeft: 8, padding: 0 }}
              >
                ×
              </Button>
            </div>
          ))}
        </div>
      )}

      <textarea
        ref={textareaRef}
        className={styles.messageInput}
        value={inputValue}
        onChange={handleInputChange}
        onKeyPress={handleKeyPress}
        placeholder={isDisabled ? "AI 正在思考中..." : "输入消息... (Enter 发送，Shift+Enter 换行)"}
        disabled={isDisabled}
        rows={1}
      />

      <div className={styles.inputActions}>
        <Upload
          beforeUpload={handleFileUpload}
          showUploadList={false}
          disabled={isDisabled}
        >
          <Button
            type="text"
            icon={<PaperClipOutlined />}
            size="small"
            disabled={isDisabled}
            title="上传文件"
          />
        </Upload>

        <Button
          type="primary"
          icon={state.isStreaming ? <LoadingOutlined /> : <SendOutlined />}
          size="small"
          onClick={handleSend}
          disabled={isDisabled || (!inputValue.trim() && attachedFiles.length === 0)}
          loading={state.isStreaming}
        >
          发送
        </Button>
      </div>
    </div>
  );
};

export default MessageInput;
