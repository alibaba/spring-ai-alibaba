import { useFlowInteraction, useNodesReadOnly } from '@/hooks';
import $i18n from '@/i18n';
import { Tooltip } from 'antd';
import classNames from 'classnames';
import React, { memo } from 'react';
import CustomIcon from '../CustomIcon';

export default memo(function LayoutBtn() {
  const { onLayout } = useFlowInteraction();
  const { nodesReadOnly } = useNodesReadOnly();
  return (
    <Tooltip
      rootClassName="spark-flow-tool-tooltip"
      destroyTooltipOnHide
      title={$i18n.get({
        id: 'spark-flow.components.FlowTools.LayoutBtn.optimizeLayout',
        dm: '布局优化',
      })}
    >
      <div
        onClick={onLayout}
        className={classNames(
          'spark-flow-tool-icon-btn size-[32px] flex-center',
          {
            'spark-flow-tool-icon-btn-disabled': nodesReadOnly,
          },
        )}
      >
        <CustomIcon className="text-[20px]" type="spark-cardAddition-line" />
      </div>
    </Tooltip>
  );
});
