import React from 'react';
import { Collapse, Tag, Typography } from 'antd';
import { UserOutlined, RobotOutlined, ToolOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { Message } from '../contexts/ChatContext';
import { useConfigContext } from '../contexts/ConfigContext';
import styles from '../index.module.less';

const { Panel } = Collapse;
const { Text } = Typography;

interface MessageListProps {
  messages: Message[];
}

const MessageList: React.FC<MessageListProps> = ({ messages }) => {
  const { config } = useConfigContext();

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const renderAttachments = (attachments?: File[]) => {
    if (!attachments || attachments.length === 0) return null;

    return (
      <div className={styles.fileAttachment}>
        {attachments.map((file, index) => (
          <Tag key={index} icon={<ToolOutlined />}>
            {file.name}
          </Tag>
        ))}
      </div>
    );
  };

  const renderToolCalls = (toolCalls?: any[]) => {
    if (!config.showToolCalls || !toolCalls || toolCalls.length === 0) return null;

    return (
      <div className={styles.messageToolCalls}>
        <Collapse size="small" ghost>
          <Panel header="🔧 工具调用详情" key="1">
            {toolCalls.map((call, index) => (
              <div key={index} style={{ marginBottom: 8 }}>
                <Text strong>函数: {call.name}</Text>
                <pre style={{ margin: '4px 0', fontSize: 11 }}>
                  参数: {JSON.stringify(call.arguments, null, 2)}
                </pre>
                {call.result && (
                  <pre style={{ margin: '4px 0', fontSize: 11 }}>
                    结果: {JSON.stringify(call.result, null, 2)}
                  </pre>
                )}
              </div>
            ))}
          </Panel>
        </Collapse>
      </div>
    );
  };

  const renderError = (error?: string) => {
    if (!error) return null;

    return (
      <div className={styles.messageError}>
        <ExclamationCircleOutlined style={{ marginRight: 4 }} />
        {error}
      </div>
    );
  };

  return (
    <div>
      {messages.map((message) => (
        <div
          key={message.id}
          className={`${styles.message} ${message.type === 'user' ? styles.user : ''}`}
        >
          <div className={styles.messageAvatar}>
            {message.type === 'user' ? (
              <UserOutlined />
            ) : (
              <RobotOutlined />
            )}
          </div>

          <div className={styles.messageContent}>
            <div className={styles.messageBubble}>
              <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {message.content}
              </div>
              {renderAttachments(message.attachments)}
            </div>

            <div className={styles.messageTime}>
              {formatTime(message.timestamp)}
            </div>

            {renderToolCalls(message.toolCalls)}
            {renderError(message.error)}
          </div>
        </div>
      ))}
    </div>
  );
};

export default MessageList;
