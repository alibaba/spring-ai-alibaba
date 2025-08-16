import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { ISparkChatRef } from '@/pages/App/AssistantAppEdit/components/SparkChat';
import {
  getAppComponentServersByCodes,
  IAppType,
} from '@/services/appComponent';
import {
  getAppDetail,
  getAppVersionDetail,
  updateApp,
} from '@/services/appManage';
import { getKnowledgeListByCodes } from '@/services/knowledge';
import { listMcpServersByCodes } from '@/services/mcp';
import { getModelDetail } from '@/services/modelService';
import { getPluginToolsByIds } from '@/services/plugin';
import { IAppComponentListItem } from '@/types/appComponent';
import {
  IAppStatus,
  IAssistantAppDetail,
  IAssistantAppDetailWithInfos,
  IAssistantConfig,
  IAssistantConfigWithInfos,
  ModalityType,
} from '@/types/appManage';
import { IKnowledgeListItem } from '@/types/knowledge';
import { IMcpServer } from '@/types/mcp';
import { IModel } from '@/types/modelService';
import { PluginTool } from '@/types/plugin';
import { Empty, IconFont, renderTooltip } from '@spark-ai/design';
import { useDebounceFn, useSetState } from 'ahooks';
import { Flex, Spin, Typography } from 'antd';
import dayjs from 'dayjs';
import { useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import ChannelConfig from '../components/ChannelConfig';
import { EditNameModal } from '../components/EditNameModal';
import { AssistantAppContext } from './AssistantAppContext';
import AppActions from './components/AppActions';
import AppStatusBar from './components/AppStatus';
import AssistantConfig from './components/AssistantConfig';
import { promptEventBus } from './components/AssistantPromptEditor/eventBus';
import styles from './index.module.less';

export const queryToolsByCode = (
  tools?: Array<{ id: string }>,
): Promise<PluginTool[]> => {
  if (!tools?.length) return Promise.resolve([]);
  return getPluginToolsByIds(tools.map((item) => item.id)).then((res) => {
    return res.data.filter((item) => !!item) || [];
  });
};

export const queryMCPsByCodes = (codes?: string[]): Promise<IMcpServer[]> => {
  if (!codes?.length) return Promise.resolve([]);
  return listMcpServersByCodes({
    server_codes: codes,
    need_tools: true,
  }).then((res) => {
    return res.data.filter((item) => !!item) || [];
  });
};

export const queryComponentsByCodes = (
  codes?: string[],
): Promise<IAppComponentListItem[]> => {
  if (!codes?.length) return Promise.resolve([]);
  return getAppComponentServersByCodes(codes).then((res) => {
    return res;
  });
};

export const queryKnowledgeListByCode = (
  list: string[] = [],
): Promise<IKnowledgeListItem[]> => {
  if (!list.length) return Promise.resolve([]);
  return getKnowledgeListByCodes(list);
};

export const transformAppData = (
  cacheAppDetailWithInfo: IAssistantConfigWithInfos,
): IAssistantConfig => {
  const {
    tools,
    mcp_servers,
    file_search,
    agent_components,
    workflow_components,
    ...extraConfig
  } = cacheAppDetailWithInfo;
  const newAppConfig = {
    ...extraConfig,
    model: extraConfig.model?.model_id,
    tools: tools?.map((item) => ({ id: item.tool_id })) || [],
    mcp_servers: mcp_servers?.map((item) => ({ id: item.server_code })) || [],
    agent_components: agent_components?.map((item) => item.code) || [],
    workflow_components: workflow_components?.map((item) => item.code) || [],
    file_search: {
      ...file_search,
      kb_ids: file_search?.kbs?.map((item) => item.kb_id) || [],
      kbs: undefined,
    },
  };
  return newAppConfig as IAssistantConfig;
};

export function isTextModal(modalType: ModalityType) {
  return ['textDialog', 'textGenerate'].includes(modalType);
}

export type IAssistantAppEditState = {
  /**Page Settings */
  activeKey: string;

  /**Application Settings */
  showEditNameModal: boolean;
  appBasicConfig: IAssistantAppDetailWithInfos | null;
  loading: boolean;
  modelList: any[];
  saveLoading: boolean;
  appStatus: IAppStatus;
  autoSaveTime: string;

  /**Application Experience */
  modalType: ModalityType; // modality type
  flushing: boolean; // whether the conversation is in progress

  /**Related to History Version */
  hasInitData: boolean; // whether the application data has been initialized
  showHistoryPanel: boolean; // whether to show the history panel
  selectedVersion: string;
  historyRefreshCount: number;
  isReleaseVersion: boolean; // whether current version is the latest release version
  readonly: boolean; // whether the page is read-only (cannot modify the configuration or experience), due to the version is neither the latest release version nor the draft version
  canChat: boolean; // whether the page is ready to chat
};

export default function AssistantAppEdit() {
  const { id } = useParams();
  const { tabKey = 'config' } = useParams();
  const [state, setState] = useSetState<IAssistantAppEditState>({
    activeKey: tabKey,
    appBasicConfig: null,
    loading: true,
    modelList: [],
    saveLoading: false,
    appStatus: 'draft',
    autoSaveTime: '',
    modalType: 'textDialog', // default modality type is [textDialog]
    showHistoryPanel: false,
    selectedVersion: 'draft',
    historyRefreshCount: 0,
    flushing: false,
    hasInitData: false,
    showEditNameModal: false,
    isReleaseVersion: false,
    readonly: false,
    canChat: true,
  });
  const cacheAppDetailWithInfo = useRef<IAssistantAppDetailWithInfos | null>(
    null,
  ); // this is the local cache of the agent configuration, compared to the service-side saved configuration, it has more information to display on the page
  const sparkChatComponentRef = useRef<ISparkChatRef | null>(null); // SparkChat component ref
  const getSaveData = () => {
    if (!cacheAppDetailWithInfo.current) return null;
    return transformAppData(cacheAppDetailWithInfo.current.config);
  };

  const updateAppDetailWithInfos = async (appDetail: IAssistantAppDetail) => {
    /** Get detailed information of various plugins, mcp, components, knowledge bases, etc.
     * Convert the codes returned from the service side to specific infos, to display on the page
     * */
    let tools: PluginTool[] = [];
    let model: IModel | undefined = undefined;
    if (appDetail.config.model && appDetail.config.model_provider) {
      model = (
        await getModelDetail(
          appDetail.config.model_provider,
          appDetail.config.model,
        )
      ).data;
    }
    if (process.env.BACK_END === 'java') {
      tools = await queryToolsByCode(appDetail.config.tools);
    }
    const mcp_servers = await queryMCPsByCodes(
      appDetail.config.mcp_servers?.map((item) => item.id) || [],
    );
    const agent_components = await queryComponentsByCodes(
      appDetail.config.agent_components,
    );
    const workflow_components = await queryComponentsByCodes(
      appDetail.config.workflow_components,
    );
    const knowledgeBaseList = await queryKnowledgeListByCode(
      appDetail.config.file_search?.kb_ids,
    );
    const detailWithInfos: IAssistantAppDetailWithInfos = {
      ...appDetail,
      config: {
        ...appDetail.config,
        tools,
        mcp_servers,
        agent_components,
        workflow_components,
        model,
        file_search: {
          ...appDetail.config.file_search,
          kbs: knowledgeBaseList,
        },
      },
    };
    cacheAppDetailWithInfo.current = detailWithInfos;
    setState({
      appBasicConfig: detailWithInfos,
      appStatus: appDetail.status,
      loading: false,
    });
  };
  const refreshAppDetail = async () => {
    if (!id?.length) return;
    let appDetail: IAssistantAppDetail;
    setState({
      hasInitData: false,
    });
    if (state.selectedVersion === 'draft') {
      appDetail = (await getAppDetail(id)) as IAssistantAppDetail;
    } else {
      if (!state.appBasicConfig) {
        return;
      }
      const versionDetail = await getAppVersionDetail(
        id,
        state.selectedVersion,
      );
      const versionConfig = JSON.parse(
        versionDetail.data.config,
      ) as IAssistantConfig;
      appDetail = {
        app_id: id,
        name: state.appBasicConfig.name,
        description: state.appBasicConfig.description,
        status: state.appBasicConfig.status,
        gmt_modified: versionDetail.data.gmt_modified,
        config: versionConfig,
        type: IAppType.AGENT,
        pub_config: state.appBasicConfig.pub_config,
      };
    }
    // initialize the modality type
    setState({
      modalType: appDetail?.config?.modality_type || 'textDialog',
    });
    // update the prompt
    promptEventBus.emit('setEditorCon', appDetail.config.instructions);
    await updateAppDetailWithInfos(appDetail);
    setState({
      hasInitData: true,
    });
  };

  const { run: autoSave } = useDebounceFn(
    (needFresh, appDetailWithInfo = cacheAppDetailWithInfo.current) => {
      if (state.saveLoading || !id) return;
      setState((prev) => ({
        ...prev,
        saveLoading: true,
        autoSaveTime: dayjs().format('HH:mm:ss'),
        appStatus:
          prev.appStatus === 'published' ? 'published_editing' : prev.appStatus,
      }));

      const newAppConfig = getSaveData();

      if (newAppConfig) {
        updateApp({
          app_id: id,
          name: cacheAppDetailWithInfo.current?.name,
          description: cacheAppDetailWithInfo.current?.description,
          config: newAppConfig,
        })
          .then(() => {
            if (needFresh) {
              refreshAppDetail();
            }
          })
          .finally(() => {
            setState({
              saveLoading: false,
            });
          });
      }
    },
    { wait: 300 },
  );

  const onAppChange = (
    payload: Partial<IAssistantAppDetailWithInfos>,
    disableSave = false,
    needFresh = false,
  ) => {
    const newApp = {
      ...cacheAppDetailWithInfo.current,
      ...payload,
      config: {
        ...cacheAppDetailWithInfo.current?.config,
        ...payload.config,
      },
    };
    cacheAppDetailWithInfo.current = newApp as IAssistantAppDetailWithInfos;
    setState({
      appBasicConfig: newApp as IAssistantAppDetailWithInfos | null,
      appStatus: newApp.status as IAppStatus,
    });
    if (!disableSave) autoSave(needFresh);
  };

  const onAppConfigChange = (
    payload: Partial<IAssistantConfigWithInfos>,
    disableSave = false,
    needFresh = false,
  ) => {
    /** Application configurations to be synchronized to the backend */
    if (state.readonly) return; // in read-only state, cannot modify the configuration
    onAppChange(
      { config: payload as IAssistantConfigWithInfos },
      disableSave,
      needFresh,
    );
  };

  useEffect(() => {
    // when the conversation is in progress, close the history panel
    if (state.flushing) {
      setState({
        showHistoryPanel: false,
      });
    }
  }, [state.flushing]);

  useEffect(() => {
    if (
      state.hasInitData &&
      state.appBasicConfig?.config.modality_type !== state.modalType
    ) {
      // update the configuration according to the selectedmodality type
      onAppConfigChange(
        {
          modality_type: state.modalType,
        },
        false,
        true,
      );
    }
  }, [state.modalType, state.hasInitData]);

  useEffect(() => {
    setState({
      canChat:
        (state.isReleaseVersion || state.selectedVersion === 'draft') &&
        !!state.appBasicConfig?.config?.model?.model_id?.length,
      readonly: state.selectedVersion !== 'draft',
    });
  }, [
    state.isReleaseVersion,
    state.selectedVersion,
    state.appBasicConfig?.config?.model?.model_id,
  ]);

  useEffect(() => {
    refreshAppDetail();
  }, [state.selectedVersion]);

  return (
    <AssistantAppContext.Provider
      value={{
        appState: state,
        setAppState: setState,
        onAppConfigChange,
        onAppChange,
        autoSave,
        appCode: id,
        getSaveData,
        refreshAppDetail,
        sparkChatComponentRef,
      }}
    >
      <div className={styles.page}>
        <Spin spinning={state.loading} rootClassName={styles.loading}>
          {!state.loading && !state.appBasicConfig ? (
            <Empty
              description={$i18n.get({
                id: 'main.pages.App.AssistantAppEdit.index.appNotExists',
                dm: '应用不存在，请返回',
              })}
            />
          ) : (
            <InnerLayout
              loading={state.loading}
              onTabChange={(key) => {
                setState({ activeKey: key });
              }}
              activeTab={state.activeKey}
              right={
                <AppActions
                  activeKey={state.activeKey}
                  updateAppDetailWithInfos={updateAppDetailWithInfos}
                />
              }
              breadcrumbLinks={[
                {
                  title: $i18n.get({
                    id: 'main.pages.App.AssistantAppEdit.index.appManagement',
                    dm: '应用管理',
                  }),
                  path: '/app/agent',
                },
                {
                  title: (
                    <Flex align="center" gap={8}>
                      <Typography.Text
                        ellipsis={{
                          tooltip: renderTooltip(
                            state.appBasicConfig?.name || '',
                            {
                              getPopupContainer: () => document.body,
                            },
                          ),
                        }}
                      >
                        <span className="text-[16px]">
                          {state.appBasicConfig?.name?.trim()}
                        </span>
                      </Typography.Text>
                      <IconFont
                        className={styles.icon}
                        onClick={() => {
                          setState({ showEditNameModal: true });
                        }}
                        type="spark-edit-line"
                      />
                    </Flex>
                  ),
                },
              ]}
              left={
                <AppStatusBar
                  appStatus={state.appStatus}
                  autoSaveTime={state.autoSaveTime}
                />
              }
              tabs={[
                {
                  key: 'config',
                  label: $i18n.get({
                    id: 'main.pages.App.AssistantAppEdit.index.appConfig',
                    dm: '配置',
                  }),
                  children: !state.loading && <AssistantConfig />,
                },
                {
                  key: 'share',
                  label: $i18n.get({
                    id: 'main.pages.App.AssistantAppEdit.index.publishChannels',
                    dm: '发布',
                  }),
                  children: !state.loading && (
                    <ChannelConfig
                      app_id={id || ''}
                      status={state.appStatus}
                      type={IAppType.AGENT}
                    ></ChannelConfig>
                  ),
                },
              ]}
            ></InnerLayout>
          )}
        </Spin>
        {state.showEditNameModal && (
          <EditNameModal
            app_id={id || ''}
            name={state.appBasicConfig?.name}
            description={state.appBasicConfig?.description}
            onOk={() => {
              refreshAppDetail();
              setState({
                showEditNameModal: false,
              });
            }}
            onClose={() => {
              setState({
                showEditNameModal: false,
              });
            }}
            maxLength={50}
          ></EditNameModal>
        )}
      </div>
    </AssistantAppContext.Provider>
  );
}
