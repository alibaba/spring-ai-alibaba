import { useStore } from '@/flow/context';
import classNames from 'classnames';
import React, { memo } from 'react';
import CustomIcon from '../CustomIcon';
import './index.less';

export default memo(function FlowIcon({
  nodeType,
  size = 'default',
  showBg = true,
  noWidth = false,
}: {
  nodeType: string;
  size?: 'small' | 'default';
  showBg?: boolean;
  noWidth?: boolean;
}) {
  const nodeSchemaMap = useStore((store) => store.nodeSchemaMap);
  return (
    <div
      style={{
        backgroundColor: showBg
          ? nodeSchemaMap[nodeType]?.bgColor
          : 'transparent',
      }}
      className={classNames('spark-flow-node-icon-wrap', {
        ['size-[24px]']: size === 'default' && !noWidth,
        ['size-[16px]']: size === 'small' && !noWidth,
        ['spark-node-icon-small']: size === 'small' && !noWidth,
        ['spark-flow-node-icon-no-bg']: !showBg,
      })}
    >
      <CustomIcon
        className="spark-flow-type-icon"
        size="small"
        type={nodeSchemaMap[nodeType].iconType}
      />
    </div>
  );
});
