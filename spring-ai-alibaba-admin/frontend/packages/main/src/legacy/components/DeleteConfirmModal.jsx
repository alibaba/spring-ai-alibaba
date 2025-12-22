import React, { useState } from 'react';
import {
  Modal,
  Input,
  Button,
  Typography,
  Space,
  Alert,
  message
} from 'antd';
import {
  ExclamationCircleOutlined,
  CloseOutlined
} from '@ant-design/icons';

const { Text, Paragraph } = Typography;

const DeleteConfirmModal = ({ prompt, onConfirm, onClose }) => {
  const [confirmName, setConfirmName] = useState('');

  const handleConfirm = () => {
    const promptName = prompt.promptKey || prompt.name;
    if (confirmName === promptName) {
      onConfirm();
    } else {
      message.error('输入的名称不匹配，请重新输入');
    }
  };

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            width: 40,
            height: 40,
            backgroundColor: '#fff2f0',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <ExclamationCircleOutlined style={{ color: '#ff4d4f', fontSize: 20 }} />
          </div>
          <Text strong style={{ fontSize: 18 }}>确认删除</Text>
        </div>
      }
      open={true}
      onCancel={onClose}
      width={480}
      centered
      footer={[
        <Button key="cancel" onClick={onClose}>
          取消
        </Button>,
        <Button
          key="confirm"
          type="primary"
          danger
          disabled={confirmName !== (prompt.promptKey || prompt.name)}
          onClick={handleConfirm}
        >
          确认删除
        </Button>
      ]}
      closeIcon={<CloseOutlined />}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          message={
            <div>
              <Paragraph style={{ margin: 0, marginBottom: 8 }}>
                你确定要删除Prompt吗？请输入 <Text strong style={{ color: '#ff4d4f' }}>{prompt.promptKey || prompt.name}</Text> 确认删除操作。
              </Paragraph>
            </div>
          }
          type="warning"
          showIcon
        />
        
        <div>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>请输入Prompt名称确认：</Text>
          <Input
            value={confirmName}
            onChange={(e) => setConfirmName(e.target.value)}
            placeholder={`输入"${prompt.promptKey || prompt.name}"确认删除`}
            status={confirmName && confirmName !== (prompt.promptKey || prompt.name) ? 'error' : undefined}
            size="large"
          />
          {confirmName && confirmName !== (prompt.promptKey || prompt.name) && (
            <Text type="danger" style={{ fontSize: '12px', marginTop: 4, display: 'block' }}>
              输入的名称与 Prompt 名称不匹配
            </Text>
          )}
        </div>
      </Space>
    </Modal>
  );
};

export default DeleteConfirmModal;