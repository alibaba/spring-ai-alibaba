import $i18n from '@/i18n';
import { Button, IconButton } from '@spark-ai/design';
import { Divider, Flex, Input } from 'antd';
import classNames from 'classnames';
import { useState } from 'react';
import { InputParamItem } from '../InputParamsConfig';
import styles from './index.module.less';

export interface IExampleItem {
  query?: string;
  path?: string;
  parameters?: Record<string, string | undefined>;
}

interface IProps {
  examples: Array<IExampleItem>;
  onChange: (val: Array<IExampleItem>) => void;
  inputParams: Array<InputParamItem>;
}

const MAX_LEN = 5;

export default function ExampleConfigForm({
  examples = [],
  onChange,
  inputParams,
}: IProps) {
  const [expand] = useState(true);
  const changeExampleItem = (payload: any, index: number) => {
    onChange(
      examples.map((item, ind) => {
        if (ind === index)
          return {
            ...item,
            ...payload,
          };
        return item;
      }),
    );
  };

  const handleAddExample = () => {
    if (examples.length >= MAX_LEN) return;
    onChange([...examples, { query: void 0, parameters: {} }]);
  };

  const onRemove = (ind: number) => {
    onChange(examples.filter((_, index) => ind !== index));
  };

  return (
    <>
      {expand && (
        <Flex className={styles.content} vertical gap={8}>
          {examples.map((item, index) => (
            <div key={index}>
              <Flex
                key={index}
                gap={16}
                align="center"
                className={styles.formArea}
              >
                <Flex
                  className={styles.formItem}
                  style={{ flex: 1, width: 100 }}
                  vertical
                  gap={8}
                >
                  <Flex gap={8} className={styles.label}>
                    <span style={{ flex: 1, width: 'calc(50% - 98px)' }}>
                      {$i18n.get({
                        id: 'main.pages.Component.Plugin.components.ExampleConfigForm.index.userQuery',
                        dm: '用户输入Query',
                      })}
                    </span>
                    <span style={{ width: 180, flexShrink: 0 }}>
                      {$i18n.get({
                        id: 'main.pages.Component.Plugin.components.ExampleConfigForm.index.inputParameters',
                        dm: '输入参数',
                      })}
                    </span>
                    <Flex
                      gap={8}
                      style={{ flex: 1, width: 'calc(50% - 98px)' }}
                    >
                      Value
                    </Flex>
                  </Flex>
                  <Flex align="flex-start" gap={8}>
                    <Input
                      onChange={(e) =>
                        changeExampleItem({ query: e.target.value }, index)
                      }
                      value={item.query}
                      placeholder={$i18n.get({
                        id: 'main.pages.Component.Plugin.components.ExampleConfigForm.index.enterQuery',
                        dm: '请输入query',
                      })}
                      style={{ flexShrink: 0, width: 'calc(50% - 98px)' }}
                    />

                    <Flex style={{ flex: 1, width: 100 }} vertical gap={8}>
                      {inputParams.map(
                        (inputParam, inputIndex) =>
                          !!inputParam.key?.trim() && (
                            <Flex key={inputIndex} gap={8}>
                              <Input
                                disabled
                                style={{ width: 180, flexShrink: 0 }}
                                key={inputIndex}
                                value={inputParam.key}
                              />

                              <Input
                                placeholder={$i18n.get({
                                  id: 'main.pages.Component.Plugin.components.ExampleConfigForm.index.enterExampleValue',
                                  dm: '请输入示例值',
                                })}
                                onChange={(e) =>
                                  changeExampleItem(
                                    {
                                      parameters: {
                                        ...(item.parameters || {}),
                                        [inputParam.key as string]:
                                          e.target.value,
                                      },
                                    },
                                    index,
                                  )
                                }
                                style={{ flex: 1, width: 100 }}
                                key={inputIndex}
                                value={item.parameters?.[inputParam.key]}
                              />
                            </Flex>
                          ),
                      )}
                    </Flex>
                  </Flex>
                </Flex>
                <Divider type="vertical" style={{ height: 40 }} />

                <IconButton
                  onClick={() => onRemove(index)}
                  icon="spark-delete-line"
                  shape="circle"
                />
              </Flex>
            </div>
          ))}
          <a
            onClick={handleAddExample}
            className={classNames({
              [styles.disabled]: examples.length >= 5,
            })}
          >
            <Button
              type="link"
              size="small"
              disabled={examples.length >= MAX_LEN}
              iconType="spark-plus-line"
            >
              {$i18n.get({
                id: 'main.pages.Component.Plugin.components.ExampleConfigForm.index.addExample',
                dm: '增加示例',
              })}
              （{examples.length || 0}/{MAX_LEN}）
            </Button>
          </a>
        </Flex>
      )}
    </>
  );
}
