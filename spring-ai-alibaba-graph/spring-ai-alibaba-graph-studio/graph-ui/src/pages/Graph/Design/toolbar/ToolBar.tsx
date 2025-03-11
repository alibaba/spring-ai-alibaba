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
