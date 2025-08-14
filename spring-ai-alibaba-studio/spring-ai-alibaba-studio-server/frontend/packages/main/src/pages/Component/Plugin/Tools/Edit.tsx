import InnerLayout from '@/components/InnerLayout';
import { useInnerLayout } from '@/components/InnerLayout/utils';
import ScrollMenu from '@/components/ScrollMenu';
import $i18n from '@/i18n';
import { getPlugin, getTool, publishTool, saveTool } from '@/services/plugin';
import { Button, Form, message } from '@spark-ai/design';
import { useRequest, useSetState } from 'ahooks';
import { Divider, Flex, Input, Select } from 'antd';
import classNames from 'classnames';
import { useRef, useState } from 'react';
import { history, useNavigate, useParams } from 'umi';
import ExampleConfigForm, {
  IExampleItem,
} from '../components/ExampleConfigForm';
import InputParamsConfig, {
  InputParamItem,
} from '../components/InputParamsConfig';
import OutputParamsConfig, {
  IOutputParamItem,
} from '../components/OutputParamsConfig';
import styles from './index.module.less';
import Test from './Test';
export default function () {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const menuRef = useRef<HTMLDivElement[]>([]);

  const [state, setState] = useSetState({
    inputParams: [] as InputParamItem[],
    outputParams: [] as IOutputParamItem[],
    examples: [] as Array<IExampleItem>,
  });
  const [activeIndex, setActiveIndex] = useState(0);

  const { id = '', toolId } = useParams<{ id: string; toolId?: string }>();
  const { data } = useRequest(() => getPlugin(id));
  useRequest(() => getTool(id, toolId || ''), {
    onSuccess(data) {
      if (data?.data) {
        form.setFieldsValue(data.data);
        setState({
          // @ts-ignore
          inputParams: data.data.config?.input_params || [],
          // @ts-ignore
          outputParams: data.data.config?.output_params || [],
          examples: data.data.config?.examples || [],
        });
      }
    },
  });

  const { bottomPortal } = useInnerLayout();
  const requestMethod = Form.useWatch(['config', 'request_method'], form);

  const save = async function () {
    const values = await form.validateFields();
    const { inputParams, outputParams, examples } = state;

    const requestData = {
      tool_id: toolId,
      plugin_id: id,
      ...values,
      config: {
        ...values.config,
        input_params: inputParams,
        output_params: outputParams,
        examples: (examples || []).map((item) => {
          return {
            ...item,
            path: values.config.path,
          };
        }),
      },
    };

    return saveTool(requestData).then((res) => {
      if (!toolId) {
        history.replace(`/component/plugin/${id}/tool/${res}`);
      }
      return res;
    });
  };

  const handleMenuClick = (index: number) => {
    setActiveIndex(index);

    menuRef.current[index]?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    });
  };

  const saveButton = (
    <Button
      onClick={async () => {
        save().then(() => {
          message.success(
            $i18n.get({
              id: 'main.pages.Component.Plugin.Tools.Edit.successSave',
              dm: '保存成功',
            }),
          );
        });
      }}
    >
      {$i18n.get({
        id: 'main.pages.Component.Plugin.Tools.Edit.save',
        dm: '保存',
      })}
    </Button>
  );

  const publishButton = (
    <Button
      type="primary"
      onClick={async () => {
        const toolId = (await save()) as string;
        publishTool(id, toolId).then(() => {
          message.success(
            $i18n.get({
              id: 'main.pages.Component.Plugin.Tools.Edit.successPublish',
              dm: '发布成功',
            }),
          );
        });
      }}
    >
      {$i18n.get({
        id: 'main.pages.Component.Plugin.Tools.Edit.saveAndPublish',
        dm: '保存并发布',
      })}
    </Button>
  );

  const cancelButton = (
    <Button
      onClick={() => {
        navigate('/component/plugin');
      }}
    >
      {$i18n.get({
        id: 'main.pages.Component.Plugin.Tools.Edit.cancel',
        dm: '取消',
      })}
    </Button>
  );

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.Component.Plugin.Tools.Edit.componentManagement',
            dm: '组件管理',
          }),
        },
        {
          title: data?.data.name,
          path: `/component/plugin/${id}/tools`,
        },
        {
          title: $i18n.get({
            id: 'main.pages.Component.Plugin.Tools.Edit.editTool',
            dm: '编辑工具',
          }),
        },
      ]}
      simplifyBreadcrumb
    >
      <div className={styles.form}>
        <div className={styles['form-content']}>
          <Form
            id="content-scroll"
            form={form}
            labelCol={{ span: 14 }}
            wrapperCol={{ span: 14 }}
            layout="vertical"
            colon={false}
          >
            <Flex vertical>
              <div
                ref={(el: HTMLDivElement) => {
                  menuRef.current[0] = el;
                }}
                className={styles['form-area']}
              >
                <div className={styles.title}>
                  {$i18n.get({
                    id: 'main.pages.Component.Plugin.Tools.Edit.toolInfo',
                    dm: '工具信息',
                  })}
                </div>
                <Form.Item
                  name="name"
                  rules={[
                    {
                      required: true,
                      message: $i18n.get({
                        id: 'main.pages.Component.Plugin.Tools.Edit.enterToolName',
                        dm: '请输入工具名称',
                      }),
                    },
                  ]}
                  label={$i18n.get({
                    id: 'main.pages.Component.Plugin.Tools.Edit.toolName',
                    dm: '工具名称',
                  })}
                >
                  <Input
                    placeholder={$i18n.get({
                      id: 'main.pages.Component.Plugin.Tools.Edit.enterToolName',
                      dm: '请输入工具名称',
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
                        id: 'main.pages.Component.Plugin.Tools.Edit.enterToolDescription',
                        dm: '请输入工具描述，帮助用户更好的理解工具功能和使用场景',
                      }),
                    },
                  ]}
                  label={$i18n.get({
                    id: 'main.pages.Component.Plugin.Tools.Edit.toolDescription',
                    dm: '工具描述',
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
                      id: 'main.pages.Component.Plugin.Tools.Edit.enterToolDescription',
                      dm: '请输入工具描述，帮助用户更好的理解工具功能和使用场景',
                    })}
                  />
                </Form.Item>

                <Form.Item
                  name={['config', 'path']}
                  rules={[
                    {
                      required: true,
                      message: $i18n.get({
                        id: 'main.pages.Component.Plugin.Tools.Edit.enterToolPath',
                        dm: '请输入工具路径',
                      }),
                    },
                  ]}
                  label={$i18n.get({
                    id: 'main.pages.Component.Plugin.Tools.Edit.toolPath',
                    dm: '工具路径',
                  })}
                >
                  <Input addonBefore={data?.data.config?.server} />
                </Form.Item>

                <Form.Item
                  name={['config', 'request_method']}
                  rules={[
                    {
                      required: true,
                      message: $i18n.get({
                        id: 'main.pages.Component.Plugin.Tools.Edit.selectRequestMethod',
                        dm: '请选择请求方法',
                      }),
                    },
                  ]}
                  label={$i18n.get({
                    id: 'main.pages.Component.Plugin.Tools.Edit.requestMethod',
                    dm: '请求方法',
                  })}
                >
                  <Select
                    options={['GET', 'POST'].map((item) => ({
                      value: item,
                      label: item,
                    }))}
                  />
                </Form.Item>

                {requestMethod === 'POST' && (
                  <Form.Item
                    name={['config', 'content_type']}
                    rules={[
                      {
                        required: true,
                        message: $i18n.get({
                          id: 'main.pages.Component.Plugin.Tools.Edit.selectSubmitMethod',
                          dm: '请选择提交方式',
                        }),
                      },
                    ]}
                    label={$i18n.get({
                      id: 'main.pages.Component.Plugin.Tools.Edit.submitMethod',
                      dm: '提交方式',
                    })}
                    style={{ marginBottom: 0 }}
                  >
                    <Select
                      options={[
                        'application/json',
                        'application/x-www-form-urlencoded',
                      ].map((item) => ({ value: item, label: item }))}
                    />
                  </Form.Item>
                )}
              </div>
              <Divider />
              <div
                ref={(el: HTMLDivElement) => {
                  menuRef.current[1] = el;
                }}
                className={styles['form-area']}
              >
                <div className={classNames(styles.title, styles.required)}>
                  {$i18n.get({
                    id: 'main.pages.Component.Plugin.Tools.Edit.configureInputParameters',
                    dm: '配置输入参数',
                  })}
                </div>
                <InputParamsConfig
                  requestMethod={requestMethod}
                  params={state.inputParams}
                  onChange={(val) => {
                    setState({ inputParams: val });
                  }}
                />
              </div>
              <Divider />
              <div
                ref={(el: HTMLDivElement) => {
                  menuRef.current[2] = el;
                }}
                className={styles['form-area']}
              >
                <div className={classNames(styles.title, styles.required)}>
                  {$i18n.get({
                    id: 'main.pages.Component.Plugin.Tools.Edit.configureOutputParameters',
                    dm: '配置输出参数',
                  })}
                </div>
                <OutputParamsConfig
                  params={state.outputParams}
                  onChange={(val) => {
                    setState({ outputParams: val });
                  }}
                />
              </div>
              <Divider />
              <div
                ref={(el: HTMLDivElement) => {
                  menuRef.current[3] = el;
                }}
                className={styles['form-area']}
              >
                <Flex justify="space-between" align="center">
                  <span className={styles.title}>
                    {$i18n.get({
                      id: 'main.pages.Component.Plugin.Tools.Edit.advancedConfiguration',
                      dm: '高级配置',
                    })}
                  </span>
                </Flex>
                <div className={styles.tip}>
                  {$i18n.get({
                    id: 'main.pages.Component.Plugin.Tools.Edit.addCallExamples',
                    dm: '（可选）为大模型增加调用示例，提升大模型调用插件的准确性',
                  })}
                </div>
                <ExampleConfigForm
                  inputParams={state.inputParams}
                  onChange={(val) => setState({ examples: val })}
                  examples={state.examples}
                />
              </div>
              {bottomPortal(
                <Flex className={styles['bottom-bar']} align="center" gap={12}>
                  {publishButton}
                  {saveButton}
                  {cancelButton}
                  <div className={styles.divider} />
                  <Test
                    pluginId={id}
                    toolId={toolId as string}
                    inputParams={state.inputParams}
                  />
                </Flex>,
              )}
            </Flex>
          </Form>
          <div className={styles['scroll-control']}>
            <ScrollMenu
              activeMenuCode={activeIndex}
              handleMenuClick={handleMenuClick}
              menus={[
                $i18n.get({
                  id: 'main.pages.Component.Plugin.Tools.Edit.toolInfo',
                  dm: '工具信息',
                }),
                $i18n.get({
                  id: 'main.pages.Component.Plugin.Tools.Edit.inputParameters',
                  dm: '输入参数',
                }),
                $i18n.get({
                  id: 'main.pages.Component.Plugin.Tools.Edit.outputParameters',
                  dm: '输出参数',
                }),
                $i18n.get({
                  id: 'main.pages.Component.Plugin.Tools.Edit.advancedConfiguration',
                  dm: '高级配置',
                }),
              ]}
            />
          </div>
        </div>
      </div>
    </InnerLayout>
  );
}
