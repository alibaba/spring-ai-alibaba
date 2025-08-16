import $i18n from '@/i18n';
import { SliderSelector } from '@spark-ai/design';
import { Flex, Switch } from 'antd';
import classNames from 'classnames';
import { IRetryConfig } from '../../types';
import InfoIcon from '../InfoIcon';
import styles from './index.module.less';

export default function RetryForm({
  value,
  onChange,
  disabled,
}: {
  value: IRetryConfig;
  onChange: (val: IRetryConfig) => void;
  disabled?: boolean;
}) {
  return (
    <Flex vertical gap={12}>
      <div className="flex-justify-between">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'main.pages.App.Workflow.components.RetryForm.index.retryOnFailure',
            dm: '失败时重试',
          })}

          <InfoIcon
            tip={$i18n.get({
              id: 'main.pages.App.Workflow.components.RetryForm.index.retryWhenError',
              dm: '开启后，当发生错误时节点会尝试重新执行。',
            })}
          />
        </div>
        <Switch
          checked={value.retry_enabled}
          onChange={(checked) => onChange({ ...value, retry_enabled: checked })}
          disabled={disabled}
        />
      </div>
      {value.retry_enabled && (
        <div
          className={classNames(
            styles['panel-form-area'],
            'flex flex-col gap-[20px] ',
          )}
        >
          <div className="flex items-center">
            <span className={styles['panel-form-area-label']}>
              {$i18n.get({
                id: 'main.pages.App.Workflow.components.RetryForm.index.maxRetryTimes',
                dm: '最大重试次数',
              })}
            </span>
            <SliderSelector
              disabled={disabled}
              value={value.max_retries}
              onChange={(val) =>
                onChange({ ...value, max_retries: val as number })
              }
              step={1}
              className={classNames(
                'flex-1 ml-[20px] mr-[12px]',
                styles['panel-slider-selector'],
              )}
              min={1}
              max={10}
              inputNumberWrapperStyle={{ width: 64 }}
            />
          </div>
          <div className="flex items-center">
            <span className={styles['panel-form-area-label']}>
              {$i18n.get({
                id: 'main.pages.App.Workflow.components.RetryForm.index.retryInterval',
                dm: '重试间隔',
              })}
            </span>
            <SliderSelector
              disabled={disabled}
              className={classNames(
                'flex-1 ml-[20px]',
                styles['panel-slider-selector'],
              )}
              value={value.retry_interval}
              onChange={(val) =>
                onChange({ ...value, retry_interval: val as number })
              }
              step={100}
              min={300}
              max={10000}
              inputNumberWrapperStyle={{ width: 64 }}
            />
          </div>
        </div>
      )}
    </Flex>
  );
}
