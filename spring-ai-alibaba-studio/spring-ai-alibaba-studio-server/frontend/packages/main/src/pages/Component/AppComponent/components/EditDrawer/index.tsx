import $i18n from '@/i18n';
import {
  createAppComponent,
  getAppComponentDetailByCode,
  getConfigByAppId,
  IAppType,
  updateAppComponent,
} from '@/services/appComponent';
import { IAppComponentListItem } from '@/types/appComponent';
import { Button, Drawer, Empty, Form, message } from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { Flex, Input, Spin } from 'antd';
import classNames from 'classnames';
import { useMemo } from 'react';
import InputParamsComp, {
  IConfigInput,
  IOutputParamItem,
} from '../InputParamsComp';
import OutputParamsComp from '../OutputParamsComp';
import styles from './index.module.less';

interface IProps {
  data: Partial<IAppComponentListItem>;
  onClose: () => void;
  onOk: () => void;
}

export default function EditDrawer(props: IProps) {
  const [state, setState] = useSetState({
    loading: !!props.data.code,
    detail: null as IAppComponentListItem | null,
    saveLoading: false,
    input: {
      system_params: [],
      user_params: [],
    } as IConfigInput,
    output: [] as IOutputParamItem[],
  });
  const [form] = Form.useForm();

  const renderTitle = useMemo(() => {
    const componentNameMap = {
      [IAppType.AGENT]: $i18n.get({
        id: 'main.pages.Component.AppComponent.components.EditDrawer.index.smartAgent',
        dm: '智能体',
      }),
      [IAppType.WORKFLOW]: $i18n.get({
        id: 'main.pages.Component.AppComponent.components.EditDrawer.index.workflow',
        dm: '工作流',
      }),
    };
    if (props.data.code)
      return $i18n.get({
        id: 'main.pages.Component.AppComponent.components.EditDrawer.index.editComponent',
        dm: '编辑组件',
      });
    const componentName = componentNameMap[props.data.type as IAppType];
    return $i18n.get(
      {
        id: 'main.pages.Component.AppComponent.components.EditDrawer.index.publishAsVar1Component',
        dm: '发布为{var1}组件',
      },
      { var1: componentName },
    );
  }, [props.data.code, props.data.type]);

  const handleSave = () => {
    form.validateFields().then((formValues: any) => {
      const { input } = state;
      let params = [...input.system_params];
      input.user_params.forEach((item) => {
        params = [...params, ...item.params];
      });
      if (params.some((item) => !item.alias && item.display)) {
        message.warning(
          $i18n.get({
            id: 'main.pages.Component.AppComponent.components.EditDrawer.index.parameterAliasRequiredCheck',
            dm: '参数别名必填，请检查',
          }),
        );
        return;
      }
      const aliasMap = new Map();
      for (const item of params) {
        if (item.alias) {
          if (aliasMap.has(item.alias)) {
            message.warning(
              $i18n.get({
                id: 'main.pages.Component.AppComponent.components.EditDrawer.index.parameterAliasCannotRepeatCheck',
                dm: '参数别名不能重复，请检查',
              }),
            );
            return;
          }
          aliasMap.set(item.alias, true);
        }
      }
      setState({ saveLoading: true });
      const updateApi = props.data.code
        ? updateAppComponent
        : createAppComponent;
      updateApi({
        app_id: props.data.app_id as string,
        code: props.data.code as string,
        name: formValues.name,
        description: formValues.description,
        type: props.data.type as IAppType,
        config: JSON.stringify({
          input: state.input,
          output: state.output,
        }),
      })
        .then(() => {
          message.success(
            props.data.code
              ? $i18n.get({
                  id: 'main.pages.Component.AppComponent.components.EditDrawer.index.updateSuccess',
                  dm: '更新成功',
                })
              : $i18n.get({
                  id: 'main.pages.Component.AppComponent.components.EditDrawer.index.createSuccess',
                  dm: '创建成功',
                }),
          );
          props.onOk();
        })
        .finally(() => {
          setState({ saveLoading: false });
        });
    });
  };

  const renderFooter = useMemo(() => {
    if (!state.detail) return null;
    return (
      <Flex justify="flex-end" gap={8}>
        <Button
          disabled={state.saveLoading}
          loading={state.saveLoading}
          onClick={() => handleSave()}
          type="primary"
        >
          {state.saveLoading
            ? $i18n.get({
                id: 'main.pages.Component.AppComponent.components.EditDrawer.index.publishing',
                dm: '发布中...',
              })
            : $i18n.get({
                id: 'main.pages.Component.AppComponent.components.EditDrawer.index.confirmPublish',
                dm: '确定发布',
              })}
        </Button>
        <Button onClick={props.onClose}>
          {$i18n.get({
            id: 'main.pages.Component.AppComponent.components.EditDrawer.index.cancel',
            dm: '取消',
          })}
        </Button>
      </Flex>
    );
  }, [state]);

  useMount(async () => {
    try {
      if (props.data.code) {
        const res = await getAppComponentDetailByCode(props.data.code);
        if (!res) return;
        const componentDetailCfg = JSON.parse(res.config);
        setState({
          input: componentDetailCfg.input,
          output: componentDetailCfg.output,
          detail: res,
        });
        form.setFieldsValue({
          name: res.name,
          description: res.description,
        });
      } else {
        const ret = await getConfigByAppId(props.data.app_id as string);
        const componentParams = JSON.parse(ret.config);
        setState({
          input: componentParams.input,
          output: componentParams.output,
          // @ts-ignore
          detail: {
            app_name: ret.app_name,
            app_id: ret.app_id,
          },
        });
      }
    } finally {
      setState({
        loading: false,
      });
    }
  });

  return (
    <Drawer
      className={styles.drawer}
      width={960}
      footer={renderFooter}
      open
      onClose={props.onClose}
      title={renderTitle}
    >
      {state.loading ? (
        <Spin className="loading-center" />
      ) : !state.detail ? (
        <div className="loading-center">
          <Empty
            title={$i18n.get({
              id: 'main.pages.Component.AppComponent.components.EditDrawer.index.configurationInformationNotExists',
              dm: '配置信息不存在',
            })}
            description={$i18n.get({
              id: 'main.pages.Component.AppComponent.components.EditDrawer.index.componentAssociatedApplicationHasBeenDeletedDeleteAndRecreate',
              dm: '该组件关联的应用已被删除，请删除重建',
            })}
          />
        </div>
      ) : (
        <Form className={styles.form} layout={'vertical'} form={form}>
          <div
            className={classNames(
              styles['form-item-con'],
              'flex flex-col gap-5',
            )}
          >
            <div className={styles['form-title']}>
              {$i18n.get({
                id: 'main.pages.Component.AppComponent.components.EditDrawer.index.basicInformation',
                dm: '基础信息',
              })}
            </div>
            <Form.Item
              label={$i18n.get({
                id: 'main.pages.Component.AppComponent.components.EditDrawer.index.componentName',
                dm: '组件名称',
              })}
              required
              name="name"
              rules={[
                {
                  required: true,
                  message: $i18n.get({
                    id: 'main.pages.Component.AppComponent.components.EditDrawer.index.enterComponentName',
                    dm: '请输入组件名称',
                  }),
                },
              ]}
            >
              <Input
                maxLength={50}
                showCount
                placeholder={$i18n.get({
                  id: 'main.pages.Component.AppComponent.components.EditDrawer.index.enterComponentName',
                  dm: '请输入组件名称',
                })}
              />
            </Form.Item>
            <Form.Item
              label={$i18n.get({
                id: 'main.pages.Component.AppComponent.components.EditDrawer.index.componentDescription',
                dm: '组件描述',
              })}
              name="description"
              rules={[
                {
                  required: true,
                  message: $i18n.get({
                    id: 'main.pages.Component.AppComponent.components.EditDrawer.index.enterComponentDescription',
                    dm: '请输入组件描述',
                  }),
                },
              ]}
            >
              <Input.TextArea
                autoSize={{ minRows: 4, maxRows: 4 }}
                maxLength={1000}
                showCount
                placeholder={$i18n.get({
                  id: 'main.pages.Component.AppComponent.components.EditDrawer.index.enterComponentDescription',
                  dm: '请输入组件描述',
                })}
              />
            </Form.Item>
          </div>
          <div
            className={classNames(
              styles['form-item-con'],
              'flex flex-col gap-5',
            )}
          >
            <div className={classNames(styles['form-title'], styles.required)}>
              <span>
                {$i18n.get({
                  id: 'main.pages.Component.AppComponent.components.EditDrawer.index.inputInformation',
                  dm: '输入信息',
                })}
              </span>
              <img src="/images/require.svg" alt="" />
            </div>
            <InputParamsComp
              input={state.input}
              onChange={(val) =>
                setState({
                  input: {
                    ...state.input,
                    ...val,
                  },
                })
              }
            />
          </div>
          <div
            className={classNames(
              styles['form-item-con'],
              'flex flex-col gap-5',
            )}
          >
            <div className={classNames(styles['form-title'], styles.required)}>
              <span>
                {$i18n.get({
                  id: 'main.pages.Component.AppComponent.components.EditDrawer.index.outputInformation',
                  dm: '输出信息',
                })}
              </span>
              <img src="/images/require.svg" alt="" />
            </div>
            <OutputParamsComp output={state.output} />
          </div>
        </Form>
      )}
    </Drawer>
  );
}
