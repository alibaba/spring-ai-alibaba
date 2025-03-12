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
import { Button, Flex, Form, Input, Table } from 'antd';
import FormItem from 'antd/es/form/FormItem';
import { history } from 'umi';
import styles from './index.less';

const AgentIndex: React.FC = () => {
  const toChatbotEdit = (prop: any) => {
    history.push('/agent/edit?id=' + prop.id);
  };
  const columns: any = [
    {
      title: <FormattedMessage id={'page.agent.sn'} />,
      dataIndex: '',
      rowScope: 'row',
      render: (text: any, record: any, index: number) => {
        return index + 1;
      },
    },
    {
      title: <FormattedMessage id={'page.agent.agentName'} />,
      dataIndex: 'name',
    },
    {
      title: <FormattedMessage id={'page.agent.version'} />,
      dataIndex: 'version',
    },
    {
      title: <FormattedMessage id={'page.agent.agentDesc'} />,
      dataIndex: 'desc',
    },
    {
      title: <FormattedMessage id={'page.agent.createTime'} />,
      dataIndex: 'createTime',
    },
    {
      title: <FormattedMessage id={'page.agent.updateTime'} />,
      dataIndex: 'updateTime',
    },
    {
      title: <FormattedMessage id={'page.agent.option'} />,
      dataIndex: '',
      key: 'x',
      render: (_: any, record: any) => {
        return (
          <>
            <Flex gap="small">
              <Button
                type="primary"
                size="small"
                onClick={() => toChatbotEdit(record)}
              >
                <FormattedMessage id={'page.agent.editMeta'} />
              </Button>
              <Button
                type="primary"
                size="small"
                onClick={() => toDesign(record)}
              >
                <FormattedMessage id={'page.agent.design'} />
              </Button>
              <Button type="dashed" size="small">
                <FormattedMessage id={'page.agent.genCode'} />
              </Button>
              <Button type="dashed" size="small">
                <FormattedMessage id={'page.agent.genProject'} />
              </Button>
              <Button type="primary" size="small" danger>
                <FormattedMessage id={'page.agent.delete'} />
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
      version: '1.1',
      createTime: new Date().toTimeString(),
      updateTime: new Date().toTimeString(),
      desc: '这是一个示例',
    },
    {
      id: '3',
      version: '1.0',
      createTime: new Date().toTimeString(),
      updateTime: new Date().toTimeString(),
      name: '示例流程图',
      desc: '这是一个示例',
    },
  ];

  const [search] = Form.useForm();
  return (
    <PageContainer ghost>
      <Form
        labelCol={{ span: 8 }}
        wrapperCol={{ span: 16 }}
        form={search}
        layout="inline"
      >
        <FormItem
          name="name"
          label={<FormattedMessage id={'page.agent.agentName'} />}
        >
          <Input></Input>
        </FormItem>
        <FormItem name="" label="">
          <Button>
            {' '}
            <FormattedMessage id={'page.agent.search'} />
          </Button>
        </FormItem>
      </Form>
      <div className={styles.container}>
        <Flex gap="small">
          <Button
            type="primary"
            style={{
              marginBottom: '20px',
            }}
          >
            {' '}
            <FormattedMessage id={'page.agent.addNew'} />
          </Button>
        </Flex>
        <Table dataSource={data} columns={columns}></Table>
      </div>
    </PageContainer>
  );
};

export default AgentIndex;
