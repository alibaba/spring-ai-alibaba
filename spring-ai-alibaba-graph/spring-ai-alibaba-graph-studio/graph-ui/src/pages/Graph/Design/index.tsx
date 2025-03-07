import { ReactFlowProvider } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import './index.less';
import './xyTheme.less';

import GraphMap from '@/pages/Graph/Design/GraphMap';
import { PageContainer } from '@ant-design/pro-components';

export default function () {
  return (
    <PageContainer className="graph-design" ghost>
      <div
        style={{
          width: '100%',
          height: '100vh',
        }}
      >
        <ReactFlowProvider>
          <GraphMap />
        </ReactFlowProvider>
      </div>
    </PageContainer>
  );
}
