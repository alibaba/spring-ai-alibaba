import classNames from 'classnames';
import React, { memo } from 'react';
import CustomIcon from '../CustomIcon';

interface IPanelContainerProps {
  title?: React.ReactNode;
  right?: React.ReactNode;
  children: React.ReactNode;
  onClose?: () => void;
  noPadding?: boolean;
  hiddenRight?: boolean;
  headerBottom?: React.ReactNode;
}
export default memo(function PanelContainer(props: IPanelContainerProps) {
  return (
    <div className="spark-flow-panel h-full">
      <div className="spark-flow-panel-header">
        <div className="flex-justify-between">
          <div className="spark-flow-panel-title flex-1">{props.title}</div>
          {!props.hiddenRight && (
            <div className="flex items-center gap-[8px]">
              {props.right}
              <div
                onClick={props.onClose}
                className="spark-flow-operator-icon-with-bg size-[32px] rounded-[6px] flex-center"
              >
                <CustomIcon
                  className="text-[24px] spark-flow-node-action-btn"
                  type="spark-false-line"
                />
              </div>
            </div>
          )}
        </div>
        {props.headerBottom}
      </div>
      <div
        className={classNames('spark-flow-panel-content', {
          'p-[20px]': !props.noPadding,
        })}
      >
        {props.children}
      </div>
    </div>
  );
});
