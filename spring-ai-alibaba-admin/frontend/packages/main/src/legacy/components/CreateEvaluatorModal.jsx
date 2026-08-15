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
import $i18n from '@/i18n';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

const CreateEvaluatorModal = ({ onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async () => {
    try {
      setLoading(true);
      setError(null);

      const values = await form.validateFields();

      // 调用创建评估器API
      const response = await API.createEvaluator({
        name: values.name,
        description: values.description || ''
      });

      if (response.code === 200) {
        notifySuccess({ 
          message: $i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.createdSuccess', dm: '评估器创建成功' }),
          description: $i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.createdDesc', dm: '评估器 "{name}" 已成功创建' }, { name: values.name })
        });
        form.resetFields();
        onSuccess?.(response.data);
        onClose();
      } else {
        throw new Error(response.message || $i18n.get({ id: 'legacy.evaluation.common.creationFailed', dm: '创建失败' }));
      }
    } catch (error) {
      console.error('Failed to create evaluator:', error);
      if (error.errorFields) {
        // 表单验证错误
        setError($i18n.get({ id: 'legacy.evaluation.common.checkFormPeriod', dm: '请检查表单填写是否正确' }));
      } else {
        handleApiError(error, $i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.createContext', dm: '创建评估器' }));
        setError(error.message || $i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.createFailedRetry', dm: '创建失败，请稍后重试' }));
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    setError(null);
    onClose();
  };

  return (
    <Modal
      title={
        <div className="flex items-center gap-3">
          <div className="flex items-center justify-center w-10 h-10 bg-green-100 rounded-lg">
            <PlusOutlined style={{ color: '#52c41a', fontSize: 20 }} />
          </div>
          <Title level={3} style={{ margin: 0 }}>
            {$i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.title', dm: '创建新评估器' })}
          </Title>
        </div>
      }
      open={true}
      onCancel={handleCancel}
      width={600}
      centered
      styles={{
        body: {
          maxHeight: '70vh',
          overflowY: 'auto',
          padding: '24px'
        }
      }}
      footer={[
        <Button key="cancel" onClick={handleCancel}>
          {$i18n.get({ id: 'legacy.evaluation.common.cancel', dm: '取消' })}
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={loading}
          onClick={handleSubmit}
          icon={<PlusOutlined />}
        >
          {loading
            ? $i18n.get({ id: 'legacy.evaluation.common.creating', dm: '创建中...' })
            : $i18n.get({ id: 'legacy.evaluation.common.createEvaluator', dm: '创建评估器' })}
        </Button>
      ]}
      closeIcon={<CloseOutlined />}
    >
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        {error && (
          <Alert
            message={$i18n.get({ id: 'legacy.evaluation.common.creationFailedTitle', dm: '创建失败' })}
            description={error}
            type="error"
            icon={<ExclamationCircleOutlined />}
            showIcon
            closable
            onClose={() => setError(null)}
          />
        )}

        <Form
          form={form}
          layout="vertical"
          requiredMark={false}
          style={{ width: '100%' }}
        >
          <Form.Item
            label={$i18n.get({ id: 'legacy.evaluation.common.evaluatorName', dm: '评估器名称' })}
            name="name"
            rules={[
              { required: true, message: $i18n.get({ id: 'legacy.evaluation.common.pleaseEnterEvaluatorName', dm: '请输入评估器名称' }) },
              { max: 50, message: $i18n.get({ id: 'legacy.evaluation.common.nameMax50', dm: '名称不能超过50个字符' }) },
              { 
                pattern: /^[a-zA-Z0-9\u4e00-\u9fa5_-]+$/, 
                message: $i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.namePattern', dm: '名称只能包含中英文、数字、下划线和横线' })
              }
            ]}
          >
            <Input
              placeholder={$i18n.get({ id: 'legacy.evaluation.common.enterEvaluatorName', dm: '输入评估器名称' })}
              size="large"
              showCount
              maxLength={50}
            />
          </Form.Item>

          <Form.Item
            label={$i18n.get({ id: 'legacy.evaluation.common.description', dm: '描述' })}
            name="description"
            rules={[
              { max: 500, message: $i18n.get({ id: 'legacy.evaluation.common.descMax500', dm: '描述不能超过500个字符' }) }
            ]}
          >
            <TextArea
              placeholder={$i18n.get({ id: 'legacy.evaluation.common.enterEvaluatorDescOptional', dm: '输入评估器描述（可选）' })}
              rows={4}
              showCount
              maxLength={500}
            />
          </Form.Item>
        </Form>

        {/* 提示信息 */}
        <Alert
          message={$i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.nextSteps', dm: '创建后的配置步骤' })}
          description={
            <div>
              <Paragraph style={{ margin: 0, marginBottom: 8 }}>
                {$i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.nextStepsDesc', dm: '创建评估器后，您可以在详情页面配置具体的版本信息，包括：' })}
              </Paragraph>
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                <li>{$i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.nextStep1', dm: '裁判模型选择（GPT-4、Claude等）' })}</li>
                <li>{$i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.nextStep2', dm: '评估Prompt内容' })}</li>
                <li>{$i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.nextStep3', dm: '模型参数配置' })}</li>
                <li>{$i18n.get({ id: 'legacy.evaluation.createEvaluatorModal.nextStep4', dm: '版本管理和发布' })}</li>
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
