import { useFlowInteraction } from '@/hooks';
import { useFlowViewPort } from '@/hooks/useFlowViewPort';
import $i18n from '@/i18n';
import { transformToMacKey } from '@/utils';
import { useKeyPress } from 'ahooks';
import { Divider, Dropdown, Tooltip } from 'antd';
import React, { memo } from 'react';
import CustomIcon from '../CustomIcon';

export default memo(function ScaleBtn() {
  const { handleScale, scaleRate, handleSetScale } = useFlowViewPort();
  const { autoFitView } = useFlowInteraction();

  useKeyPress(
    [`${transformToMacKey('ctrl')}.equalsign`],
    (event) => {
      event.preventDefault();
      handleScale(1);
    },
    { useCapture: true, exactMatch: true },
  );

  useKeyPress(
    [`${transformToMacKey('ctrl')}.dash`],
    (event) => {
      event.preventDefault();
      handleScale(-1);
    },
    {
      useCapture: true,
      exactMatch: true,
    },
  );

  return (
    <>
      <Dropdown
        menu={{
          items: [
            { label: '200%', key: '200' },
            { label: '100%', key: '100' },
            { label: '50%', key: '50' },
            {
              label: $i18n.get({
                id: 'spark-flow.components.FlowTools.ScaleBtn.autoView',
                dm: '自适应试图',
              }),
              key: 'fit',
            },
          ],

          onClick: (e) => {
            if (e.key === 'fit') {
              autoFitView();
            } else {
              handleSetScale(parseInt(e.key) / 100);
            }
          },
        }}
        trigger={['click']}
        getPopupContainer={(ele) => ele}
      >
        <Tooltip
          title={$i18n.get({
            id: 'spark-flow.components.FlowTools.ScaleBtn.scale',
            dm: '缩放',
          })}
        >
          <div className="spark-flow-tool-icon-btn flex items-center gap-[4px]">
            <div className="p-[6px] h-[32px]">
              <CustomIcon
                onClick={(e) => {
                  e.stopPropagation();
                  handleScale(-1);
                }}
                className="cursor-pointer"
                type="spark-reduce-line"
              />
            </div>
            <span className={'spark-flow-tools-scale-rate'}>{scaleRate}%</span>
            <div className="p-[6px] h-[32px]">
              <CustomIcon
                onClick={(e) => {
                  e.stopPropagation();
                  handleScale(1);
                }}
                className="cursor-pointer text-[20px]"
                type="spark-amplify-line"
              />
            </div>
          </div>
        </Tooltip>
      </Dropdown>
      <Divider type="vertical" className="m-0" />
      <Tooltip
        title={$i18n.get({
          id: 'spark-flow.components.FlowTools.ScaleBtn.autoView',
          dm: '自适应试图',
        })}
      >
        <div
          onClick={autoFitView}
          className="spark-flow-tool-icon-btn size-[32px] flex-center"
        >
          <CustomIcon className="text-[20px]" type="spark-defaultSize-line" />
        </div>
      </Tooltip>
    </>
  );
});
