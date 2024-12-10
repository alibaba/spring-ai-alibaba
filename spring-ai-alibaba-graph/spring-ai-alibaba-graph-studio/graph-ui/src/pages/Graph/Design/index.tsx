import { ReactFlowProvider } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import './index.less';
import './xyTheme.less';

import { LayoutFlow } from '@/pages/Graph/Design/map';
import ToolBar from '@/pages/Graph/Design/toolbar/ToolBar';
import { PageContainer } from '@ant-design/pro-components';
import { Icon } from '@iconify/react';
import { Affix, Button } from 'antd';
import { history } from 'umi';

export default function () {
  return (
    <PageContainer ghost>
      <Affix>
        <Button
          onClick={() => {
            history.push('/graph');
          }}
          type="dashed"
        >
          返回
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
          <ToolBar></ToolBar>
        </ReactFlowProvider>
      </div>
    </PageContainer>
  );
}
