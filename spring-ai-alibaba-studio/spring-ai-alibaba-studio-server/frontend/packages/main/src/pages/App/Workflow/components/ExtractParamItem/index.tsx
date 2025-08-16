import $i18n from '@/i18n';
import { IconFont } from '@spark-ai/design';
import { Typography } from 'antd';
import classNames from 'classnames';
import { memo } from 'react';
import { IParameterExtractorNodeParam } from '../../types';
import styles from './index.module.less';

interface IExtractParamItemProps {
  onDelete: () => void;
  onEdit: () => void;
  data: IParameterExtractorNodeParam['extract_params'][number];
  disabled?: boolean;
}

export default memo(function ExtractParamItem(props: IExtractParamItemProps) {
  const { data, onDelete, onEdit, disabled } = props;
  return (
    <div
      className={classNames(
        styles['extract-param-item'],
        'flex items-center gap-4',
      )}
    >
      <div className="flex gap-[4px] items-center">
        <Typography.Text
          ellipsis={{ tooltip: data.key }}
          className={styles['extract-param-item-key']}
        >
          {data.key}
        </Typography.Text>
        <span className={styles['extract-param-item-type']}>
          {`[${data.type}]`}
        </span>
      </div>
      <Typography.Text
        ellipsis={{ tooltip: data.desc }}
        className={classNames(
          'flex-1 w-[1px]',
          styles['extract-param-item-desc'],
        )}
      >
        {data.desc}
      </Typography.Text>
      <div>
        <div className={styles['extract-param-item-info']}>
          {data.required
            ? $i18n.get({
                id: 'main.pages.App.Workflow.components.ExtractParamItem.index.required',
                dm: '必填',
              })
            : $i18n.get({
                id: 'main.pages.App.Workflow.components.ExtractParamItem.index.optional',
                dm: '非必填',
              })}
        </div>
        <div
          className={classNames(
            'flex gap-[8px]',
            styles['extract-param-item-actions'],
          )}
        >
          <IconFont
            onClick={onEdit}
            className={disabled ? 'disabled-icon-btn' : ''}
            type="spark-edit-line"
            isCursorPointer={!disabled}
          />

          <IconFont
            onClick={onDelete}
            className={disabled ? 'disabled-icon-btn' : ''}
            type="spark-delete-line"
            isCursorPointer={!disabled}
          />
        </div>
      </div>
    </div>
  );
});
