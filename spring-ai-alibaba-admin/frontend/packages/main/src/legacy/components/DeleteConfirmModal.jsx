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
      message.error('The entered name does not match. Please try again.');
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
          <Text strong style={{ fontSize: 18 }}>Confirm Deletion</Text>
        </div>
      }
      open={true}
      onCancel={onClose}
      width={480}
      centered
      footer={[
        <Button key="cancel" onClick={onClose}>
          Cancel
        </Button>,
        <Button
          key="confirm"
          type="primary"
          danger
          disabled={confirmName !== (prompt.promptKey || prompt.name)}
          onClick={handleConfirm}
        >
          Delete
        </Button>
      ]}
      closeIcon={<CloseOutlined />}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          message={
            <div>
              <Paragraph style={{ margin: 0, marginBottom: 8 }}>
                Are you sure you want to delete this prompt? Enter <Text strong style={{ color: '#ff4d4f' }}>{prompt.promptKey || prompt.name}</Text> to confirm.
              </Paragraph>
            </div>
          }
          type="warning"
          showIcon
        />
        
        <div>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>Enter the prompt name to confirm:</Text>
          <Input
            value={confirmName}
            onChange={(e) => setConfirmName(e.target.value)}
            placeholder={`Enter "${prompt.promptKey || prompt.name}" to confirm deletion`}
            status={confirmName && confirmName !== (prompt.promptKey || prompt.name) ? 'error' : undefined}
            size="large"
          />
          {confirmName && confirmName !== (prompt.promptKey || prompt.name) && (
            <Text type="danger" style={{ fontSize: '12px', marginTop: 4, display: 'block' }}>
              The entered name does not match the prompt name.
            </Text>
          )}
        </div>
      </Space>
    </Modal>
  );
};

export default DeleteConfirmModal;