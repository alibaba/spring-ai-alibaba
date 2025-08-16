import $i18n from '@/i18n';
import { UserPromptParams } from '@/types/appManage';
import { Button, Form, IconFont, Tag } from '@spark-ai/design';
import { Divider, Flex, Input } from 'antd';
import classNames from 'classnames';
import { useContext, useEffect, useState } from 'react';
// @ts-ignore
import uuid from 'uuid';
import { AssistantAppContext } from '../../../AssistantAppContext';
import styles from './index.module.less';

interface IProps {
  variables: { label: string; code: string }[];
  userDefinedVariables: { label: string; code: string }[];
  setUserDefinedVariables: React.Dispatch<
    React.SetStateAction<
      {
        label: string;
        code: string;
      }[]
    >
  >;
}
export default (props: IProps) => {
  const { variables } = props;
  const { appState, onAppConfigChange } = useContext(AssistantAppContext);
  const prompt_variables: UserPromptParams[] = (appState.appBasicConfig?.config
    .prompt_variables || []) as UserPromptParams[]; // variables in database
  const [expand, setExpand] = useState(true);
  const [tempVariables, setTempVariables] = useState<
    (UserPromptParams & { key: string })[]
  >([]); // this is the local state, will be saved to database when input loses focus
  // local form validation information
  const [validateInfo, setValidateInfo] = useState<
    Record<
      string,
      {
        validateStatus?: 'success' | 'warning' | 'error' | 'validating';
        help: string;
      }
    >[]
  >([]);
  const [hasInitTempVariables, setHasInitTempVariables] = useState(false);

  useEffect(() => {
    if (
      !hasInitTempVariables &&
      appState.hasInitData &&
      !!prompt_variables?.length
    ) {
      // initialize the variable list according to the variables in database
      setTempVariables(
        prompt_variables.map((variable) => ({
          ...variable,
          key: uuid(8),
        })),
      );
      setValidateInfo(
        new Array(prompt_variables.length).fill({
          name: {
            help: '',
          },
        }),
      );
      setHasInitTempVariables(true);
    }
  }, [appState.hasInitData, prompt_variables, hasInitTempVariables]);

  const handleRemoveVariable = (index: number) => {
    /**delete variable */
    const newTemplateVariables = tempVariables.filter((_, i) => i !== index);
    const newValidateInfo = validateInfo.filter((_, i) => i !== index);
    setTempVariables(newTemplateVariables); // delete the local variable
    setValidateInfo(newValidateInfo); // delete the validation information
    /**filter out the valid variables, and update the database */
    const validVariables = newTemplateVariables.filter((_, index) => {
      return (
        !!_.name?.length &&
        newValidateInfo[index].name?.validateStatus !== 'error'
      );
    });
    onAppConfigChange({
      prompt_variables: validVariables,
    });
  };
  const handleUpdateVariable = (
    index: number,
    key: 'name' | 'default_value' | 'description',
    value: string,
  ) => {
    if (key === 'name') {
      const newValidateInfo = [...validateInfo];
      if (!/^[a-zA-Z0-9]*$/.test(value)) {
        newValidateInfo[index]['name'].validateStatus = 'error';
        newValidateInfo[index]['name'].help = $i18n.get({
          id: 'main.pages.App.AssistantAppEdit.components.AssistantPromptEditor.Variables.index.variableNameOnlyContainsEnglishAndNumbers',
          dm: '变量名只能包含英文和数字',
        });
        setValidateInfo(newValidateInfo);
        return;
      }
      /**
       * check if the variable name is empty
       */
      if (!value.length) {
        // the variable name is empty, but it does not block the modification of appConfig, because if the user manually clears the variable name, we need to remove it from the variable list
        newValidateInfo[index]['name'].validateStatus = 'error';
        newValidateInfo[index]['name'].help = $i18n.get({
          id: 'main.pages.App.AssistantAppEdit.components.AssistantPromptEditor.Variables.index.variableNameCannotBeEmpty',
          dm: '变量名不能为空',
        });
        setValidateInfo(newValidateInfo);
      } else {
        /**
         * check if the variable name is duplicate, if so, mark it in validateInfo
         */
        const variablesName = new Set();
        variables.forEach((variable) => {
          // all the existing variables in PromptEditor
          variablesName.add(variable.code);
        });
        if (variablesName.has(value)) {
          newValidateInfo[index]['name'].validateStatus = 'error';
          newValidateInfo[index]['name'].help = $i18n.get({
            id: 'main.pages.App.AssistantAppEdit.components.AssistantPromptEditor.Variables.index.variableNameCannotRepeat',
            dm: '变量名不能重复',
          });
          setValidateInfo(newValidateInfo);
        } else {
          /**validate status */
          newValidateInfo[index]['name'].validateStatus = undefined;
          newValidateInfo[index]['name'].help = '';
          setValidateInfo(newValidateInfo);
        }
      }
    }
    setTempVariables((prev) =>
      prev.map((variable, i) => {
        if (i === index) {
          return {
            ...variable,
            [key]: value,
          };
        }
        return variable;
      }),
    );
  };

  const handleSaveAppConfig = () => {
    const validVariables = tempVariables.filter((_, index) => {
      return (
        !!_.name?.length && validateInfo[index].name?.validateStatus !== 'error'
      );
    });
    onAppConfigChange({
      prompt_variables: validVariables,
    });
  };

  return (
    <div className="mb-[20px]">
      <Flex justify="space-between">
        <Flex gap={8} align="center" className={styles.title}>
          <span
            className="text-[13px] text-medium leading-[20px]"
            style={{ color: 'var(--ag-ant-color-text)' }}
          >
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.VariableHandle.schema.variable',
              dm: '变量',
            })}
          </span>
        </Flex>
        {
          <Flex gap={16} align="center" className={styles.actions}>
            <Button
              style={{ padding: 0 }}
              size="small"
              type="text"
              iconType="spark-plus-line"
              onClick={() => {
                setTempVariables((prev) => [
                  ...prev,
                  {
                    description: '',
                    name: '',
                    default_value: '',
                    key: uuid(8),
                    type: 'string',
                  },
                ]);
                setValidateInfo((prev) => [
                  ...prev,
                  {
                    name: {
                      help: '',
                    },
                  },
                ]);
                setExpand(true);
              }}
            >
              {$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Start.panel.customVariables',
                dm: '自定义变量',
              })}
            </Button>
            <Divider type="vertical"></Divider>
            <IconFont
              onClick={() => setExpand((prev) => !prev)}
              className={classNames(styles.expandBtn, !expand && styles.hidden)}
              type="spark-up-line"
            />
          </Flex>
        }
      </Flex>

      {expand && !!tempVariables.length && (
        <>
          <div
            className={
              'text-[12px] text-normal leading-[24px] mt-[4px] mb-[8px]'
            }
            style={{
              color: 'var(--ag-ant-color-text-tertiary)',
            }}
          >
            {$i18n.get({
              id: 'main.pages.App.AssistantAppEdit.components.AssistantPromptEditor.Variables.index.canBeFilledThroughParameterFormOr',
              dm: '可通过入参变量表单填写，或',
            })}
            <Tag
              color="mauve"
              style={{ marginInlineStart: 'var(--ag-ant-margin-xs)' }}
            >
              biz_params
            </Tag>
            {$i18n.get({
              id: 'main.pages.App.AssistantAppEdit.components.AssistantPromptEditor.Variables.index.fieldPassage',
              dm: '字段传递，传入变量值将替换提示词中对应的变量位置。',
            })}
          </div>
          <div className={styles.variablesWrapper}>
            <Flex vertical gap={8}>
              <Flex style={{ width: '100%' }} gap={8}>
                <div
                  className="text-[12px] text-normal leading-[20px]"
                  style={{
                    flex: 3,
                    color: 'var(--ag-ant-color-text-secondary)',
                  }}
                >
                  {$i18n.get({
                    id: 'main.pages.App.Workflow.components.IteratorVariableForm.index.variableName',
                    dm: '变量名',
                  })}
                </div>
                <div
                  className="text-[12px] text-normal leading-[20px]"
                  style={{
                    flex: 5,
                    color: 'var(--ag-ant-color-text-secondary)',
                  }}
                >
                  {$i18n.get({
                    id: 'main.pages.App.Workflow.components.ExtractParamEditModal.index.description',
                    dm: '描述',
                  })}
                </div>
                <div
                  className="text-[12px] text-normal leading-[20px]"
                  style={{
                    flex: 2,
                    color: 'var(--ag-ant-color-text-secondary)',
                  }}
                >
                  {$i18n.get({
                    id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.defaultValue',
                    dm: '默认值',
                  })}
                </div>
              </Flex>
              <Form className={styles.form}>
                {tempVariables.map((item, index) => (
                  <Flex key={item.key} style={{ width: '100%' }} gap={8}>
                    <div
                      className="text-[12px] text-normal leading-[20px]"
                      style={{
                        flex: 3,
                        color: 'var(--ag-ant-color-text-secondary)',
                      }}
                    >
                      <Form.Item
                        validateStatus={
                          validateInfo[index]?.['name'].validateStatus
                        }
                        help={validateInfo[index]?.['name'].help}
                      >
                        <Input
                          value={item.name}
                          onBlur={() => {
                            handleSaveAppConfig();
                          }}
                          onPressEnter={() => {
                            handleSaveAppConfig();
                          }}
                          onChange={(e) => {
                            handleUpdateVariable(index, 'name', e.target.value);
                          }}
                        ></Input>
                      </Form.Item>
                    </div>
                    <div
                      className="text-[12px] text-normal leading-[20px]"
                      style={{
                        flex: 5,
                        color: 'var(--ag-ant-color-text-secondary)',
                      }}
                    >
                      <Form.Item>
                        <Input
                          value={item.description}
                          onBlur={() => {
                            handleSaveAppConfig();
                          }}
                          onPressEnter={() => {
                            handleSaveAppConfig();
                          }}
                          onChange={(e) => {
                            handleUpdateVariable(
                              index,
                              'description',
                              e.target.value,
                            );
                          }}
                        ></Input>
                      </Form.Item>
                    </div>
                    <div
                      className="text-[12px] text-normal leading-[20px]"
                      style={{
                        flex: 2,
                        color: 'var(--ag-ant-color-text-secondary)',
                      }}
                    >
                      <Flex gap={8} align="center">
                        <Form.Item>
                          <Input
                            value={item.default_value}
                            onBlur={() => {
                              handleSaveAppConfig();
                            }}
                            onPressEnter={() => {
                              handleSaveAppConfig();
                            }}
                            onChange={(e) => {
                              handleUpdateVariable(
                                index,
                                'default_value',
                                e.target.value,
                              );
                            }}
                          ></Input>
                        </Form.Item>
                        <IconFont
                          className={'mb-[16px]'}
                          onClick={() => handleRemoveVariable(index)}
                          type="spark-delete-line"
                          isCursorPointer
                        />
                      </Flex>
                    </div>
                  </Flex>
                ))}
              </Form>
            </Flex>
          </div>
        </>
      )}
    </div>
  );
};
