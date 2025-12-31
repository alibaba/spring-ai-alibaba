import React, { useState } from 'react';
import {
  Modal,
  Card,
  Typography,
  Button,
  Input,
  Alert,
  Space,
  Form,
  message
} from 'antd';
import {
  CloseOutlined,
  PlusOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined
} from '@ant-design/icons';
import { notifyError, notifySuccess, handleApiError } from '../utils/notification';
import API from '../services';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

const CreateEvaluatorModal = ({ onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      setError(null);

      const response = await API.createEvaluator({
        name: values.name,
        description: values.description || ''
      });

      if (response.code === 200) {
        notifySuccess({ 
          message: '评估器创建成功',
          description: `评估器 "${values.name}" 已成功创建`
        });
        form.resetFields();
        onSuccess?.(response.data);
        onClose();
      } else {
        throw new Error(response.message || '创建失败');
      }
    } catch (error) {
      console.error('创建评估器失败:', error);
      if (error.errorFields) {
        // 表单验证错误
        setError('请检查表单填写是否正确');
      } else {
        handleApiError(error, '创建评估器');
        setError(error.message || '创建失败，请稍后重试');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            width: 40,
            height: 40,
            backgroundColor: '#f6ffed',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <PlusOutlined style={{ color: '#52c41a', fontSize: 20 }} />
          </div>
          <Title level={3} style={{ margin: 0 }}>
            创建新评估器
          </Title>
        </div>
      }
      open={true}
      onCancel={handleCancel}
      width={600}
      centered
      style={{
        maxHeight: 'calc(100vh - 40px)'
      }}
      bodyStyle={{
        maxHeight: 'calc(100vh - 200px)',
        overflowY: 'auto'
      }}
      footer={[
        <Button key="cancel" onClick={handleCancel}>
          取消
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={loading}
          onClick={handleSubmit}
          icon={<PlusOutlined />}
        >
          {loading ? '创建中...' : '创建评估器'}
        </Button>
      ]}
      closeIcon={<CloseOutlined />}
    >
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        {error && (
          <Alert
            message="创建失败"
            description={error}
            type="error"
            icon={<ExclamationCircleOutlined />}
            showIcon
          />
        )}

        <Form
          form={form}
          layout="vertical"
          requiredMark="optional"
          style={{ width: '100%' }}
        >
          <Form.Item
            label="评估器名称"
            name="name"
            rules={[
              { required: true, message: '请输入评估器名称' },
              { max: 50, message: '名称不能超过50个字符' },
              { 
                pattern: /^[a-zA-Z0-9\u4e00-\u9fa5_-]+$/, 
                message: '名称只能包含中英文、数字、下划线和横线' 
              }
            ]}
          >
            <Input
              placeholder="输入评估器名称"
              size="large"
              showCount
              maxLength={50}
            />
          </Form.Item>

          <Form.Item
            label="描述"
            name="description"
            rules={[
              { max: 500, message: '描述不能超过500个字符' }
            ]}
          >
            <TextArea
              placeholder="输入评估器描述（可选）"
              rows={4}
              showCount
              maxLength={500}
            />
          </Form.Item>
        </Form>

        {/* 提示信息 */}
        <Alert
          message="创建后的配置步骤"
          description={
            <div>
              <Paragraph style={{ margin: 0, marginBottom: 8 }}>
                创建评估器后，您可以在详情页面配置具体的版本信息，包括：
              </Paragraph>
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                <li>裁判模型选择（GPT-4、Claude等）</li>
                <li>评估Prompt内容</li>
                <li>模型参数配置</li>
                <li>版本管理和发布</li>
              </ul>
            </div>
          }
          type="info"
          icon={<InfoCircleOutlined />}
          showIcon
        />
      </Space>
    </Modal>
  );
};

export default CreateEvaluatorModal;