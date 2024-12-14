import { FormattedMessage } from '@@/exports';
import { PageContainer } from '@ant-design/pro-components';
import { history } from '@umijs/max';
import { Button, Flex, Form, Input, Table } from 'antd';
import FormItem from 'antd/es/form/FormItem';
import styles from './index.less';

const GraphIndex: React.FC = () => {
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
              <Button type="primary" size="small">
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
    </PageContainer>
  );
};

export default GraphIndex;
