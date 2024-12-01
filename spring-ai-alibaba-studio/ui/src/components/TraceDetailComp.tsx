import React from 'react';
import { Card, Col, Drawer, Row, Tree } from 'antd';
import { CarryOutOutlined, FormOutlined } from '@ant-design/icons';

const input = '{"input":"What is a document loader?","chat_history":[]}';
const output = '{"output":"A document loader is a tool used to load and process documents for various tasks such as text analysis, information extraction, and more."}';
const preStyle: any = {
    whiteSpace: 'pre-wrap', // This will allow the text to wrap
    wordWrap: 'break-word', // This will break long words
    overflow: 'auto', // This will provide a scrollbar if needed
};
const TraceDetailComp = ({ open, setOpen }) => {
    const [selectedKey, setSelectedKey] = React.useState('Chat Model');
    const treeData = [
        {
            title: 'Chat Client',
            key: 'Chat Client',
            icon: <CarryOutOutlined />,
            children: [
                {
                    title: 'Chat Model',
                    key: 'Chat Model',
                    icon: <CarryOutOutlined />,
                    children: [
                        { title: 'template', key: 'template', icon: <CarryOutOutlined /> },
                        {
                            title: 'tools',
                            key: 'tools',
                            icon: <CarryOutOutlined />,
                        },
                        { title: 'leaf', key: '0-0-0-2', icon: <CarryOutOutlined /> },
                    ],
                },
                {
                    title: 'parent 1-1',
                    key: '0-0-1',
                    icon: <CarryOutOutlined />,
                    children: [{ title: 'leaf', key: '0-0-1-0', icon: <CarryOutOutlined /> }],
                },
                {
                    title: 'parent 1-2',
                    key: '0-0-2',
                    icon: <CarryOutOutlined />,
                    children: [
                        { title: 'leaf', key: '0-0-2-0', icon: <CarryOutOutlined /> },
                        {
                            title: 'leaf',
                            key: '0-0-2-1',
                            icon: <CarryOutOutlined />,
                            switcherIcon: <FormOutlined />,
                        },
                    ],
                },
            ],
        },
    ];
    return (
      <div>
        <Drawer title="Trace Detail" width={'90%'} onClose={() => setOpen(false)} open={open}>
          <Row>
            <Col span={6}>
              <Tree
                onSelect={(e: any) => setSelectedKey(e[0])}
                showLine
                showIcon
                defaultExpandedKeys={['Chat Model']}
                treeData={treeData}
              />
            </Col>
            <Col span={18}>
              <Card title={selectedKey}>
                <Card title={'Input'} style={{ marginTop: 10 }}><pre style={preStyle}>{JSON.stringify(JSON.parse(input), null, 2)}</pre></Card>
                <Card title={'Output'} style={{ marginTop: 10 }}><pre style={preStyle}>{JSON.stringify(JSON.parse(output), null, 2)}</pre></Card>
                <Card title={'Messages'} style={{ marginTop: 10 }}>
                  aaa
                </Card>
              </Card>

            </Col>
          </Row>
        </Drawer>
      </div>
    );
};

export default TraceDetailComp;