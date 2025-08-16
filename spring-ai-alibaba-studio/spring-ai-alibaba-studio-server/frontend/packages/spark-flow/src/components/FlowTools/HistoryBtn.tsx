import { useNodesReadOnly } from '@/hooks';
import { useHistory } from '@/hooks/useHistory';
import $i18n from '@/i18n';
import { isEventInInput, transformToMacKey } from '@/utils';
import { useKeyPress } from 'ahooks';
import { Tooltip } from 'antd';
import classNames from 'classnames';
import React, { memo } from 'react';
import CustomIcon from '../CustomIcon';

export default memo(function HistoryBtn() {
  const { canUndo, canRedo, undo, redo } = useHistory();
  const { nodesReadOnly } = useNodesReadOnly();

  useKeyPress(
    [`${transformToMacKey('ctrl')}.z`],
    (event) => {
      if (isEventInInput(event) || nodesReadOnly) return;
      undo();
    },
    { exactMatch: true },
  );

  useKeyPress(
    [`${transformToMacKey('ctrl')}.shift.z`],
    (event) => {
      if (isEventInInput(event) || nodesReadOnly) return;
      redo();
    },
    { exactMatch: true },
  );

  return (
    <div className="spark-flow-tools gap-[8px] items-center flex">
      <Tooltip
        destroyTooltipOnHide
        title={$i18n.get({
          id: 'spark-flow.components.FlowTools.HistoryBtn.undo',
          dm: '撤销',
        })}
      >
        <div
          className={classNames(
            'spark-flow-tool-icon-btn size-[32px] flex-center',
            {
              ['spark-flow-tool-icon-btn-disabled']: !canUndo || nodesReadOnly,
            },
          )}
          onClick={nodesReadOnly ? undefined : undo}
        >
          <CustomIcon className="text-[20px]" type="spark-undo-line" />
        </div>
      </Tooltip>
      <Tooltip
        destroyTooltipOnHide
        title={$i18n.get({
          id: 'spark-flow.components.FlowTools.HistoryBtn.redo',
          dm: '重做',
        })}
      >
        <div
          className={classNames(
            'spark-flow-tool-icon-btn size-[32px] flex-center',
            {
              ['spark-flow-tool-icon-btn-disabled']: !canRedo || nodesReadOnly,
            },
          )}
          onClick={nodesReadOnly ? undefined : redo}
        >
          <CustomIcon className="text-[20px]" type="spark-redo-line" />
        </div>
      </Tooltip>
    </div>
  );
});
