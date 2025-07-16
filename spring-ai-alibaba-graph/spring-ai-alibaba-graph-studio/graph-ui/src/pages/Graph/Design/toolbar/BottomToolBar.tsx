/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import ToolBar, { ToolType } from '@/pages/Graph/Design/toolbar/ToolBar';
import { useReactFlow, useViewport } from '@xyflow/react';
import type { MenuProps } from 'antd';
import { Affix, Dropdown } from 'antd';
import React, { useState } from 'react';
import { OperationMode, ZoomType } from '../types';
import './toolbar.less';

interface Props {
  name?: string;
  reLayoutCallback: any;
  changeOperationMode: (mode: OperationMode) => void;
  viewport: any;
}

const BottomToolBar: React.FC<Props> = (props: Props) => {
  const [toolbarBottom] = useState<number>(12);
  const { zoomIn, zoomOut, zoomTo } = useReactFlow();
  const { zoom } = useViewport();
  const { reLayoutCallback, changeOperationMode, viewport } = props;

  const handleZoomIn = (e: MouseEvent) => {
    e.stopPropagation();
    zoomIn();
  };
  const handleZoomOut = (e: MouseEvent) => {
    e.stopPropagation();
    zoomOut();
  };

  const zoomOptions = [
    {
      key: ZoomType.ZOOM_TO_200,
      text: '200%',
      value: 2,
    },
    {
      key: ZoomType.ZOOM_TO_150,
      text: '150%',
      value: 1.5,
    },
    {
      key: ZoomType.ZOOM_TO_100,
      text: '100%',
      value: 1,
    },
    {
      key: ZoomType.ZOOM_TO_75,
      text: '75%',
      value: 0.75,
    },
    {
      key: ZoomType.ZOOM_TO_50,
      text: '50%',
      value: 0.5,
    },
    {
      key: ZoomType.ZOOM_TO_25,
      text: '25%',
      value: 0.25,
    },
  ];

  const options: MenuProps['items'] = zoomOptions.map((item) => {
    const option = { key: item.key, label: <></> };
    option.label = <a onClick={() => zoomTo(item.value)}>{item.text}</a>;
    return option;
  });

  const toolList: ToolType[] = [
    {
      type: 'edit',
      options: [
        {
          title: 'undo',
          onClick: () => {},
          icon: 'iconamoon:do-undo-light',
        },
        {
          title: 'redo',
          onClick: () => {},
          icon: 'iconamoon:do-redo-light',
          split: false,
        },
      ],
    },
    {
      type: 'map',
      options: [
        {
          title: 're_layout',
          onClick: reLayoutCallback,
          icon: 'material-symbols:responsive-layout-outline-rounded',
          split: false,
        },
      ],
    },
    {
      type: 'zoom',
      options: [
        {
          title: 'zoom_in',
          onClick: handleZoomIn,
          icon: 'iconamoon:zoom-in-light',
          split: true,
        },
        {
          title: 'zoom',
          text: (
            <Dropdown
              menu={{ items: options }}
              trigger={['click']}
              placement="top"
            >
              <div style={{ width: 40 }}>
                {parseFloat(`${zoom * 100}`).toFixed(0)}%
              </div>
            </Dropdown>
          ),
          split: true,
        },
        {
          title: 'zoom_out',
          onClick: handleZoomOut,
          icon: 'iconamoon:zoom-out-light',
          split: false,
        },
      ],
    },
    {
      type: 'mode',
      options: [
        {
          title: 'hand',
          onClick: () => changeOperationMode(OperationMode.HAND),
          icon: 'tabler:hand-stop',
          split: true,
        },
        {
          title: 'pointer',
          onClick: () => changeOperationMode(OperationMode.POINT),
          icon: 'tabler:pointer',
          split: false,
        },
      ],
    },
    {
      type: 'state',
      options: [
        {
          title: 'x',
          onClick: reLayoutCallback,
          text: `x:${viewport.y.toPrecision(4)}`,
          split: true,
        },
        {
          title: 'y',
          onClick: reLayoutCallback,
          text: ` y:${viewport.y.toPrecision(4)}`,
          split: true,
        },
        {
          title: 'zoom',
          onClick: reLayoutCallback,
          text: `zoom:${viewport.zoom.toPrecision(4)}`,
          split: false,
        },
      ],
    },
  ];
  return (
    <Affix offsetBottom={toolbarBottom} style={{}}>
      <ToolBar toolList={toolList}></ToolBar>
    </Affix>
  );
};
export default BottomToolBar;
