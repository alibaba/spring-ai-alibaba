import $i18n from '@/i18n';
import { IconFont, SliderSelector } from '@spark-ai/design';
import { Switch } from 'antd';
import React from 'react';
import { IRetryConfig } from '../../types/flow';
import './index.less';

export default function RetryForm({
  value,
  onChange,
}: {
  value: IRetryConfig;
  onChange: (val: IRetryConfig) => void;
}) {
  return (
    <>
      <div className="flex-justify-between">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.components.RetryForm.index.retryOnFailure',
            dm: '失败时重试',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <Switch
          checked={value.retry_enabled}
          onChange={(checked) => onChange({ ...value, retry_enabled: checked })}
        />
      </div>
      {value.retry_enabled && (
        <div className="spark-flow-panel-form-area flex flex-col gap-[20px]">
          <div className="flex items-center">
            <span className="spark-flow-panel-form-area-label">
              {$i18n.get({
                id: 'spark-flow.demos.spark-flow-1.components.RetryForm.index.maxRetryTimes',
                dm: '最大重试次数',
              })}
            </span>
            <SliderSelector
              value={value.max_retries}
              onChange={(val) =>
                onChange({ ...value, max_retries: val as number })
              }
              step={1}
              className="flex-1 ml-[20px] mr-[12px]"
              min={1}
              max={10}
            />
          </div>
          <div className="flex items-center">
            <span className="spark-flow-panel-form-area-label">
              {$i18n.get({
                id: 'spark-flow.demos.spark-flow-1.components.RetryForm.index.retryInterval',
                dm: '重试间隔',
              })}
            </span>
            <SliderSelector
              className="flex-1 ml-[20px]"
              value={value.retry_interval}
              onChange={(val) =>
                onChange({ ...value, retry_interval: val as number })
              }
              step={100}
              min={300}
              max={10000}
            />
          </div>
        </div>
      )}
    </>
  );
}
