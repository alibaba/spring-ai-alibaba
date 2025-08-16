import $i18n from '@/i18n';
import { Form, Input, Modal, Radio } from '@spark-ai/design';
import { IVarTreeItem, VarInputTextArea } from '@spark-ai/flow';
import { useSetState } from 'ahooks';
import { message, Select } from 'antd';
import { useCallback } from 'react';
import { IApiNodeParam } from '../../types';
import styles from './index.module.less';

interface IProps {
  value: IApiNodeParam['authorization'];
  variableList: IVarTreeItem[];
  onClose: () => void;
  onOk: (value: IApiNodeParam['authorization']) => void;
}

export default function AuthConfigFormModal(props: IProps) {
  const [value, setValue] = useSetState<IApiNodeParam['authorization']>(
    props.value,
  );

  const changeAuthConfig = useCallback(
    (payload: Partial<IApiNodeParam['authorization']['auth_config']>) => {
      setValue({
        auth_config: {
          ...(value.auth_config || {}),
          ...payload,
        } as Record<string, string>,
      });
    },
    [value],
  );

  const handleOk = useCallback(() => {
    if (value.auth_type !== 'NoAuth' && !value.auth_config?.value) {
      message.error(
        $i18n.get({
          id: 'main.pages.App.Workflow.components.AuthConfigFormModal.index.enterToken',
          dm: '请输入TOKEN值',
        }),
      );
      return;
    }
    if (value.auth_type === 'ApiKeyAuth' && !value.auth_config?.key) {
      message.error(
        $i18n.get({
          id: 'main.pages.App.Workflow.components.AuthConfigFormModal.index.enterKey',
          dm: '请输入KEY',
        }),
      );
      return;
    }
    props.onOk(value);
    props.onClose();
  }, [value]);

  return (
    <Modal
      width={560}
      open
      onCancel={props.onClose}
      onOk={handleOk}
      title={$i18n.get({
        id: 'main.pages.App.Workflow.components.AuthConfigFormModal.index.auth',
        dm: '鉴权',
      })}
    >
      <Form className={styles.form} layout="vertical">
        <Form.Item
          required
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.App.Workflow.components.AuthConfigFormModal.index.selectAuthMethod',
                dm: '请选择鉴权方式',
              }),
            },
          ]}
          label={$i18n.get({
            id: 'main.pages.App.Workflow.components.AuthConfigFormModal.index.authMethod',
            dm: '鉴权方式',
          })}
        >
          <Radio.Group
            value={value.auth_type}
            onChange={(e) => {
              setValue({
                auth_type: e.target
                  .value as IApiNodeParam['authorization']['auth_type'],
                auth_config:
                  e.target.value === 'NoAuth'
                    ? void 0
                    : {
                        ...(value.auth_config || {}),
                        key: 'token',
                      },
              });
            }}
            options={[
              {
                label: 'none',
                value: 'NoAuth',
              },
              {
                label: 'Bearer Token',
                value: 'BearerAuth',
              },
              {
                label: 'Api Key',
                value: 'ApiKeyAuth',
              },
            ]}
          />
        </Form.Item>
        {value.auth_type === 'BearerAuth' && (
          <Form.Item label="Token">
            <VarInputTextArea
              value={value.auth_config?.value}
              onChange={(val) => changeAuthConfig({ value: val })}
              variableList={props.variableList}
            />
          </Form.Item>
        )}
        {value.auth_type === 'ApiKeyAuth' && (
          <>
            <Form.Item label="Key">
              <Input
                onChange={(e) => changeAuthConfig({ key: e.target.value })}
                value={value.auth_config?.key}
                placeholder={$i18n.get({
                  id: 'main.pages.App.Workflow.components.AuthConfigFormModal.index.enterKey',
                  dm: '请输入KEY',
                })}
              />
            </Form.Item>
            <Form.Item label="Value">
              <div className={styles.valueInput}>
                <VarInputTextArea
                  value={value.auth_config?.value}
                  onChange={(val) => changeAuthConfig({ value: val })}
                  variableList={props.variableList}
                />
              </div>
            </Form.Item>
            <Form.Item label="Add to">
              <Select
                placeholder={$i18n.get({
                  id: 'main.pages.App.Workflow.components.AuthConfigFormModal.index.select',
                  dm: '请选择',
                })}
                popupMatchSelectWidth={false}
                style={{ width: '100%' }}
                onChange={(val) => changeAuthConfig({ add_to: val })}
                value={value.auth_config?.add_to}
                options={[
                  { label: 'Header', value: 'Header' },
                  { label: 'Query', value: 'QueryParams' },
                ]}
              />
            </Form.Item>
          </>
        )}
      </Form>
    </Modal>
  );
}
