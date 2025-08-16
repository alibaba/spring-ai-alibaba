import TipBox from '@/components/TipBox';
import $i18n from '@/i18n';
import { API_KEY_TIP_SECTIONS } from '@/pages/Setting/utils';
import { createProvider, getProviderProtocols } from '@/services/modelService';
import type { ICreateProviderParams } from '@/types/modelService';
import { Button, Form, Input, Modal, Radio, message } from '@spark-ai/design';
import { Space } from 'antd';
import React, { useEffect, useState } from 'react';
import styles from './index.module.less';

interface ModelServiceProviderModalProps {
  open: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}

const ModelServiceProviderModal: React.FC<ModelServiceProviderModalProps> = ({
  open,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [protocols, setProtocols] = useState<string[]>([]);

  useEffect(() => {
    getProviderProtocols().then((res) => {
      setProtocols(res.data);
      form.setFieldsValue({
        protocol: res.data[0],
      });
    });
  }, []);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const params: ICreateProviderParams = {
        name: values.name,
        protocol: values.protocol,
        credential_config: {
          api_key: values.api_key,
          endpoint: values.endpoint,
        },
      };

      const res = await createProvider(params);

      if (res) {
        message.success(
          $i18n.get({
            id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.createSuccess',
            dm: '模型服务商创建成功',
          }),
        );
        form.resetFields();
        onSuccess();
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={$i18n.get({
        id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.addServiceProvider',
        dm: '新增模型服务商',
      })}
      open={open}
      onCancel={() => {
        form.resetFields();
        onCancel();
      }}
      footer={
        <div className={styles['form-footer']}>
          <Button
            onClick={() => {
              form.resetFields();
              onCancel();
            }}
          >
            {$i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.cancel',
              dm: '取消',
            })}
          </Button>
          <Button type="primary" loading={loading} onClick={handleSubmit}>
            {$i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.confirm',
              dm: '确认',
            })}
          </Button>
        </div>
      }
      width={640}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        requiredMark={false}
        className={styles['provider-modal']}
      >
        <Form.Item
          name="name"
          label={$i18n.get({
            id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.serviceProviderName',
            dm: '服务商名称',
          })}
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.enterServiceProviderName',
                dm: '请输入服务商名称',
              }),
            },
          ]}
        >
          <Input
            placeholder={$i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.enterYourServiceProviderName',
              dm: '输入您的服务商名称',
            })}
            maxLength={15}
            showCount
          />
        </Form.Item>

        <Form.Item
          name="api_key"
          label="API-KEY"
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.enterApiKey',
                dm: '请输入API-KEY',
              }),
            },
          ]}
          required
        >
          <Input.Password
            placeholder={$i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.enterYourApiKey',
              dm: '输入您的API-KEY',
            })}
            maxLength={100}
          />
        </Form.Item>

        <Form.Item
          name="endpoint"
          label="API URL"
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.enterApiUrl',
                dm: '请输入API URL',
              }),
            },
            {
              type: 'url',
              message: $i18n.get({
                id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.enterValidUrl',
                dm: '请输入有效的URL地址',
              }),
            },
          ]}
        >
          <Input.TextArea
            rows={2}
            placeholder={$i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.enterYourApiUrl',
              dm: '输入您的API URL，请在服务商文档获取，如 https://dashscope.aliyuncs.com/compatible-mode',
            })}
          />
        </Form.Item>

        <Form.Item
          name="protocol"
          label={$i18n.get({
            id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.serviceProviderType',
            dm: '服务商类型',
          })}
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.selectServiceProviderType',
                dm: '请选择服务商类型',
              }),
            },
          ]}
        >
          <Radio.Group className={styles['protocol-type-radio']}>
            <Space>
              {protocols.map((protocol) => (
                <Radio key={protocol} value={protocol}>
                  {protocol}
                </Radio>
              ))}
            </Space>
            <TipBox
              title={$i18n.get({
                id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.howToGetModelServiceApi',
                dm: '如何获取模型服务API？',
              })}
              sections={API_KEY_TIP_SECTIONS}
            />
          </Radio.Group>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ModelServiceProviderModal;
