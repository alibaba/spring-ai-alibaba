import $i18n from '@/i18n';
import { SelectWithDesc, VariableInput } from '@spark-ai/flow';
import { Switch } from 'antd';
import React, { memo } from 'react';
import { ITryCatchConfig } from '../../types/flow';

const strategyOpts = [
  {
    label: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.components.ErrorCatchForm.index.defaultValue',
      dm: '默认值',
    }),
    value: 'defaultValue',
    desc: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.components.ErrorCatchForm.index.defaultOutputWhenExceptionOccurs',
      dm: '当发生异常时，指定默认输出内容',
    }),
  },
  {
    label: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.components.ErrorCatchForm.index.exceptionBranch',
      dm: '异常分支',
    }),
    value: 'failBranch',
    desc: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.components.ErrorCatchForm.index.executeExceptionBranchWhenExceptionOccurs',
      dm: '当发生异常时，则执行异常分支',
    }),
  },
];

export default memo(function ErrorCatchForm({
  value,
  onChange,
}: {
  value: ITryCatchConfig;
  onChange: (val: ITryCatchConfig) => void;
}) {
  return (
    <>
      <div className="flex-justify-between">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.components.ErrorCatchForm.index.exceptionHandling',
            dm: '异常处理',
          })}
        </div>
        <Switch
          checked={value.strategy !== 'noop'}
          onChange={(checked) =>
            onChange({ ...value, strategy: checked ? 'defaultValue' : 'noop' })
          }
        />
      </div>
      {value.strategy !== 'noop' && (
        <>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.components.ErrorCatchForm.index.handlingMethod',
              dm: '处理方式',
            })}
          </div>
          <SelectWithDesc
            value={value.strategy}
            onChange={(val) =>
              onChange({
                ...value,
                strategy: val as ITryCatchConfig['strategy'],
              })
            }
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
            <VariableInput type={item.type} onChange={() => {}} />
          </React.Fragment>
        ))}
    </>
  );
});
