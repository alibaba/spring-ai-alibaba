import $i18n from '@/i18n';
import type { INodeDataInputParamItem } from '@spark-ai/flow';
import {
  SelectWithDesc,
  useEdgesInteraction,
  VariableBaseInput,
} from '@spark-ai/flow';
import { Flex, Switch } from 'antd';
import React, { memo } from 'react';
import { ITryCatchConfig } from '../../types';
import InfoIcon from '../InfoIcon';

const strategyOpts = [
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.components.ErrorCatchForm.index.defaultValue',
      dm: '默认值',
    }),
    value: 'defaultValue',
    desc: $i18n.get({
      id: 'main.pages.App.Workflow.components.ErrorCatchForm.index.index.outputSpecifiedContent',
      dm: '当发生异常时，输出指定内容。',
    }),
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.components.ErrorCatchForm.index.index.exceptionBranch',
      dm: '异常分支',
    }),
    value: 'failBranch',
    desc: $i18n.get({
      id: 'main.pages.App.Workflow.components.ErrorCatchForm.index.exceptionBranch',
      dm: '当发生异常时，执行异常分支。',
    }),
  },
];

export default memo(function ErrorCatchForm({
  value,
  onChange,
  nodeId,
  disabled,
  onChangeType,
}: {
  value: ITryCatchConfig;
  onChange: (val: ITryCatchConfig) => void;
  nodeId: string;
  disabled?: boolean;
  onChangeType: (type: ITryCatchConfig['strategy']) => void;
}) {
  const { handleRemoveEdgeByTargetId } = useEdgesInteraction();
  const changeRowItem = (
    ind: number,
    val: Partial<Omit<INodeDataInputParamItem, 'value_from'>>,
  ) => {
    const newVal = [...(value.default_values || [])];
    newVal[ind] = { ...newVal[ind], ...val };
    onChange({ ...value, default_values: newVal });
  };

  return (
    <Flex vertical gap={12}>
      <div className="flex-justify-between">
        <div className="spark-flow-panel-form-title">
          <span>
            {$i18n.get({
              id: 'main.pages.App.Workflow.components.ErrorCatchForm.index.executeExceptionBranch',
              dm: '异常处理',
            })}
          </span>
          <InfoIcon
            tip={$i18n.get({
              id: 'main.pages.App.Workflow.components.ErrorCatchForm.index.handleErrors',
              dm: '针对报错等异常情况，节点根据配置继续执行。',
            })}
          />
        </div>
        <Switch
          checked={value.strategy !== 'noop'}
          disabled={disabled}
          onChange={(checked) => {
            handleRemoveEdgeByTargetId(`${nodeId}_fail`);
            onChangeType(checked ? 'failBranch' : 'noop');
          }}
        />
      </div>
      {value.strategy !== 'noop' && (
        <>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.components.ErrorCatchForm.index.handlingMethod',
              dm: '处理方式',
            })}
          </div>
          <SelectWithDesc
            disabled={disabled}
            value={value.strategy}
            onChange={(val) => {
              handleRemoveEdgeByTargetId(`${nodeId}_fail`);
              onChange({
                ...value,
                strategy: val as ITryCatchConfig['strategy'],
              });
            }}
            options={strategyOpts}
          />
        </>
      )}
      {value.strategy === 'defaultValue' &&
        !!value.default_values &&
        value.default_values.map((item, index) => (
          <React.Fragment key={index}>
            <div className="spark-flow-panel-form-title">
              {item.key}
              <div className="spark-flow-panel-form-title-desc">
                {`(${item.type})`}
              </div>
            </div>
            <VariableBaseInput
              disabled={disabled}
              type={item.type}
              value={item.value}
              onChange={(val) => {
                changeRowItem(index, { value: val.value });
              }}
            />
          </React.Fragment>
        ))}
    </Flex>
  );
});
