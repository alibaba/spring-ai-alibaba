import { Icon } from '@iconify/react';
import { Affix, Card, Col, Row } from 'antd';
import React, { useState } from 'react';
import './toolbar.less';

interface Props {
  name?: string;
}

const ToolBar: React.FC<Props> = () => {
  const [toolbarBottom] = useState<number>(12);

  const toolList = [
    {
      type: 'edit',
      options: [
        {
          title: 'undo',
          icon: 'iconamoon:do-undo-light',
        },
        {
          title: 'redo',
          icon: 'iconamoon:do-redo-light',
          split: true,
        },

        {
          title: 'history',
          icon: 'material-symbols:history-toggle-off',
          split: false,
        },
      ],
    },
    {
      type: 'map',
    },
    {
      type: 'graph',
    },
  ];
  return (
    <Affix offsetBottom={toolbarBottom}>
      <Row gutter={12}>
        {toolList.map((group) => {
          let opts = group?.options || [];
          return (
            <>
              <Col lg={opts.length || 2} sm={opts.length * 2 || 2}>
                <Card className="toolbar-card">
                  {opts.map((tool) => {
                    return (
                      <>
                        <Icon className="icon" icon={tool.icon}></Icon>
                        {tool.split ? (
                          <div className="split">
                            <div className="item"></div>
                          </div>
                        ) : (
                          ''
                        )}
                      </>
                    );
                  })}
                </Card>
              </Col>
            </>
          );
        })}
      </Row>
    </Affix>
  );
};
export default ToolBar;
