import $i18n from '@/i18n';
import { IconFont } from '@spark-ai/design';
import { Typography } from 'antd';
import React, { memo } from 'react';
import { IParameterExtractorNodeParam } from '../../types/flow';
import './index.less';

interface IExtractParamItemProps {
  onDelete: () => void;
  onEdit: () => void;
  data: IParameterExtractorNodeParam['extract_params'][number];
}

export default memo(function ExtractParamItem(props: IExtractParamItemProps) {
  const { data, onDelete, onEdit } = props;
  return (
    <div className="spark-flow-panel-extract-param-item flex items-center gap-[16px]">
      <div className="flex gap-[4px] items-center">
        <IconFont type="spark-intervention-line" />
        <span>{data.key}</span>
        <span className="spark-flow-panel-extract-param-item-type">{`(${data.type})`}</span>
      </div>
      <Typography.Text
        ellipsis={{ tooltip: data.desc }}
        className="flex-1 w-[1px]"
      >
        {data.desc}
      </Typography.Text>
      <div>
        <div className="spark-flow-panel-extract-param-item-info">
          {data.required
            ? $i18n.get({
                id: 'spark-flow.demos.spark-flow-1.components.ExtractParamItem.index.required',
                dm: '必填',
              })
            : $i18n.get({
                id: 'spark-flow.demos.spark-flow-1.components.ExtractParamItem.index.notRequired',
                dm: '非必填',
              })}
        </div>
        <div className="flex gap-[8px] spark-flow-panel-extract-param-item-actions">
          <IconFont onClick={onEdit} isCursorPointer type="spark-edit-line" />

          <IconFont
            onClick={onDelete}
            isCursorPointer
            type="spark-delete-line"
          />
        </div>
      </div>
    </div>
  );
});
