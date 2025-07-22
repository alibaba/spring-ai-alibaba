import InnerLayout from '@/components/InnerLayout';
import { useInnerLayout } from '@/components/InnerLayout/utils';
import $i18n from '@/i18n';
import { createPlugin, savePlugin } from '@/services/plugin';
import { Plugin } from '@/types/plugin';
import { AlertDialog, Button, Form, Input } from '@spark-ai/design';
import { Flex, Select } from 'antd';
import { useEffect } from 'react';
import { history } from 'umi';
import HeadersEditForm from '../components/HeadersEditForm';
import styles from './index.module.less';

interface IProps {
  pluginData?: Plugin;
  isCreate?: boolean;
}

export default function (props: IProps) {
  const portal = useInnerLayout();
  const [form] = Form.useForm();
  const authType = Form.useWatch(['config', 'auth', 'type'], form);

  useEffect(() => {
    if (props.pluginData) {
      if (props.pluginData.config?.headers) {
        props.pluginData.config.headers = headersConvert.toArray(
          props.pluginData.config.headers as Record<string, string>,
        );
      }
      form.setFieldsValue(props.pluginData);
    }
    return () => {
      form.resetFields();
    };
  }, []);

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.Component.Plugin.Info.index.componentManagement',
            dm: '组件管理',
          }),
          path: '/component',
        },
        {
          title: props.isCreate
            ? $i18n.get({
                id: 'main.pages.Component.Plugin.Info.index.createCustomPlugin',
                dm: '创建自定义插件',
              })
            : $i18n.get({
                id: 'main.pages.Component.Plugin.Info.index.editCustomPlugin',
                dm: '编辑自定义插件',
              }),
        },
      ]}
    >
      <div className={styles.container}>
        <Form
          form={form}
          labelCol={{ span: 24 }}
          wrapperCol={{ span: 24 }}
          colon={false}
          layout="vertical"
        >
          <div className={styles.title}>
            {$i18n.get({
              id: 'main.pages.Component.Plugin.Info.index.pluginInfo',
              dm: '插件信息',
            })}
          </div>
          <Form.Item
            name="name"
            rules={[
              {
                required: true,
                message: $i18n.get({
                  id: 'main.pages.Component.Plugin.Info.index.enterPluginName',
                  dm: '请输入插件名称',
                }),
              },
            ]}
            label={$i18n.get({
              id: 'main.pages.Component.Plugin.Info.index.pluginName',
              dm: '插件名称',
            })}
          >
            <Input
              placeholder={$i18n.get({
                id: 'main.pages.Component.Plugin.Info.index.enterPluginName',
                dm: '请输入插件名称',
              })}
              showCount
              maxLength={128}
            />
          </Form.Item>

          <Form.Item
            name="description"
            rules={[
              {
                required: true,
                message: $i18n.get({
                  id: 'main.pages.Component.Plugin.Info.index.enterPluginDescription',
                  dm: '请输入插件描述，帮助用户更好的理解插件功能和使用场景',
                }),
              },
            ]}
            label={$i18n.get({
              id: 'main.pages.Component.Plugin.Info.index.pluginDescription',
              dm: '插件描述',
            })}
          >
            <Input.TextArea
              autoSize={{
                minRows: 4,
                maxRows: 4,
              }}
              showCount
              maxLength={200}
              placeholder={$i18n.get({
                id: 'main.pages.Component.Plugin.Info.index.enterPluginDescription',
                dm: '请输入插件描述，帮助用户更好的理解插件功能和使用场景',
              })}
            />
          </Form.Item>

          <Form.Item
            rules={[
              {
                required: true,
                message: $i18n.get({
                  id: 'main.pages.Component.Plugin.Info.index.enterPluginUrl',
                  dm: '请输入插件URL',
                }),
              },
              {
                pattern: /^https?:\/\/[^\s]+$/,
                message: $i18n.get({
                  id: 'main.pages.Component.Plugin.Info.index.enterValidPluginUrl',
                  dm: '请输入合法的插件URL地址',
                }),
              },
            ]}
            name={['config', 'server']}
            label={$i18n.get({
              id: 'main.pages.Component.Plugin.Info.index.pluginUrl',
              dm: '插件URL',
            })}
          >
            <Input
              placeholder={$i18n.get({
                id: 'main.pages.Component.Plugin.Info.index.enterPluginUrl',
                dm: '请输入插件URL',
              })}
            />
          </Form.Item>

          <Form.Item
            label={$i18n.get({
              id: 'main.pages.Component.Plugin.Info.index.headerList',
              dm: 'Header列表',
            })}
          >
            <HeadersEditForm />
          </Form.Item>

          <Form.Item
            label={$i18n.get({
              id: 'main.pages.Component.Plugin.Info.index.isAuthentication',
              dm: '是否鉴权',
            })}
            name={['config', 'auth', 'type']}
          >
            <Select
              options={['api_key', 'none'].map((item) => ({
                value: item,
                label: item,
              }))}
              onChange={() => {
                form.setFieldsValue({
                  config: {
                    auth: {
                      authorization_type: undefined,
                      authorization_position: undefined,
                      authorization_key: undefined,
                      authorization_value: undefined,
                    },
                  },
                });
              }}
            />
          </Form.Item>

          {authType !== 'none' && (
            <>
              <Form.Item
                label={$i18n.get({
                  id: 'main.pages.Component.Plugin.Info.index.authenticationType',
                  dm: '认证类型',
                })}
                name={['config', 'auth', 'authorization_type']}
                rules={[
                  {
                    required: authType !== 'none',
                    message: $i18n.get({
                      id: 'main.pages.Component.Plugin.Info.index.selectAuthenticationType',
                      dm: '请选择认证类型',
                    }),
                  },
                ]}
              >
                <Select
                  options={['basic', 'bearer', 'custom'].map((item) => ({
                    value: item,
                    label: item,
                  }))}
                />
              </Form.Item>

              <Form.Item
                label={$i18n.get({
                  id: 'main.pages.Component.Plugin.Info.index.authenticationLocation',
                  dm: '认证位置',
                })}
                name={['config', 'auth', 'authorization_position']}
                rules={[
                  {
                    required: authType !== 'none',
                    message: $i18n.get({
                      id: 'main.pages.Component.Plugin.Info.index.selectAuthenticationLocation',
                      dm: '请选择认证位置',
                    }),
                  },
                ]}
              >
                <Select
                  options={['header', 'query'].map((item) => ({
                    value: item,
                    label: item,
                  }))}
                />
              </Form.Item>

              <Form.Item
                label={$i18n.get({
                  id: 'main.pages.Component.Plugin.Info.index.authenticationParameterName',
                  dm: '认证参数名',
                })}
                name={['config', 'auth', 'authorization_key']}
                rules={[
                  {
                    required: authType !== 'none',
                    message: $i18n.get({
                      id: 'main.pages.Component.Plugin.Info.index.enterAuthenticationParameterName',
                      dm: '请输入认证参数名',
                    }),
                  },
                ]}
              >
                <Input />
              </Form.Item>

              <Form.Item
                label={$i18n.get({
                  id: 'main.pages.Component.Plugin.Info.index.authenticationParameterValue',
                  dm: '认证参数值',
                })}
                name={['config', 'auth', 'authorization_value']}
                rules={[
                  {
                    required: authType !== 'none',
                    message: $i18n.get({
                      id: 'main.pages.Component.Plugin.Info.index.enterAuthenticationParameterValue',
                      dm: '请输入认证参数值',
                    }),
                  },
                ]}
              >
                <Input />
              </Form.Item>
            </>
          )}
        </Form>
      </div>

      {portal.bottomPortal(
        <Flex className={styles['bottom-bar']} gap={8}>
          <Button
            type="primary"
            onClick={async () => {
              const v = await form.validateFields();
              if (v.config.headers) {
                v.config.headers = headersConvert.toObject(v.config.headers);
              }
              savePlugin({ ...v, plugin_id: props.pluginData?.plugin_id }).then(
                () => {
                  history.replace(`/component/plugin`);
                },
              );
            }}
          >
            {props.isCreate
              ? $i18n.get({
                  id: 'main.pages.Component.Plugin.Info.index.confirmCreate',
                  dm: '确认创建',
                })
              : $i18n.get({
                  id: 'main.pages.Component.Plugin.Info.index.save',
                  dm: '保存',
                })}
          </Button>
          {props.isCreate ? (
            <Button
              onClick={async () => {
                const v = await form.validateFields();
                if (v.config.headers) {
                  v.config.headers = headersConvert.toObject(v.config.headers);
                }
                createPlugin(v).then((res) => {
                  history.replace(`/component/plugin/${res.data}/tool/create`);
                });
              }}
            >
              {$i18n.get({
                id: 'main.pages.Component.Plugin.Info.index.continueAddTool',
                dm: '继续添加工具',
              })}
            </Button>
          ) : null}

          <Button
            onClick={() => {
              const text = props.isCreate
                ? $i18n.get({
                    id: 'main.pages.Component.Plugin.Info.index.confirmExitCustomPluginCreate',
                    dm: '创建',
                  })
                : $i18n.get({
                    id: 'main.pages.Component.Plugin.Info.index.confirmExitCustomPluginEdit',
                    dm: '编辑',
                  });
              AlertDialog.warning({
                title: $i18n.get(
                  {
                    id: 'main.pages.Component.Plugin.Info.index.confirmExitCustomPlugin',
                    dm: '是否退出{var1}自定义插件',
                  },
                  { var1: text },
                ),
                children: $i18n.get({
                  id: 'main.pages.Component.Plugin.Info.index.exitWithoutSave',
                  dm: '退出后，插件信息不会保存，是否确认？',
                }),
                onOk() {
                  history.push('/component/plugin');
                },
              });
            }}
          >
            {$i18n.get({
              id: 'main.pages.Component.Plugin.Info.index.cancel',
              dm: '取消',
            })}
          </Button>
        </Flex>,
      )}
    </InnerLayout>
  );
}

const headersConvert = {
  toArray(object: Record<string, string>) {
    return Object.keys(object).map((key) => ({
      name: key,
      value: object[key],
    }));
  },
  toObject(array: { name: string; value: string }[]) {
    return array.reduce((p, c) => {
      return {
        ...p,
        [c.name]: c.value,
      };
    }, {});
  },
};
