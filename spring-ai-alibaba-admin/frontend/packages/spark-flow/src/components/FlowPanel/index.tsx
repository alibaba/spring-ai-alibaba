import React, { memo } from 'react';
import './index.less';

interface IFlowPanel {
  children?: React.ReactNode[] | React.ReactNode;
}

export default memo(function FlowPanel(props: IFlowPanel) {
  return (
    <div className="spark-flow-panel-group absolute flex gap-[16px] flex-nowrap">
      {props.children}
    </div>
  );
});
