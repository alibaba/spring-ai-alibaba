/**
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect } from 'react';
import { Button, Card, Col, Drawer, Row, Table, Tree } from 'antd';
import { CarryOutOutlined, FormOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

// const input = '{"input":"What is a document loader?","chat_history":[]}';
// const output = '{"output":"A document loader is a tool used to load and process documents for various tasks such as text analysis, information extraction, and more."}';
const preStyle: any = {
    whiteSpace: 'pre-wrap', // This will allow the text to wrap
    wordWrap: 'break-word', // This will break long words
    overflow: 'auto', // This will provide a scrollbar if needed
};
const TraceDetailComp = ({ record, open, setOpen }) => {
    const { t, i18n } = useTranslation();
    const [traceNodeInfo, setTraceNodeInfo] = React.useState({ title: 'chat_client', input: 'What is a document loader?', output: 'A document loader is a tool used to load and process documents for various tasks such as text analysis, information extraction, and more.' } as any);

    const treeContainerStyle = {
        whiteSpace: 'nowrap',
        overflowX: 'auto',
        width: '100%', // 容器宽度，可以根据需要调整
        borderRight: '1px solid #d9d9d9', // 添加竖线
        paddingRight: '10px', // 若需要留一些空间，避免内容贴住边界
    };

    useEffect(() => {
        setTraceNodeInfo({ input: record.input, output: record.output, title: record.model });
    }, [record]);

    const buildTraceNodeInfo = (traceNodeInfo) => {
        const arrayAttributes = [] as any;
        Object.keys(traceNodeInfo.attributes || []).forEach((key) => {
            arrayAttributes.push({ name: key, value: traceNodeInfo.attributes[key] });
        });
        const newData = JSON.parse(JSON.stringify(traceNodeInfo));
        if (newData.input.length == 0) {
            newData.input = record.input;
        }
        if (newData.output.length == 0) {
            newData.output = record.output;
        }
        newData.attributes = arrayAttributes;
        setTraceNodeInfo(newData);
    };
    return (
      <div>
        <Drawer title="Trace Detail" width={'90%'} onClose={() => setOpen(false)} open={open}>
          <Row>
            <Col span={6} style={treeContainerStyle as any}>
              <Tree
                onSelect={(e: any, info) => buildTraceNodeInfo(info.selectedNodes[0])}
                showLine
                showIcon
                defaultExpandAll
                treeData={[record.traceDetail]}
              />
            </Col>
            <Col span={18}>
              <Card title={traceNodeInfo.title} extra={<Button disabled>{t('gotoDebug')}</Button>}>
                <Row>
                  <Col span={16} style={{ paddingRight: 10 }}>
                    <Card title={'Input'} style={{ marginTop: 10 }}><pre style={preStyle}>{JSON.stringify(traceNodeInfo.input, null, 2)}</pre></Card>
                    <Card title={'Output'} style={{ marginTop: 10 }}><pre style={preStyle}>{JSON.stringify(traceNodeInfo.output, null, 2)}</pre></Card>
                    <Card title={'Messages'} style={{ marginTop: 10 }}>
                      aaa
                    </Card>
                  </Col>
                  <Col span={7}>
                    <Table pagination={false} columns={[{ title: 'Name', dataIndex: 'name' }, { title: 'Value', dataIndex: 'value' }]} dataSource={traceNodeInfo?.attributes || []} />
                  </Col>
                </Row>
              </Card>
            </Col>

          </Row>
        </Drawer>
      </div>
    );
};

export default TraceDetailComp;