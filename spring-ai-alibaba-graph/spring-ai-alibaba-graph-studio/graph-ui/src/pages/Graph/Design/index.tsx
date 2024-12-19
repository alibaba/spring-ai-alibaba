import { ReactFlowProvider } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import './index.less';
import './xyTheme.less';

import { LayoutFlow } from '@/pages/Graph/Design/map';
import { PageContainer } from '@ant-design/pro-components';
import { Icon } from '@iconify/react';
import { history, useIntl } from '@umijs/max';
import { Affix, Button } from 'antd';

export default function () {
  const intl = useIntl();

  return (
    <PageContainer ghost>
      <Affix>
        <Button
          onClick={() => {
            history.push('/graph');
          }}
          type="dashed"
        >
          {intl.formatMessage({ id: 'page.graph.back' })}
          <Icon
            style={{
              fontSize: '20px',
            }}
            icon="material-symbols:keyboard-backspace-rounded"
          ></Icon>
        </Button>
      </Affix>
      <div
        style={{
          width: '100%',
          height: '100vh',
        }}
      >
        <ReactFlowProvider>
          <LayoutFlow />
        </ReactFlowProvider>
      </div>
    </PageContainer>
  );
}
