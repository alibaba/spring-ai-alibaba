import ProCard from '@/components/Card/ProCard';
import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import React from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './index.module.less';

const HomePage: React.FC = () => {
  const navigate = useNavigate();

  const handleFirstCardClick = () => {
    // Navigate to the original app list page
    navigate('/app');
  };

  const handleSecondCardClick = () => {
    // Navigate to Dify converter page
    navigate('/dify');
  };

  const handleThirdCardClick = () => {
    // TODO: Navigate to debugging tools page
    console.log('Navigate to debugging tools page');
  };

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.App.index.home',
            dm: '首页',
          }),
        },
      ]}
    >
      <div className={styles.homeContainer}>
        <div className={styles.cardGrid}>
          <div className={styles.cardItem}>
            <ProCard
              title="低代码智能体开发平台"
              logo={
                <div className={styles.cardIcon}>
                  <img
                    src="/images/agentLogo.svg"
                    alt="Spring AI Alibaba Platform"
                    className={styles.iconImage}
                  />
                </div>
              }
              info={[
                {
                  content: $i18n.get({
                    id: 'main.pages.App.index.platformDescription',
                    dm: '基于 Spring AI Alibaba 的低代码智能体开发平台，提供可视化的智能体构建和管理能力。支持在线调试部署，并可一键导出为 Spring AI Alibaba 工程。',
                  }),
                },
              ]}
              onClick={handleFirstCardClick}
              className={styles.clickableCard}
            />
          </div>

          <div className={styles.cardItem}>
            <ProCard
              title="DIFY 应用转换为 Spring AI Alibaba 工程"
              logo={
                <div className={styles.cardIcon}>
                  <img
                    src="/images/workflowLogo.svg"
                    alt="Dify DSL Generation"
                    className={styles.iconImage}
                  />
                </div>
              }
              info={[
                {
                  content: $i18n.get({
                    id: 'main.pages.App.index.difyDescription',
                    dm: '此功能可帮助您将 Dify 平台上开发的智能体转换成 Spring AI Alibaba 框架应用，一键下载源码并导入 IDE 开发维护。',
                  }),
                },
              ]}
              onClick={handleSecondCardClick}
              className={styles.clickableCard}
            />
          </div>

          <div className={styles.cardItem}>
            <ProCard
              title="Spring AI Alibaba 智能体调试工具"
              logo={
                <div className={styles.cardIcon}>
                  <img
                    src="/images/tool.svg"
                    alt="Debug Tools"
                    className={styles.iconImage}
                  />
                </div>
              }
              info={[
                {
                  content: $i18n.get({
                    id: 'main.pages.App.index.debugDescription',
                    dm: '为您的 Spring AI Alibaba 应用提供在线调试功能，支持可视化调试、对话、Graph 流程展示等。',
                  }),
                },
              ]}
              onClick={handleThirdCardClick}
              className={styles.clickableCard}
            />
          </div>
        </div>
      </div>
    </InnerLayout>
  );
};

export default HomePage;
