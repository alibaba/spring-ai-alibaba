import { Icon } from '@iconify/react';
import { useReactFlow, useViewport } from '@xyflow/react';
import type { MenuProps } from 'antd';
import { Dropdown } from 'antd';
import React from 'react';
import { OperationMode } from '../types';
import './toolbar.less';

enum ZoomType {
  zoomIn = 'zoomIn',
  zoomOut = 'zoomOut',
  zoomToFit = 'zoomToFit',
  zoomTo25 = 'zoomTo25',
  zoomTo50 = 'zoomTo50',
  zoomTo75 = 'zoomTo75',
  zoomTo100 = 'zoomTo100',
  zoomTo200 = 'zoomTo200',
}

const ZOOM_IN_OUT_OPTIONS = [
  {
    key: ZoomType.zoomTo200,
    text: '200%',
    value: 2,
  },
  {
    key: ZoomType.zoomTo100,
    text: '100%',
    value: 1,
  },
  {
    key: ZoomType.zoomTo75,
    text: '75%',
    value: 0.75,
  },
  {
    key: ZoomType.zoomTo50,
    text: '50%',
    value: 0.5,
  },
  {
    key: ZoomType.zoomTo25,
    text: '25%',
    value: 0.25,
  },
  {
    key: 'divider',
    text: null,
  },
  {
    key: ZoomType.zoomToFit,
    text: 'Zoom To Fit',
  },
];

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

  const options: MenuProps['items'] = ZOOM_IN_OUT_OPTIONS.map((item, index) => {
    const option = { key: item.key } as MenuProps['items'];
    if (item.key === 'divider') {
      option.type = 'divider';
    } else if (index !== ZOOM_IN_OUT_OPTIONS.length - 1) {
      option.label = <a onClick={() => zoomTo(item.value)}>{item.text}</a>;
    } else {
      option.label = <a onClick={() => fitView()}>{item.text}</a>;
    }
    return option;
  });

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
        <Dropdown menu={{ items: options }} trigger={['click']}>
          <div className="text-wrapper">
            {parseFloat(`${zoom * 100}`).toFixed(0)}%
          </div>
        </Dropdown>
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
