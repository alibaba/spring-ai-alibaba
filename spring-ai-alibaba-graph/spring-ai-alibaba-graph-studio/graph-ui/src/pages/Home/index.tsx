import Guide from '@/components/Guide';
import { trim } from '@/utils/format';
import { PageContainer } from '@ant-design/pro-components';
import { useModel } from '@umijs/max';
import { Card, Col, Row } from 'antd';
import styles from './index.less';

const WorkspacePage: React.FC = () => {
  const { name } = useModel('global');
  return (
    <PageContainer ghost>
      <div className={styles.container}>
        <Guide name={trim(name)} />
        <Row gutter={20}>
          <Col span={6}>
            <Card>{'Create a new one ->'} </Card>
          </Col>
          <Col span={6}>
            <Card>{'To Chatbot ->'} </Card>
          </Col>
          <Col span={6}>
            <Card>{'To Agent ->'} </Card>
          </Col>
          <Col span={6}>
            <Card>{'To Graph ->'} </Card>
          </Col>
        </Row>
      </div>
    </PageContainer>
  );
};

export default WorkspacePage;
