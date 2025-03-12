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
