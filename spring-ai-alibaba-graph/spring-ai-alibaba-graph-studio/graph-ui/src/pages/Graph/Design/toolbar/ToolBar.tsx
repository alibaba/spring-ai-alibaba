import { Icon } from '@iconify/react';
import { useReactFlow, useViewport } from '@xyflow/react';
import React from 'react';
import { OperationMode } from '../types';
import './toolbar.less';

interface Props {
  name?: string;
  handleUndo?: () => void;
  handleRedo?: () => void;
  history?: [];
  operationMode: OperationMode;
  changeOperationMode: (mode: OperationMode) => void;
}

const ToolBar: React.FC<Props> = (props) => {
  const { handleUndo, handleRedo, operationMode, changeOperationMode } = props;
  const { zoomIn, zoomOut, zoomTo, fitView } = useReactFlow();
  const { zoom } = useViewport();

  const buttonsDisabled = {
    undo: !Boolean(handleUndo),
    redo: !Boolean(handleRedo),
    history: Boolean(history && history.length),
  };

  return (
    <div className="toolbar">
      <div className="zoom-wrapper item-wapper">
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
      <div className="undo-redo-wrapper item-wapper">
        <div
          className={
            buttonsDisabled.undo ? 'icon-wrapper-disabled' : 'icon-wrapper'
          }
          onClick={() => !buttonsDisabled.undo && handleUndo && handleUndo()}
        >
          <Icon className="icon" icon="iconamoon:do-redo-light"></Icon>
        </div>
        <div
          className={
            buttonsDisabled.redo ? 'icon-wrapper-disabled' : 'icon-wrapper'
          }
          onClick={() => !buttonsDisabled.redo && handleRedo && handleRedo()}
        >
          <Icon className="icon" icon="iconamoon:do-undo-light"></Icon>
        </div>
        <div className="split"></div>
        <div
          className={
            buttonsDisabled.history ? 'icon-wrapper-disabled' : 'icon-wrapper'
          }
          onClick={() => !buttonsDisabled.history && handleRedo && handleRedo()}
        >
          <Icon className="icon" icon="iconamoon:history-light"></Icon>
        </div>
      </div>
      <div className="control-wrapper item-wapper">
        <div className="icon-wrapper-disabled">
          <Icon className="icon" icon="material-symbols:add-box-rounded"></Icon>
        </div>
        <div className="icon-wrapper-disabled">
          <Icon
            className="icon"
            icon="material-symbols:note-alt-outline"
          ></Icon>
        </div>
        <div className="split"></div>
        <div
          className={
            operationMode === 'hand' ? 'icon-wrapper-selected' : 'icon-wrapper'
          }
          onClick={() => changeOperationMode('hand')}
        >
          <Icon className="icon" icon="tabler:hand-stop"></Icon>
        </div>
        <div
          className={
            operationMode === 'pointer'
              ? 'icon-wrapper-selected'
              : 'icon-wrapper '
          }
          onClick={() => changeOperationMode('pointer')}
        >
          <Icon className="icon" icon="tabler:pointer"></Icon>
        </div>
        <div className="split"></div>
        <div className="icon-wrapper-disabled">
          <Icon className="icon" icon="ic:sharp-format-align-left"></Icon>
        </div>
      </div>
    </div>
  );
};
export default ToolBar;
