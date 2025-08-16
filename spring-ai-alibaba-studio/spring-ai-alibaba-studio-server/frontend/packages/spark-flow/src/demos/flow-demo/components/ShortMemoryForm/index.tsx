import $i18n from '@/i18n';
import { IconFont } from '@spark-ai/design';
import { SelectWithDesc } from '@spark-ai/flow';
import { Switch } from 'antd';
import React, { memo } from 'react';
import { IShortMemoryConfig } from '../../types/flow';

const memoryOptions = [
  {
    label: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.components.ShortMemoryForm.index.nodeCache',
      dm: '本节点缓存',
    }),
    value: 'self',
  },
  {
    label: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.components.ShortMemoryForm.index.customCache',
      dm: '自定义缓存',
    }),
    value: 'custom',
  },
];

export default memo(function ShortMemoryForm({
  value,
  onChange,
}: {
  value: IShortMemoryConfig;
  onChange: (value: IShortMemoryConfig) => void;
}) {
  return (
    <>
      <div className="flex items-center justify-between">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.components.ShortMemoryForm.index.memory',
            dm: '记忆',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <Switch
          value={value.enabled}
          onChange={(val) => onChange({ ...value, enabled: val })}
        />
      </div>
      {value.enabled && (
        <SelectWithDesc
          value={value.type}
          onChange={(val) => onChange({ ...value, type: val })}
          options={memoryOptions}
        />
      )}
    </>
  );
});
