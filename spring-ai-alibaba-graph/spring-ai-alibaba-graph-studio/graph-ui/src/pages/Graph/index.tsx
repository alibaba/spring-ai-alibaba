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

import { FormattedMessage } from '@@/exports';
import { PageContainer } from '@ant-design/pro-components';
import { history } from '@umijs/max';
import { Button, Flex, Form, Input, Modal, Table } from 'antd';
import FormItem from 'antd/es/form/FormItem';
import { useState } from 'react';
import styles from './index.less';

const GraphIndex: React.FC = () => {
  const [editFormState, setEditFormState] = useState(false);
  const toDesign = (prop: any) => {
    history.push('graph/design?id=' + prop.id);
  };
  const columns: any = [
    {
      title: <FormattedMessage id={'page.graph.sn'} />,
      dataIndex: '',
      rowScope: 'row',
      render: (text: any, record: any, index: number) => {
        return index + 1;
      },
    },
    {
      title: <FormattedMessage id={'page.graph.graphName'} />,
      dataIndex: 'name',
    },
    {
      title: <FormattedMessage id={'page.graph.version'} />,
      dataIndex: 'version',
    },
    {
      title: <FormattedMessage id={'page.graph.graphDesc'} />,
      dataIndex: 'desc',
    },
    {
      title: <FormattedMessage id={'page.graph.createTime'} />,
      dataIndex: 'createTime',
    },
    {
      title: <FormattedMessage id={'page.graph.updateTime'} />,
      dataIndex: 'updateTime',
    },
    {
      title: <FormattedMessage id={'page.graph.option'} />,
      dataIndex: '',
      key: 'x',
      render: (_: any, record: any) => {
        return (
          <>
            <Flex gap="small">
              <Button
                type="primary"
                onClick={() => {
                  setEditFormState(true);
                }}
                size="small"
              >
                <FormattedMessage id={'page.graph.editMeta'} />
              </Button>
              <Button
                type="primary"
                size="small"
                onClick={() => toDesign(record)}
              >
                <FormattedMessage id={'page.graph.design'} />
              </Button>
              <Button type="dashed" size="small">
                <FormattedMessage id={'page.graph.genCode'} />
              </Button>
              <Button type="dashed" size="small">
                <FormattedMessage id={'page.graph.genProject'} />
              </Button>
              <Button type="primary" size="small" danger>
                <FormattedMessage id={'page.graph.delete'} />
              </Button>
            </Flex>
          </>
        );
      },
    },
  ];
  const data = [
    {
      id: '1',
      name: '示例流程图',
      version: '1.0',
      groupId: 'com.demo.ai.flow',
      artifactId: 'Demo',
      springAIAlibabaVersion: '1.0.0.1',
      jdk: 'JDK17',
      createTime: '2024-12-01 12:10:09',
      updateTime: '2024-12-01 12:10:09',
      desc: '这是一个示例',
    },
  ];

  const initForm = data[0];

  const [searchForm] = Form.useForm();
  const [editForm] = Form.useForm();
  return (
    <PageContainer ghost>
      <Form
        labelCol={{ span: 8 }}
        wrapperCol={{ span: 16 }}
        form={searchForm}
        layout="inline"
      >
        <FormItem
          name="name"
          label={<FormattedMessage id={'page.graph.graphName'} />}
        >
          <Input></Input>
        </FormItem>
        <FormItem name="" label="">
          <Button>
            {' '}
            <FormattedMessage id={'page.graph.search'} />
          </Button>
        </FormItem>
      </Form>
      <div className={styles.container}>
        <Flex gap="small">
          <Button
            onClick={() => {
              setEditFormState(true);
            }}
            type="primary"
            style={{
              marginBottom: '20px',
            }}
          >
            {' '}
            <FormattedMessage id={'page.graph.addNew'} />
          </Button>
        </Flex>
        <Table dataSource={data} columns={columns}></Table>
      </div>
      <Modal
        title="修改项目"
        onCancel={() => {
          setEditFormState(false);
        }}
        onOk={() => {
          setEditFormState(false);
        }}
        open={editFormState}
      >
        <Form
          labelCol={{ span: 8 }}
          wrapperCol={{ span: 16 }}
          form={editForm}
          style={{
            paddingTop: '20px',
          }}
          initialValues={initForm}
        >
          <FormItem
            name="name"
            rules={[{ required: true }]}
            label={<FormattedMessage id={'page.graph.graphName'} />}
          >
            <Input></Input>
          </FormItem>
          <FormItem
            name="desc"
            rules={[{ required: true }]}
            label={<FormattedMessage id={'page.graph.graphDesc'} />}
          >
            <Input></Input>
          </FormItem>
          <FormItem
            name="groupId"
            rules={[{ required: true }]}
            label="项目Group"
          >
            <Input></Input>
          </FormItem>
          <FormItem
            name="artifactId"
            rules={[{ required: true }]}
            label="项目Aitifact"
          >
            <Input></Input>
          </FormItem>
          <FormItem
            name="springAIAlibabaVersion"
            rules={[{ required: true }]}
            label="SpringAIAlibaba"
          >
            <Input></Input>
          </FormItem>
          <FormItem name="jdk" rules={[{ required: true }]} label="JDK">
            <Input></Input>
          </FormItem>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default GraphIndex;
