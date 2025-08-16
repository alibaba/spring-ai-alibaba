import TipBox from '@/components/TipBox';
import $i18n from '@/i18n';
import { API_KEY_TIP_SECTIONS } from '@/pages/Setting/utils';
import { updateProvider } from '@/services/modelService';
import type { IProviderConfigInfo } from '@/types/modelService';
import { AlertDialog, Button, Form, Input, message } from '@spark-ai/design';
import React, { useEffect } from 'react';
import { ProviderAvatar } from '../ProviderAvatar';
import styles from './index.module.less';
interface ProviderInfoFormProps {
  provider: IProviderConfigInfo | null;
  providerId: string;
  onRefresh?: () => void;
}

interface ProviderNameProps {
  provider: IProviderConfigInfo | null;
  onEnableService: (enable: boolean) => void;
}

const ProviderName: React.FC<ProviderNameProps> = ({
  provider,
  onEnableService,
}) => {
  return (
    <div className={styles.label}>
      <div className={styles.labelContent}>
        {$i18n.get({
          id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.modelServiceProviderName',
          dm: '模型服务商名称',
        })}

        <div className={styles.required}>*</div>
      </div>
      <div className={styles.actions}>
        <div
          className={styles['status-tag']}
          data-status={provider?.enable ? 'enabled' : 'disabled'}
        >
          <span className={styles.dot}></span>
          <span>
            {provider?.enable
              ? $i18n.get({
                  id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.started',
                  dm: '已启动',
                })
              : $i18n.get({
                  id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.stopped',
                  dm: '已停用',
                })}
          </span>
        </div>
        <Button onClick={() => onEnableService(!provider?.enable)}>
          {provider?.enable
            ? $i18n.get({
                id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.stopService',
                dm: '停止服务',
              })
            : $i18n.get({
                id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.startService',
                dm: '启动服务',
              })}
        </Button>
      </div>
    </div>
  );
};

const ProviderInfoForm: React.FC<ProviderInfoFormProps> = ({
  provider,
  providerId,
  onRefresh,
}) => {
  const [form] = Form.useForm();
  const isPreset = provider?.source === 'preset';

  useEffect(() => {
    initForm();
  }, [provider, form]);

  const initForm = () => {
    if (!provider) return;
    form.setFieldsValue({
      name: provider?.name,
      apiKey: provider?.credential?.api_key || '',
      endpoint: provider?.credential?.endpoint || '',
    });
  };

  const handleFormSubmit = () => {
    form.validateFields().then((values) => {
      if (!providerId || !provider) return;

      updateProvider(providerId, {
        ...provider,
        name: values.name,
        credential_config: {
          api_key: values.apiKey,
          endpoint: values.endpoint,
        },
      }).then((response) => {
        if (response) {
          message.success(
            $i18n.get({
              id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.configurationUpdated',
              dm: '配置已更新',
            }),
          );
          onRefresh?.();
        }
      });
    });
  };

  const enableService = async (enable: boolean) => {
    if (!provider) return;

    await form.validateFields();

    AlertDialog.warning({
      title: enable
        ? $i18n.get({
            id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.startService',
            dm: '启动服务',
          })
        : $i18n.get({
            id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.stopService',
            dm: '停止服务',
          }),
      children: enable
        ? $i18n.get({
            id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.confirmStartService',
            dm: '确定要启动当前服务吗？',
          })
        : $i18n.get({
            id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.confirmStopService',
            dm: '确定要停止当前服务吗？停止后将无法使用该服务的模型。',
          }),

      onOk: () => {
        updateProvider(providerId, {
          ...provider,
          enable,
          credential_config: provider.credential,
        }).then((response) => {
          if (response) {
            message.success(
              $i18n.get({
                id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.stopServiceSuccess',
                dm: '停止服务成功',
              }),
            );
            onRefresh?.();
          }
        });
      },
    });
  };

  return (
    <div className={styles.wrapper}>
      <div className={styles.content}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFormSubmit}
          className={styles.form}
        >
          <div className={styles['form-row']}>
            <ProviderAvatar
              className={styles['provider-avatar']}
              provider={provider}
            />

            <Form.Item
              name="name"
              label={
                <ProviderName
                  provider={provider}
                  onEnableService={enableService}
                />
              }
              rules={[
                {
                  required: true,
                  message: $i18n.get({
                    id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.enterModelServiceProviderName',
                    dm: '请输入模型服务商名称',
                  }),
                },
              ]}
              required={false}
              className={styles['form-item']}
            >
              <Input
                placeholder={$i18n.get({
                  id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.inputModelServiceProviderName',
                  dm: '输入模型服务商名称',
                })}
                maxLength={30}
                disabled={isPreset}
              />
            </Form.Item>
          </div>
          <Form.Item
            name="apiKey"
            label="API-KEY"
            rules={[
              {
                required: true,
                message: $i18n.get({
                  id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.apiKeyRequired',
                  dm: '需要填写API-KEY验证凭证才能调用远程模型服务',
                }),
              },
            ]}
            required
          >
            <Input.Password
              placeholder={$i18n.get({
                id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.enterApiKey',
                dm: '输入您的API-KEY',
              })}
            />
          </Form.Item>

          <Form.Item
            name="endpoint"
            label="API URL"
            rules={[
              {
                required: true,
                message: $i18n.get({
                  id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.apiUrlRequired',
                  dm: '需要填写API URL才能调用远程模型服务',
                }),
              },
            ]}
            required={true}
          >
            <Input.TextArea
              rows={2}
              placeholder={$i18n.get({
                id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.enterApiUrl',
                dm: '输入您的API URL，请在服务商文档中获取，如https://dashscope.aliyuncs.com/compatible-mode',
              })}
            />
          </Form.Item>
          <Form.Item>
            <TipBox
              title={$i18n.get({
                id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.howToGetModelServiceApi',
                dm: '如何获取模型服务API？',
              })}
              sections={API_KEY_TIP_SECTIONS}
            />
          </Form.Item>
          <Form.Item>
            <div className={styles.actions}>
              <Button type="primary" htmlType="submit">
                {$i18n.get({
                  id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.save',
                  dm: '保存',
                })}
              </Button>
              <Button htmlType="button" onClick={initForm}>
                {$i18n.get({
                  id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.cancel',
                  dm: '取消',
                })}
              </Button>
            </div>
          </Form.Item>
        </Form>
      </div>
    </div>
  );
};

export default ProviderInfoForm;
