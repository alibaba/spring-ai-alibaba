import React, { Suspense } from 'react';
import { Spin } from 'antd';
import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { ChatProvider } from './contexts/ChatContext';
import { ConfigProvider } from './contexts/ConfigContext';
import { DebugProvider } from './contexts/DebugContext';
import ChatInterface from './components/ChatInterface';
import styles from './index.module.less';

const DebugPage: React.FC = () => {
  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.App.index.home',
            dm: '首页',
          }),
        },
        {
          title: $i18n.get({
            id: 'main.pages.Debug.index.title',
            dm: 'Agent Chat UI',
          }),
        },
      ]}
    >
      <div className={styles.debugContainer}>
        <ConfigProvider>
          <DebugProvider>
            <ChatProvider>
              <Suspense
                fallback={
                  <div className={styles.loadingContainer}>
                    <Spin size="large" tip="加载聊天界面..." />
                  </div>
                }
              >
                <ChatInterface />
              </Suspense>
            </ChatProvider>
          </DebugProvider>
        </ConfigProvider>
      </div>
    </InnerLayout>
  );
};

export default DebugPage;
