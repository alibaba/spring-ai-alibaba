import { Icon } from '@iconify/react';
import { useReactFlow, useViewport } from '@xyflow/react';
import React from 'react';
import './toolbar.less';

interface Props {
  name?: string;
}

const ToolBar: React.FC<Props> = () => {
  const { zoomIn, zoomOut, zoomTo, fitView } = useReactFlow();
  const { zoom } = useViewport();

  return (
    <div className="toolbar">
      <div className="zoom-wrapper">
        <div
          className="icon-wrapper"
          onClick={(e) => {
            e.stopPropagation();
            zoomOut();
          }}
        >
          <Icon className="icon" icon="iconamoon:zoom-out-light"></Icon>
        </div>
        <div>{parseFloat(`${zoom * 100}`).toFixed(0)}%</div>
        <div
          className="icon-wrapper"
          onClick={(e) => {
            e.stopPropagation();
            zoomIn();
          }}
        >
          <Icon className="icon" icon="iconamoon:zoom-in-light"></Icon>
        </div>
      </div>
    </div>
  );
};
export default ToolBar;
