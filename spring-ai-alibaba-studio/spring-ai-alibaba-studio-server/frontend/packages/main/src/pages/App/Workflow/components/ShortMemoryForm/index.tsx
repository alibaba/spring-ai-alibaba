import $i18n from '@/i18n';
import { SliderSelector } from '@spark-ai/design';
import { IVarTreeItem, SelectWithDesc, VariableSelector } from '@spark-ai/flow';
import { Flex, Switch } from 'antd';
import classNames from 'classnames';
import { memo } from 'react';
import { IShortMemoryConfig } from '../../types';
import InfoIcon from '../InfoIcon';
import styles from './index.module.less';

const memoryOptions = [
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.components.ShortMemoryForm.index.nodeCache',
      dm: '本节点缓存',
    }),
    value: 'self',
    desc: $i18n.get({
      id: 'main.pages.App.Workflow.components.ShortMemoryForm.index.contextualInfoOnlyInNode',
      dm: '模型只会记得本节点内发生的上下文信息。',
    }),
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.components.ShortMemoryForm.index.customCache',
      dm: '自定义缓存',
    }),
    value: 'custom',
    desc: $i18n.get({
      id: 'main.pages.App.Workflow.components.ShortMemoryForm.index.globalContextualInfo',
      dm: '模型会记得全局的上下文信息。',
    }),
  },
];

export default memo(function ShortMemoryForm({
  value,
  onChange,
  variableList,
  disabled,
}: {
  value: IShortMemoryConfig;
  onChange: (value: IShortMemoryConfig) => void;
  variableList: IVarTreeItem[];
  disabled?: boolean;
}) {
  return (
    <>
      <Flex vertical gap={12}>
        <div className={classNames('flex items-center justify-between')}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.components.ShortMemoryForm.index.memory',
              dm: '记忆',
            })}
          </div>
          <Switch
            value={value.enabled}
            onChange={(val) => onChange({ ...value, enabled: val })}
            disabled={disabled}
          />
        </div>
        {value.enabled && (
          <SelectWithDesc
            value={value.type}
            onChange={(val) => onChange({ ...value, type: val })}
            options={memoryOptions}
            disabled={disabled}
          />
        )}
      </Flex>
      {value.enabled &&
        (value.type === 'custom' ? (
          <Flex vertical gap={12}>
            <div className="spark-flow-panel-form-title">
              {$i18n.get({
                id: 'main.pages.App.Workflow.components.ShortMemoryForm.index.contextVariables',
                dm: '上下文变量',
              })}
            </div>
            <VariableSelector
              disabled={disabled}
              variableList={variableList}
              value={value.param}
              onChange={(val) =>
                onChange({
                  ...value,
                  param: {
                    ...value.param,
                    ...val,
                  },
                })
              }
            />
          </Flex>
        ) : (
          <Flex vertical gap={12}>
            <div className="spark-flow-panel-form-title">
              <span>
                {$i18n.get({
                  id: 'main.pages.App.Workflow.components.ShortMemoryForm.index.memoryRounds',
                  dm: '记忆轮次',
                })}
              </span>
              <InfoIcon
                tip={$i18n.get({
                  id: 'main.pages.App.Workflow.components.ShortMemoryForm.index.memoryRoundsDefinition',
                  dm: '代表记忆的轮次，一次输入一次输出代表一轮。',
                })}
              />
            </div>
            <SliderSelector
              disabled={disabled}
              min={1}
              max={50}
              step={1}
              value={value.round}
              onChange={(val) =>
                onChange({
                  ...value,
                  round: val as IShortMemoryConfig['round'],
                })
              }
              inputNumberWrapperStyle={{ width: 54 }}
              className={classNames('flex-1', styles['slider-selector'])}
            />
          </Flex>
        ))}
    </>
  );
});
