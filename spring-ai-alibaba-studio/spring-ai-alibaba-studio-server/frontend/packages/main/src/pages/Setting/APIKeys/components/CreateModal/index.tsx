import $i18n from '@/i18n';
import { createApiKey } from '@/services/apiKey';
import { Form, Input, message, Modal } from '@spark-ai/design';
import React, { useState } from 'react';

interface CreateModalProps {
  open: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}

const CreateModal: React.FC<CreateModalProps> = ({
  open,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  // Submit API key creation form
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const res = await createApiKey(values);
      if (res && res.data) {
        message.success(
          $i18n.get({
            id: 'main.pages.Setting.APIKeys.components.CreateModal.index.createSuccess',
            dm: '创建成功',
          }),
        );
        form.resetFields();
        onSuccess();
      }
    } catch (error: any) {
      if (error.errorFields) {
        return;
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  return (
    <Modal
      title={$i18n.get({
        id: 'main.pages.Setting.APIKeys.components.CreateModal.index.createApiKey',
        dm: '创建API KEY',
      })}
      open={open}
      onCancel={handleCancel}
      onOk={handleSubmit}
      confirmLoading={loading}
      maskClosable={false}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="description"
          label={$i18n.get({
            id: 'main.pages.Setting.APIKeys.components.CreateModal.index.description',
            dm: '描述',
          })}
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.Setting.APIKeys.components.CreateModal.index.enterDescription',
                dm: '请输入API KEY描述',
              }),
            },
          ]}
        >
          <Input.TextArea
            placeholder={$i18n.get({
              id: 'main.pages.Setting.APIKeys.components.CreateModal.index.enterDescription',
              dm: '请输入API KEY描述',
            })}
            rows={4}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CreateModal;
