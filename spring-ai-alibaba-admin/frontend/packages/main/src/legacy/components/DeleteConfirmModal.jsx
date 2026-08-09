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
import $i18n from '@/i18n';
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
      message.error($i18n.get({ id: 'legacy.prompts.the.entered.name.does.not.match.please.try.again', dm: '输入的名称不匹配，请重新输入' }));
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
          <Text strong style={{ fontSize: 18 }}>{$i18n.get({ id: 'legacy.prompts.confirm.deletion', dm: '确认删除' })}</Text>
        </div>
      }
      open={true}
      onCancel={onClose}
      width={480}
      centered
      footer={[
        <Button key="cancel" onClick={onClose}>
          {$i18n.get({ id: 'legacy.prompts.cancel', dm: '取消' })}
        </Button>,
        <Button
          key="confirm"
          type="primary"
          danger
          disabled={confirmName !== (prompt.promptKey || prompt.name)}
          onClick={handleConfirm}
        >
          {$i18n.get({ id: 'legacy.prompts.delete', dm: '确认删除' })}
        </Button>
      ]}
      closeIcon={<CloseOutlined />}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          message={
            <div>
              <Paragraph style={{ margin: 0, marginBottom: 8 }}>
                {$i18n.get({ id: 'legacy.prompts.delete.confirm.prefix', dm: '你确定要删除Prompt吗？请输入 ' })}
                <Text strong style={{ color: '#ff4d4f' }}>{prompt.promptKey || prompt.name}</Text>
                {$i18n.get({ id: 'legacy.prompts.delete.confirm.suffix', dm: ' 确认删除操作。' })}
              </Paragraph>
            </div>
          }
          type="warning"
          showIcon
        />
        
        <div>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>{$i18n.get({ id: 'legacy.prompts.enter.the.prompt.name.to.confirm', dm: '请输入Prompt名称确认：' })}</Text>
          <Input
            value={confirmName}
            onChange={(e) => setConfirmName(e.target.value)}
            placeholder={$i18n.get({ id: 'legacy.prompts.enter.to.confirm.deletion', dm: '输入"{promptKey}"确认删除' }, { promptKey: prompt.promptKey || prompt.name })}
            status={confirmName && confirmName !== (prompt.promptKey || prompt.name) ? 'error' : undefined}
            size="large"
          />
          {confirmName && confirmName !== (prompt.promptKey || prompt.name) && (
            <Text type="danger" style={{ fontSize: '12px', marginTop: 4, display: 'block' }}>
              {$i18n.get({ id: 'legacy.prompts.the.entered.name.does.not.match.the.prompt.name', dm: '输入的名称与 Prompt 名称不匹配' })}
            </Text>
          )}
        </div>
      </Space>
    </Modal>
  );
};

export default DeleteConfirmModal;