import { IconFont } from '@spark-ai/design';
import { Popover } from 'antd';
import React, { memo } from 'react';
import './index.less';

interface IProps {
  tip: string;
}

export default memo(function InfoIcon(props: IProps) {
  return (
    <Popover content={props.tip} destroyTooltipOnHide>
      <IconFont className={'info-icon'} size="small" type="spark-info-line" />
    </Popover>
  );
});
