import $i18n from '@/i18n';
import { IMCPTool, IToolProperty } from '@/types/mcp';
import {
  Button,
  Card,
  CodeBlock,
  Form,
  IconFont,
  Input,
  Radio,
  Tooltip,
  message,
} from '@spark-ai/design';
import classNames from 'classnames';
import React, { useState } from 'react';
import styles from './index.module.less';

interface ToolPanelProps {
  server_Code: string;
  tool: IMCPTool;
  btnDisabled: boolean;
  onExecute: (params: {
    server_code: string;
    tool_name: string;
    tool_params: any;
  }) => Promise<any>;
}

const ToolPanel: React.FC<ToolPanelProps> = ({
  server_Code,
  tool,
  onExecute,
  btnDisabled = false,
}) => {
  const [expanded, setExpanded] = useState(false);
  const [form] = Form.useForm();
  const [executionResult, setExecutionResult] = useState<{
    success: boolean;
    result: any;
  } | null>(null);
  const [loading, setLoading] = useState(false);
  const handleToolClick = () => {
    setExpanded(!expanded);
  };

  const handleSubmit = async (values: any) => {
    try {
      Object.keys(values).forEach((key) => {
        const formValue = values[key];
        const targetItem = tool.input_schema.properties?.[key];
        if (targetItem && !!formValue) {
          if (['array', 'object'].includes(targetItem.type)) {
            values[key] = JSON.parse(formValue);
          }
          if (targetItem.type === 'number') {
            if (isNaN(Number(formValue))) {
              throw new Error(
                $i18n.get(
                  {
                    id: 'main.pages.MCP.components.ToolPanel.index.var1NeedsPureNumbers',
                    dm: '{var1}需要输入纯数字',
                  },
                  { var1: key },
                ),
              );
            } else {
              values[key] = Number(formValue);
            }
          }
        }
      });
    } catch (errMsg: any) {
      message.warning(
        $i18n.get({
          id: 'main.pages.MCP.components.ToolPanel.index.parameterParsingFailedCheckParameterFormat',
          dm: '参数解析失败，请检查参数格式',
        }),
      );
      return;
    }

    try {
      setLoading(true);
      const result = await onExecute({
        server_code: server_Code,
        tool_name: tool.name,
        tool_params: values,
      });
      setExecutionResult({ success: true, result });
    } catch (error) {
      setExecutionResult({ success: false, result: error });
      console.error('Form validation failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const renderFormItem = (key: string, property: IToolProperty) => {
    if (property.type === 'boolean') {
      return (
        <Radio.Group>
          <Radio value={true}>True</Radio>
          <Radio value={false}>False</Radio>
        </Radio.Group>
      );
    } else if (property.type === 'object') {
      return (
        <Input.TextArea
          placeholder={$i18n.get(
            {
              id: 'main.pages.MCP.components.ToolPanel.index.enterVar1',
              dm: '请输入{var1}',
            },
            { var1: key },
          )}
        />
      );
    }
    return (
      <Input
        placeholder={$i18n.get(
          {
            id: 'main.pages.MCP.components.ToolPanel.index.enterVar1',
            dm: '请输入{var1}',
          },
          { var1: key },
        )}
      />
    );
  };

  const renderToolInputs = () => {
    const { properties, required = [] } = tool.input_schema;
    const propertiesKeys = Object.keys(properties).sort((key) =>
      required.includes(key) ? -1 : 1,
    );

    return propertiesKeys.map((key) => {
      const property = properties[key];
      return (
        <Form.Item
          key={key}
          label={
            <div>
              <div className={styles['form-label']}>
                <div>{key}</div>
                <div className={styles['form-label-type']}>
                  {property.type && ` (${property.type})`}
                </div>
                {required.includes(key) && (
                  <span className={styles['required']}>*</span>
                )}
              </div>
              {property?.description && (
                <div className={styles['form-description']}>
                  {property.description}
                </div>
              )}
            </div>
          }
          name={key}
          rules={[
            {
              required: required.includes(key),
              message: $i18n.get(
                {
                  id: 'main.pages.MCP.components.ToolPanel.index.enterVar1',
                  dm: '请输入{var1}',
                },
                { var1: key },
              ),
            },
          ]}
          required={false}
          initialValue={property.type === 'boolean' ? false : undefined}
        >
          {renderFormItem(key, property)}
        </Form.Item>
      );
    });
  };

  return (
    <Card className={styles['tool-card']}>
      <div className={styles['tool-header']} onClick={handleToolClick}>
        <div className={styles['header-main']}>
          <div className={styles['title-row']}>
            <div className={'flex items-center gap-2'}>
              <IconFont
                className={styles['tool-icon']}
                type="spark-tool-line"
              />

              <span className={styles['tool-name']}>{tool.name}</span>
            </div>
            <IconFont
              type={expanded ? 'spark-up-line' : 'spark-down-line'}
              className={classNames(styles['expand-icon'], {
                [styles['expanded']]: expanded,
              })}
            />
          </div>
        </div>
      </div>

      {expanded && (
        <>
          <div className={styles['tool-description']}>{tool.description}</div>
          <div className={styles['tool-content']}>
            <Form
              form={form}
              onFinish={handleSubmit}
              layout="vertical"
              className={styles['tool-form']}
            >
              {renderToolInputs()}
              <Form.Item style={{ marginBottom: 0 }}>
                <div className={styles['form-actions']}>
                  <Tooltip
                    title={
                      btnDisabled
                        ? $i18n.get({
                            id: 'main.pages.MCP.components.ToolPanel.index.openServiceFirst',
                            dm: '请先开通此MCP服务后测试',
                          })
                        : ''
                    }
                  >
                    <Button
                      loading={loading}
                      type="primary"
                      htmlType="submit"
                      disabled={btnDisabled}
                    >
                      {$i18n.get({
                        id: 'main.pages.MCP.components.ToolPanel.index.run',
                        dm: '运行',
                      })}
                    </Button>
                  </Tooltip>
                  {executionResult?.success && (
                    <div className={styles['execute-status']}>
                      <IconFont
                        type="spark-check-circle-line"
                        className={styles['success-icon']}
                      />

                      <span>
                        {$i18n.get({
                          id: 'main.pages.MCP.components.ToolPanel.index.runSuccess',
                          dm: '运行成功',
                        })}
                      </span>
                    </div>
                  )}
                </div>
              </Form.Item>
            </Form>

            {executionResult && (
              <div className={styles['result-container']}>
                <CodeBlock
                  theme="dark"
                  language="json"
                  value={JSON.stringify(executionResult.result, null, 2)}
                />
              </div>
            )}
          </div>
        </>
      )}
    </Card>
  );
};

export default ToolPanel;
