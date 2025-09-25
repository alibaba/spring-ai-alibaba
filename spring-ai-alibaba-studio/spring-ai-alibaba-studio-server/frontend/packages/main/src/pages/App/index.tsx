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
    // Navigate to debugging tools page
    navigate('/debug');
  };

  const handleCopilotCardClick = () => {
    // TODO: Navigate to Copilot creation page
    console.log('Navigate to Copilot creation page');
  };

  const handleGraphDebugCardClick = () => {
    // TODO: Navigate to Graph workflow debug page
    console.log('Navigate to Graph workflow debug page');
  };

  const handleAgentManagementCardClick = () => {
    // TODO: Navigate to Agent management page (not implemented yet)
    console.log('Agent management feature not implemented yet');
  };

  const handleTracingCardClick = () => {
    // TODO: Navigate to Tracing page (not implemented yet)
    console.log('Tracing feature not implemented yet');
  };

  const handleEvaluationCardClick = () => {
    // TODO: Navigate to Evaluation page (not implemented yet)
    console.log('Evaluation feature not implemented yet');
  };

  const handlePromptEngineeringCardClick = () => {
    // TODO: Navigate to Prompt Engineering page (not implemented yet)
    console.log('Prompt Engineering feature not implemented yet');
  };

  const handleGithubImportCardClick = () => {
    // TODO: Navigate to GitHub import page (not implemented yet)
    console.log('GitHub import feature not implemented yet');
  };

  const handleAgentSchemaCardClick = () => {
    // Navigate to Agent Schema creation page
    navigate('/agent-schema');
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
        {/* 第一栏：创建智能体 */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>创建智能体</h2>
          <div className={styles.cardGrid}>
            <div className={styles.cardItem}>
              <ProCard
                title="低代码平台开发智能体"
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
                      dm: '提供可视化的智能体开发、调试、部署、导出能力，支持聊天助手、工作流等模式。',
                    }),
                  },
                ]}
                onClick={handleFirstCardClick}
                className={styles.clickableCard}
              />
            </div>

            <div className={styles.cardItem}>
              <ProCard
                title="DIFY 应用转换为 SAA 工程"
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
                      dm: '将 Dify 平台上开发的智能体转换成 Spring AI Alibaba 应用，进而导入 IDE 开发维护。',
                    }),
                  },
                ]}
                onClick={handleSecondCardClick}
                className={styles.clickableCard}
              />
            </div>

            <div className={styles.cardItem}>
              <ProCard
                title="使用 Copilot 创建智能体"
                logo={
                  <div className={styles.cardIcon}>
                    <img
                      src="/images/copilot.svg"
                      alt="Copilot Agent Creation"
                      className={styles.iconImage}
                    />
                  </div>
                }
                info={[
                  {
                    content: '通过 AI Copilot 助手智能引导，快速创建和配置智能体，提供自然语言交互的智能体开发体验',
                  },
                ]}
                onClick={handleCopilotCardClick}
                className={styles.clickableCard}
              />
            </div>

            <div className={styles.cardItem}>
              <ProCard
                title="Agent Schema 方式创建智能体"
                logo={
                  <div className={styles.cardIcon}>
                    <img
                      src="/images/agentSchema.svg"
                      alt="Agent Schema Agent Creation"
                      className={styles.iconImage}
                    />
                  </div>
                }
                info={[
                  {
                    content: '通过 Agent Schema 方式，快速创建和配置智能体',
                  },
                ]}
                onClick={handleAgentSchemaCardClick}
                className={styles.clickableCard}
              />
            </div>

            <div className={styles.cardItem}>
              <ProCard
                title="从 GitHub 导入智能体"
                logo={
                  <div className={styles.cardIconDisabled}>
                    <img
                      src="/images/github.svg"
                      alt="GitHub Import"
                      className={styles.iconImage}
                    />
                  </div>
                }
                info={[
                  {
                    content: $i18n.get({
                      id: 'main.pages.App.index.githubImportDescription',
                      dm: '从 GitHub 仓库导入已有的智能体项目，支持多种项目结构和配置文件的自动识别。',
                    }),
                  },
                ]}
                onClick={handleGithubImportCardClick}
                className={styles.disabledCard}
              />
            </div>
          </div>
        </div>

        {/* 第二栏：调试智能体 */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>调试智能体</h2>
          <div className={styles.cardGrid}>
            <div className={styles.cardItem}>
              <ProCard
                title="Agent Chat UI"
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
                      dm: '兼容 AG-UI 规范的交互式的 UI 聊天界面，支持与智能体进行对话、和调试。',
                    }),
                  },
                ]}
                onClick={handleThirdCardClick}
                className={styles.clickableCard}
              />
            </div>

            <div className={styles.cardItem}>
              <ProCard
                title="Graph 可视化调试"
                logo={
                  <div className={styles.cardIcon}>
                    <img
                      src="/images/graph.svg"
                      alt="Graph Workflow Debug"
                      className={styles.iconImage}
                    />
                  </div>
                }
                info={[
                  {
                    content: '可视化展示 Graph 工作流的执行过程，提供节点状态监控、数据流追踪等功能。',
                  },
                ]}
                onClick={handleGraphDebugCardClick}
                className={styles.clickableCard}
              />
            </div>

            <div className={styles.cardItem}>
              <ProCard
                title="Tracing"
                logo={
                  <div className={styles.cardIconDisabled}>
                    <img
                      src="/images/tracing.svg"
                      alt="Tracing"
                      className={styles.iconImage}
                    />
                  </div>
                }
                info={[
                  {
                    content: '全链路追踪智能体执行过程，提供详细的调用链路分析和性能监控',
                  },
                ]}
                onClick={handleTracingCardClick}
                className={styles.disabledCard}
              />
            </div>

            <div className={styles.cardItem}>
              <ProCard
                title="Evaluation"
                logo={
                  <div className={styles.cardIconDisabled}>
                    <img
                      src="/images/evaluation.svg"
                      alt="Evaluation"
                      className={styles.iconImage}
                    />
                  </div>
                }
                info={[
                  {
                    content: '智能体效果评估和测试框架，支持多维度评估指标和自动化测试',
                  },
                ]}
                onClick={handleEvaluationCardClick}
                className={styles.disabledCard}
              />
            </div>

            <div className={styles.cardItem}>
              <ProCard
                title="Prompt Engineering"
                logo={
                  <div className={styles.cardIconDisabled}>
                    <img
                      src="/images/prompt.svg"
                      alt="Prompt Engineering"
                      className={styles.iconImage}
                    />
                  </div>
                }
                info={[
                  {
                    content: '提示词工程和优化工具，支持提示词模板管理、测试和效果分析',
                  },
                ]}
                onClick={handlePromptEngineeringCardClick}
                className={styles.disabledCard}
              />
            </div>
          </div>
        </div>

        {/* 第三栏：智能体管理 */}
        <div className={styles.section}>
          <h2 className={styles.sectionTitle}>智能体管理</h2>
          <div className={styles.cardGrid}>
            <div className={styles.cardItem}>
              <ProCard
                title="智能体管理"
                logo={
                  <div className={styles.cardIconDisabled}>
                    <img
                      src="/images/management.svg"
                      alt="Agent Management"
                      className={styles.iconImage}
                    />
                  </div>
                }
                info={[
                  {
                    content: '集中管理智能体生命周期，包括创建、配置、部署、监控和版本控制等功能',
                  },
                ]}
                onClick={handleAgentManagementCardClick}
                className={styles.disabledCard}
              />
            </div>
          </div>
        </div>
      </div>
    </InnerLayout>
  );
};

export default HomePage;
