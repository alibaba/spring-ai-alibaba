import ToolBar, { ToolType } from '@/pages/Graph/Design/toolbar/ToolBar';
import { Affix } from 'antd';
import React, { useState } from 'react';
import './toolbar.less';

interface Props {
  name?: string;
  reLayoutCallback: any;
  viewport: any;
}

const BottomToolBar: React.FC<Props> = ({ reLayoutCallback, viewport }) => {
  const [toolbarBottom] = useState<number>(12);

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
