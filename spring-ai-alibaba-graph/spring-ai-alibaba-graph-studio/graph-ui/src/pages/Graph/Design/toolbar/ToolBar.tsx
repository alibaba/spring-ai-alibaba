import { Icon } from '@iconify/react';
import { useReactFlow, useViewport } from '@xyflow/react';
import React from 'react';
import './toolbar.less';

interface Props {
  name?: string;
  handleUndo?: () => void;
  handleRedo?: () => void;
  history?: [];
}

const ToolBar: React.FC<Props> = (props) => {
  const { handleUndo, handleRedo } = props;
  const { zoomIn, zoomOut, zoomTo, fitView } = useReactFlow();
  const { zoom } = useViewport();

  const buttonsDisabled = {
    undo: !Boolean(handleUndo),
    redo: !Boolean(handleRedo),
    history: Boolean(history && history.length),
  };

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
      <div className="undo-redo-wrapper">
        <div
          className="icon-wrapper"
          onClick={() => !buttonsDisabled.undo && handleUndo && handleUndo()}
        >
          <Icon
            className="icon"
            icon="iconamoon:do-redo-light"
            style={{ color: buttonsDisabled.undo ? '#d0d5dc' : '#676f83' }}
          ></Icon>
        </div>
        <div
          className="icon-wrapper"
          onClick={() => !buttonsDisabled.redo && handleRedo && handleRedo()}
        >
          <Icon
            className="icon"
            icon="iconamoon:do-undo-light"
            style={{ color: buttonsDisabled.redo ? '#d0d5dc' : '#676f83' }}
          ></Icon>
        </div>
        <div className="split"></div>
        <div
          className="icon-wrapper"
          onClick={() => !buttonsDisabled.history && handleRedo && handleRedo()}
        >
          <Icon
            className="icon"
            icon="iconamoon:history-light"
            style={{ color: buttonsDisabled.history ? '#d0d5dc' : '#676f83' }}
          ></Icon>
        </div>
      </div>
    </div>
  );
};
export default ToolBar;
