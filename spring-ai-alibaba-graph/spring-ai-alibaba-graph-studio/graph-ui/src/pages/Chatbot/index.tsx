import { FormattedMessage } from '@@/exports';
import { PageContainer } from '@ant-design/pro-components';
import { Button, Flex, Form, Input, Table } from 'antd';
import FormItem from 'antd/es/form/FormItem';
import { history } from 'umi';
import styles from './index.less';

const GraphIndex: React.FC = () => {
  const toChatbotEdit = (prop: any) => {
    history.push('chatbot/edit?id=' + prop.id);
  };
  const columns: any = [
    {
      title: <FormattedMessage id={'page.chatbot.sn'} />,
      dataIndex: '',
      rowScope: 'row',
      render: (text: any, record: any, index: number) => {
        return index + 1;
      },
    },
    {
      title: <FormattedMessage id={'page.chatbot.chatbotName'} />,
      dataIndex: 'name',
    },
    {
      title: <FormattedMessage id={'page.chatbot.version'} />,
      dataIndex: 'version',
    },
    {
      title: <FormattedMessage id={'page.chatbot.chatbotDesc'} />,
      dataIndex: 'desc',
    },
    {
      title: <FormattedMessage id={'page.chatbot.createTime'} />,
      dataIndex: 'createTime',
    },
    {
      title: <FormattedMessage id={'page.chatbot.updateTime'} />,
      dataIndex: 'updateTime',
    },
    {
      title: <FormattedMessage id={'page.chatbot.option'} />,
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
                <FormattedMessage id={'page.chatbot.editMeta'} />
              </Button>
              <Button
                type="primary"
                size="small"
                onClick={() => toDesign(record)}
              >
                <FormattedMessage id={'page.chatbot.design'} />
              </Button>
              <Button type="dashed" size="small">
                <FormattedMessage id={'page.chatbot.genCode'} />
              </Button>
              <Button type="dashed" size="small">
                <FormattedMessage id={'page.chatbot.genProject'} />
              </Button>
              <Button type="primary" size="small" danger>
                <FormattedMessage id={'page.chatbot.delete'} />
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
          label={<FormattedMessage id={'page.chatbot.chatbotName'} />}
        >
          <Input></Input>
        </FormItem>
        <FormItem name="" label="">
          <Button>
            {' '}
            <FormattedMessage id={'page.chatbot.search'} />
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
            <FormattedMessage id={'page.chatbot.addNew'} />
          </Button>
        </Flex>
        <Table dataSource={data} columns={columns}></Table>
      </div>
    </PageContainer>
  );
};

export default GraphIndex;
