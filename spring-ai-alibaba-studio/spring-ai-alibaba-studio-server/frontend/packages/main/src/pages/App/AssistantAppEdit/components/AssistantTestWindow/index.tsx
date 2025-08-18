import $i18n from '@/i18n';
import { IAppStatus, ModalityTypeTexts } from '@/types/appManage';
import {
  Badge,
  Dropdown,
  IconButton,
  IconFont,
  Tooltip,
} from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { ConfigProvider, Flex } from 'antd';
import { useContext, useEffect, useRef } from 'react';
import { AssistantAppContext } from '../../AssistantAppContext';
import ExperienceConfigDrawer from '../ExperienceConfigDrawer';
import SparkChat from '../SparkChat';
import VarConfigDrawer from '../VarConfigDrawer';
import styles from './index.module.less';

interface IProps {
  assistantId?: string;
  beforeSendValidate: () => boolean;
  maxTokenContext: number;
  appStatus: IAppStatus;
}

export type IAssistantTestWindowState = {
  showVarDrawer: boolean; // whether to display the plugin custom parameter
  showExperienceConfigDrawer: boolean; // whether to display the experience configuration drawer
  bizVars: Record<string, Record<string, any>>; // the input parameters of the outer component
  showVarDrawerTipBtn: boolean; // whether to display the custom parameter switch
  isBizVarsComplete: boolean; // whether bizVars is complete
};
export default function AssistantTestWindow(props: IProps) {
  const { componentDisabled } = ConfigProvider.useConfig();
  const { appCode, appState, setAppState, sparkChatComponentRef } =
    useContext(AssistantAppContext);
  const { appBasicConfig } = appState;
  const [state, setState] = useSetState<IAssistantTestWindowState>({
    showExperienceConfigDrawer: false,
    showVarDrawer: false, // whether to display the variable configuration drawer, default not
    bizVars: {},
    showVarDrawerTipBtn: false,
    isBizVarsComplete: true,
  });
  const actionsRef = useRef<HTMLDivElement | null>(null);
  const panelGroupRef = useRef(null);
  const onSwitchModalType = (type: 'textDialog' | 'textGenerate') => {
    setAppState({
      modalType: type,
    });
  };

  useEffect(() => {
    if (
      !!appBasicConfig?.config?.tools?.length ||
      !!appBasicConfig?.config?.prompt_variables ||
      !!appBasicConfig?.config?.agent_components?.length ||
      !!appBasicConfig?.config?.workflow_components?.length
    ) {
      setState({ showVarDrawerTipBtn: true });
    } else {
      setState({ showVarDrawerTipBtn: false });
    }
  }, [
    appBasicConfig?.config?.tools?.length,
    appBasicConfig?.config?.agent_components?.length,
    appBasicConfig?.config?.workflow_components?.length,
    appBasicConfig?.config?.prompt_variables,
  ]);

  return (
    <div className={styles.container} ref={panelGroupRef}>
      <div
        className="py-[12px] px-[20px] flex items-center justify-between"
        style={{
          background: 'var(--ag-ant-color-fill-tertiary)',
          height: '48px',
        }}
      >
        {/**modality switch*/}
        <Dropdown
          overlayStyle={{ width: 380 }}
          menu={{
            items: [
              {
                key: 'textDialog',
                label: (
                  <Flex
                    align="top"
                    justify="space-between"
                    className="w-full"
                    gap={8}
                    onClick={() => {
                      onSwitchModalType('textDialog');
                    }}
                  >
                    <IconFont
                      className="w-[24px] h-[24px] rounded-[12px]"
                      style={{ background: 'var(--ag-ant-color-mauve-bg)' }}
                      type="spark-text-line"
                    ></IconFont>
                    <div className="flex-1">
                      <div
                        className="text-[12px] font-medium leading-[20px]"
                        style={{ marginBottom: '2px' }}
                      >
                        {ModalityTypeTexts.textDialog}
                      </div>
                      <div
                        className="text-[12px] font-normal leading-[20px]"
                        style={{ color: 'var(--ag-ant-color-text-tertiary)' }}
                      >
                        {$i18n.get({
                          id: 'main.components.AssistantTestWindow.index.conversationBased',
                          dm: '基于LLM的对话型交互，适合进行复杂的多轮对话',
                        })}
                      </div>
                    </div>
                  </Flex>
                ),
              },
            ],
          }}
          disabled={componentDisabled}
        >
          <Flex
            align="center"
            className="text-[16px] font-medium leading-[24px]"
          >
            {ModalityTypeTexts[appState.modalType]}
            <IconFont className="ml-[8px]" type="spark-down-line"></IconFont>
          </Flex>
        </Dropdown>
        {/**configuration header */}
        {
          <span
            className={styles.actions}
            style={{ padding: 0 }}
            ref={actionsRef}
          >
            {/**variables configuration button*/}
            {state.showVarDrawerTipBtn && !componentDisabled && (
              <Tooltip
                title={$i18n.get({
                  id: 'main.components.AssistantTestWindow.index.parameterConfiguration',
                  dm: '入参变量配置',
                })}
                placement="bottom"
              >
                <IconButton
                  icon={
                    <Badge
                      showZero={false}
                      count={state.isBizVarsComplete ? 0 : 1}
                      dot
                    >
                      <IconFont type="spark-modify-line" size="small" />
                    </Badge>
                  }
                  bordered={false}
                  onClick={() => {
                    setState({ showVarDrawer: true });
                  }}
                />
              </Tooltip>
            )}
            {/**experience configuration button*/}
            {appState.modalType === 'textDialog' && (
              <Tooltip
                placement="bottom"
                overlayStyle={{ maxWidth: 320 }}
                title={$i18n.get({
                  id: 'main.components.AssistantTestWindow.index.experienceConfiguration',
                  dm: '体验配置',
                })}
              >
                <IconButton
                  icon={<IconFont type="spark-setting-line" size="small" />}
                  bordered={false}
                  onClick={() => setState({ showExperienceConfigDrawer: true })}
                ></IconButton>
              </Tooltip>
            )}
            {/**drop current session, start a new session*/}
            {appState.modalType === 'textDialog' && (
              <Tooltip
                title={$i18n.get({
                  id: 'main.components.AssistantTestWindow.index.clearHistory',
                  dm: '清空记录',
                })}
              >
                <IconButton
                  icon={<IconFont type="spark-clear-line" size="small" />}
                  bordered={false}
                  onClick={() => sparkChatComponentRef.current?.resetSession()}
                />
              </Tooltip>
            )}
          </span>
        }
      </div>
      {appState.modalType === 'textDialog' && (
        <>
          <div className={styles.inputModalWrapper}>
            <SparkChat
              ref={sparkChatComponentRef}
              maxTokenContext={props.maxTokenContext}
              isBizVarsComplete={state.isBizVarsComplete}
              openVarDrawer={() => {
                setState({ showVarDrawer: true });
              }}
            />
          </div>
        </>
      )}
      {state.showExperienceConfigDrawer && (
        <ExperienceConfigDrawer
          onClose={() => setState({ showExperienceConfigDrawer: false })}
        />
      )}

      {appCode && (
        <VarConfigDrawer
          code={appCode}
          bizVars={state.bizVars}
          pluginToolsList={appBasicConfig?.config?.tools}
          agentComponentList={appBasicConfig?.config.agent_components}
          workflowComponentList={appBasicConfig?.config.workflow_components}
          userPromptParams={appBasicConfig?.config?.prompt_variables}
          onBizVarsUpdate={(bizVars) => {
            if (
              appState.modalType === 'textDialog' ||
              appState.modalType === 'textGenerate'
            ) {
              sparkChatComponentRef.current?.chat?.changeConfigParams({
                bizVars,
              });
            }
            setState({ bizVars });
          }}
          onCancel={() => setState({ showVarDrawer: false })}
          open={state.showVarDrawer}
          onIsBizVarsCompleteChange={(isBizVarsComplete) => {
            setState({ isBizVarsComplete });
          }}
        />
      )}
    </div>
  );
}
