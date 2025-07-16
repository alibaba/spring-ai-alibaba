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

import { Icon } from '@iconify/react';
import { Card, Flex } from 'antd';
import React from 'react';
import './toolbar.less';

export type ToolType = {
  type: string;
  options: {
    title: string;
    onClick?: any;
    icon?: string;
    text?: any;
    split?: boolean;
  }[];
};
interface Props {
  toolList: ToolType[];
}

const ToolBar: React.FC<Props> = ({ toolList }) => {
  return (
    <Flex style={{ marginLeft: '20px' }}>
      {toolList.map((group) => {
        let opts = group?.options || [];
        return (
          <div key={group.type}>
            <Card className="toolbar-card">
              <Flex align={'stretch'}>
                {opts.map((tool) => {
                  return (
                    <div onClick={tool.onClick} key={tool.title}>
                      <Flex className="toolbar-body" gap={5}>
                        {tool.icon ? (
                          <Icon className="icon" icon={tool.icon}></Icon>
                        ) : (
                          ''
                        )}
                        {tool.text ? (
                          <div className="text">{tool.text}</div>
                        ) : (
                          ''
                        )}
                        {tool.split ? (
                          <div className="split">
                            <div className="item"></div>
                          </div>
                        ) : (
                          ''
                        )}
                      </Flex>
                    </div>
                  );
                })}
              </Flex>
            </Card>
          </div>
        );
      })}
    </Flex>
  );
};
export default ToolBar;
