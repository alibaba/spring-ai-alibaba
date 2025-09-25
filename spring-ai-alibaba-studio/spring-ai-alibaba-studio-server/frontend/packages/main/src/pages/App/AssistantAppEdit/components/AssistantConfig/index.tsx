import defaultSettings from '@/defaultSettings';
import $i18n from '@/i18n';
import { IconFont } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { ConfigProvider, Flex, message, Timeline } from 'antd';
import { default as classNames } from 'classnames';
import { compact } from 'lodash-es';
import { useContext, useLayoutEffect, useRef } from 'react';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import { isTextModal } from '../..';
import { AssistantAppContext } from '../../AssistantAppContext';
import AgentSelectorComp from '../AgentCompSelector';
import { AssistantPromptEditorWrap } from '../AssistantPromptEditor';
import AssistantTestWindow from '../AssistantTestWindow';
import HistoryPanelComp from '../HistoryPanel/HistoryPanelComp';
import KnowledgeBaseSelectorComp from '../KnowledgeSelectorComp';
import MCPSelectorComp from '../MCPSelectorComp';
import PluginSelectorComp from '../PluginSelectorComp';
import WorkFlowSelectorComp from '../WorkFlowSelectorComp';
import styles from './index.module.less';
import ModelConfig from './modelConfig';

export const RAG_PROMPT_TEMPLATE = $i18n.get({
  id: 'main.pages.App.AssistantAppEdit.components.AssistantConfig.index.knowledgeBaseTip',
  dm: '# 知识库\\n请记住以下材料，他们可能对回答问题有帮助。\\n${documents}',
});

export default function AssistantConfig() {
  const { appState, setAppState, appCode, onAppConfigChange } =
    useContext(AssistantAppContext);
  const { appBasicConfig } = appState;
  const prompt = appBasicConfig?.config.instructions;
  const containerRef = useRef(null as HTMLDivElement | null);
  const [widthLayout, setWidthLayout] = useSetState({
    leftWidth: 33.3,
    rightWidth: 66.7,
  });

  const beforeSendValidate = () => {
    if (!appBasicConfig?.config?.model) {
      message.warning(
        $i18n.get({
          id: 'main.pages.App.AssistantAppEdit.components.AssistantConfig.index.selectModelFirst',
          dm: '请先选择模型！',
        }),
      );
      return false;
    }

    return true;
  };

  const getUniqueId = () => {
    return {
      left: `${appState.modalType}_${widthLayout.leftWidth.toString()}`,
      right: `${appState.modalType}_${widthLayout.rightWidth.toString()}`,
    };
  };

  useLayoutEffect(() => {
    if (!isTextModal(appState.modalType)) {
      setWidthLayout({ leftWidth: 33.3, rightWidth: 66.7 });
    } else {
      setWidthLayout({ leftWidth: 50, rightWidth: 50 });
    }
  }, [appState.modalType]);

  // Switch agent version
  const onSelectVersion = async (version: string, index?: number) => {
    if (!appCode) return;
    if (version === 'draft') {
      // switch to current draft
      setAppState({ selectedVersion: 'draft', isReleaseVersion: false });
    } else {
      // switch to historical version
      setAppState({ selectedVersion: version, isReleaseVersion: index === 0 });
    }
  };

  return (
    <div
      id={`agent_${appCode}`}
      ref={containerRef}
      className={styles.container}
      style={{ background: 'var(--ag-ant-color-bg-base)' }}
    >
      <PanelGroup direction="horizontal" id="group">
        <Panel
          minSizePixels={340}
          defaultSizePercentage={widthLayout.leftWidth}
          id={getUniqueId().left}
          order={1}
          style={{ overflowY: 'auto' }}
        >
          <ConfigProvider componentDisabled={appState.readonly}>
            <div className="p-[8px_20px]">
              <Flex
                justify="space-between"
                className={classNames(styles.title, 'w-full')}
              >
                {$i18n.get({
                  id: 'main.pages.App.AssistantAppEdit.components.AssistantConfig.index.apiConfiguration',
                  dm: 'API配置',
                })}

                <ModelConfig></ModelConfig>
              </Flex>
            </div>
            <div className={styles.configTimelineContainer}>
              <Timeline
                className={styles.configTimeline}
                style={{ padding: '0 20px', marginTop: 8 }}
                items={compact([
                {
                  children: (
                    <div>
                      <div
                        className="text-[14px] font-medium leading-[24px] mb-[10px]"
                        style={{ color: 'var(--ag-ant-color-text)' }}
                      >
                        {$i18n.get({
                          id: 'main.pages.App.AssistantAppEdit.components.AssistantConfig.index.instruction',
                          dm: '指令',
                        })}
                      </div>
                      <AssistantPromptEditorWrap
                        maxTokenContext={
                          defaultSettings.agentSystemPromptMaxLength
                        }
                        appBasicConfig={appBasicConfig}
                        changePrompt={(val) =>
                          onAppConfigChange({ instructions: val })
                        }
                        prompt={prompt || ''}
                      />
                    </div>
                  ),

                  dot: (
                    <IconFont
                      className="width-[20px] height-[20px] rounded-[50%]"
                      type="spark-code02-line"
                    ></IconFont>
                  ),
                },
                {
                  children: (
                    <div>
                      <div
                        className="text-[14px] font-medium leading-[24px] mb-[10px]"
                        style={{ color: 'var(--ag-ant-color-text)' }}
                      >
                        {$i18n.get({
                          id: 'main.pages.App.AssistantAppEdit.components.AssistantConfig.index.knowledge',
                          dm: '知识',
                        })}
                      </div>
                      <KnowledgeBaseSelectorComp></KnowledgeBaseSelectorComp>
                    </div>
                  ),

                  dot: <IconFont type="spark-paper-line"></IconFont>,
                },
                {
                  children: (
                    <div>
                      <div
                        className="text-[14px] font-medium leading-[24px] mb-[10px]"
                        style={{ color: 'var(--ag-ant-color-text)' }}
                      >
                        {$i18n.get({
                          id: 'main.pages.App.AssistantAppEdit.components.AssistantConfig.index.skill',
                          dm: '技能',
                        })}
                      </div>
                      <MCPSelectorComp />
                      <PluginSelectorComp />
                      <AgentSelectorComp />
                      <WorkFlowSelectorComp />
                    </div>
                  ),

                  dot: <IconFont type="spark-toolbox-line"></IconFont>,
                },
              ])}
            ></Timeline>
            </div>
          </ConfigProvider>
        </Panel>
        <PanelResizeHandle className={styles.resizeHandle}>
          <div className={styles.divider1}>
            <img draggable={false} src="/images/panelResizeHandle.svg" alt="" />
          </div>
        </PanelResizeHandle>
        <Panel
          minSizePixels={600}
          defaultSizePercentage={widthLayout.rightWidth}
          order={2}
          id={getUniqueId().right}
        >
          <ConfigProvider componentDisabled={!appState.canChat}>
            <div className={styles.testWindow}>
              <AssistantTestWindow
                appStatus={appState.appStatus}
                beforeSendValidate={beforeSendValidate}
                maxTokenContext={defaultSettings.agentUserPromptMaxLength}
                assistantId={appCode}
              />
            </div>
          </ConfigProvider>
        </Panel>
      </PanelGroup>
      {appState.showHistoryPanel && appCode && appBasicConfig && (
        <HistoryPanelComp
          hasInitData
          appDetail={appBasicConfig}
          onSelectVersion={onSelectVersion}
          selectedVersion={appState.selectedVersion}
          onClose={() => {
            setAppState({ showHistoryPanel: false });
          }}
        />
      )}
    </div>
  );
}
