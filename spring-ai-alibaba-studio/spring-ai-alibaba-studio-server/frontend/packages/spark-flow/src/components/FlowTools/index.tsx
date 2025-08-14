import { Divider } from 'antd';
import React, { memo } from 'react';
import AddNodeBtn from './AddNodeBtn';
import HistoryBtn from './HistoryBtn';
import './index.less';
import LayoutBtn from './LayoutBtn';
import MiniMapBtn from './MiniMapBtn';
import ScaleBtn from './ScaleBtn';
import ShortKeyBtn from './ShortKeyBtn';
import TouchModeBtn from './TouchModeBtn';

export default memo(function FlowTools() {
  return (
    <div
      id="spark-flow-bottom-tools-container"
      className="nopan absolute items-center flex left-[16px] bottom-[16px] gap-[12px]"
    >
      <div className="spark-flow-tools gap-[8px] items-center flex">
        <MiniMapBtn />
        <Divider type="vertical" className="m-0" />
        <ScaleBtn />
      </div>
      <div className="spark-flow-tools gap-[8px] items-center flex">
        <AddNodeBtn />
        <LayoutBtn />
        <Divider type="vertical" className="m-0" />
        <TouchModeBtn />
        <Divider type="vertical" className="m-0" />
        <ShortKeyBtn />
      </div>
      <HistoryBtn />
    </div>
  );
});
