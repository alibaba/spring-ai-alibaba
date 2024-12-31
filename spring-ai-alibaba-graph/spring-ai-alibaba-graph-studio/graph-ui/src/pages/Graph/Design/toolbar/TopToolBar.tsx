import ToolBar, { ToolType } from '@/pages/Graph/Design/toolbar/ToolBar';
import { FormattedMessage } from '@@/exports';
import { Icon } from '@iconify/react';
import { Affix } from 'antd';
import React, { useState } from 'react';
import './toolbar.less';

interface Props {
  name?: string;
  reLayoutCallback?: any;
  viewport?: any;
}

const TopToolBar: React.FC<Props> = () => {
  const [toolbarTop] = useState<number>(12);
  const toolList: ToolType[] = [
    {
      type: 'dsl-import',
      options: [
        {
          title: 'import',
          onClick: () => {
            console.log(2222);
          },
          text: (
            <>
              <div className="button">
                <Icon className="icon" icon={'prime:file-import'}></Icon>
                <FormattedMessage id="page.graph.toolbar.import-dsl"></FormattedMessage>
              </div>
            </>
          ),
        },
      ],
    },
    {
      type: 'dsl-export',
      options: [
        {
          title: 'export',
          onClick: () => {
            console.log(3333);
          },
          split: false,
          text: (
            <>
              <div className="button">
                <Icon className="icon" icon={'prime:file-export'}></Icon>
                <FormattedMessage id="page.graph.toolbar.export-dsl"></FormattedMessage>
              </div>
            </>
          ),
        },
      ],
    },
  ];
  return (
    <Affix
      className="toolbar-wrapper"
      offsetTop={toolbarTop}
      style={{
        cursor: 'pointer',
      }}
    >
      <ToolBar toolList={toolList}></ToolBar>
    </Affix>
  );
};
export default TopToolBar;
