import { useStore } from '@/flow/context';
import $i18n from '@/i18n';
import { Popover, Tooltip } from 'antd';
import classNames from 'classnames';
import React, { memo, useMemo } from 'react';
import CustomIcon from '../CustomIcon';

const MODEL_OPTIONS = [
  {
    label: $i18n.get({
      id: 'spark-flow.components.FlowTools.TouchModeBtn.mouseFriendlyMode',
      dm: '鼠标友好模式',
    }),
    desc: $i18n.get({
      id: 'spark-flow.components.FlowTools.TouchModeBtn.mouseLeftDragCanvasZoom',
      dm: '鼠标左键拖动画布，滚轮缩放',
    }),
    code: 'mouse',
    iconType: 'spark-mouse-line',
  },
  {
    label: $i18n.get({
      id: 'spark-flow.components.FlowTools.TouchModeBtn.touchpadFriendlyMode',
      dm: '触控板友好模式',
    }),
    desc: $i18n.get({
      id: 'spark-flow.components.FlowTools.TouchModeBtn.twoFingersMoveDragTwoFingersOpenZoom',
      dm: '双指同向移动进行拖动，双指开合进行缩放',
    }),
    code: 'touch',
    iconType: 'spark-trackpad-line',
  },
];

const TouchModeBtn = () => {
  const interactiveMode = useStore((state) => state.interactiveMode);
  const setInteractiveMode = useStore((state) => state.setInteractiveMode);

  const memoTouchModeSelect = useMemo(() => {
    return (
      <div className="flex flex-col gap-[4px]">
        {MODEL_OPTIONS.map((item) => {
          const isActive = interactiveMode === item.code;
          return (
            <div
              key={item.code}
              className={classNames(
                'flex items-center gap-[12px] spark-flow-touch-mode-select-item',
                {
                  ['spark-flow-touch-mode-select-item-active']: isActive,
                },
              )}
              onClick={() => {
                if (isActive) return;
                setInteractiveMode(item.code as 'touch' | 'mouse');
              }}
            >
              <div className="spark-flow-touch-mode-select-item-icon flex-center size-[36px]">
                <CustomIcon className="text-[24px]" type={item.iconType} />
              </div>
              <div className="w-0 flex-1">
                <div className="spark-flow-touch-mode-select-item-label">
                  {item.label}
                </div>
                <div className="spark-flow-touch-mode-select-item-desc">
                  {item.desc}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    );
  }, [interactiveMode, setInteractiveMode]);

  const currentInteractiveMode = MODEL_OPTIONS.find(
    (i) => i.code === interactiveMode,
  );

  return (
    <Popover
      placement="top"
      arrow={false}
      destroyTooltipOnHide
      content={memoTouchModeSelect}
      getPopupContainer={(ele) => ele}
      trigger="click"
    >
      <Tooltip title={currentInteractiveMode?.label}>
        <div className="spark-flow-tool-icon-btn spark-flow-touch-mode-btn-container h-[32px] flex items-center gap-[4px] p-[6px]">
          <CustomIcon type={currentInteractiveMode?.iconType as string} />
          <CustomIcon size="small" type="spark-down-line" />
        </div>
      </Tooltip>
    </Popover>
  );
};

export default memo(TouchModeBtn);
