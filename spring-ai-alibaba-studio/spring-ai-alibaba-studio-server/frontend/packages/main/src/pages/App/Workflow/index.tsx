import InnerLayout from '@/components/InnerLayout';
import { useInnerLayout } from '@/components/InnerLayout/utils';
import $i18n from '@/i18n';
import { getAppComponentInputAndOutputParams } from '@/services/appComponent';
import {
  getAppDetail,
  getAppVersionDetail,
  publishApp,
  updateApp,
} from '@/services/appManage';
import { IAppStatus, IWorkFlowAppDetail } from '@/types/appManage';
import { IBizFlowData } from '@/types/workflow';
import {
  Button,
  Empty,
  IconButton,
  IconFont,
  renderTooltip,
} from '@spark-ai/design';
import {
  CheckListBtn,
  ConfigPanel,
  Edge,
  Flow,
  FlowAside,
  FlowPanel,
  FlowTools,
  IValueType,
  IWorkFlowNode,
  ReactFlowProvider,
  TaskStatus,
  useFlowInteraction,
  useStore,
  WorkflowContextProvider,
} from '@spark-ai/flow';
import { useMount, useSetState } from 'ahooks';
import { Flex, message, Space, Tooltip, Typography } from 'antd';
import dayjs from 'dayjs';
import React, { memo, useCallback, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { dispatch } from 'use-bus';
import AppStatusBar from '../AssistantAppEdit/components/AppStatus';
import HistoryPanelComp from '../AssistantAppEdit/components/HistoryPanel/HistoryPanelComp';
import PublishAppSuccessModal from '../AssistantAppEdit/components/PublishAppSuccessModal';
import ChannelConfig from '../components/ChannelConfig';
import { channelConfigEventBus } from '../components/ChannelConfig/PublishComponentCard';
import { EditNameModal } from '../components/EditNameModal';
import { MCPToolSelectModalFuncs } from '../components/MCPSelector';
import { ToolSelectorModalFuncs } from '../components/PluginSelector/show';
import { ComponentSelectorModalFuncs } from './components/ComponentSelectorModal/show';
import GlobalVariableFormModal from './components/GlobalVariableFormModal';
import { HistoryConfigBtn } from './components/HistoryConfigModal';
import SingleNodeDrawer from './components/SingleNodeDrawer';
import TaskResultShowBtn from './components/TaskResultShowBtn';
import TestPanel from './components/TestPanel';
import VersionManagerBtn from './components/VersionManagerBtn';
import {
  useWorkflowAppStore,
  WorkflowAppProvider,
} from './context/WorkflowAppProvider';
import { useGlobalVariableList } from './hooks/useGlobalVariableList';
import { useInitDebug } from './hooks/useInitDebug';
import NODE_COMPONENT_MAP from './nodes/constant';
import getConfigPanel from './nodes/getConfigPanel';
import { NODE_SCHEMA_MAP } from './nodes/nodeSchemaMap';
import {
  IAppComponentNodeParam,
  IMCPNodeParam,
  IPluginNodeParam,
} from './types';
import { getMCPNodeInputParams } from './utils';
import { transformToBizData, transformToFlowData } from './utils/transform';
import { convertDifyToSpringAI } from '@/services/difyConverter';

interface IProps {
  onSave: (data: IBizFlowData) => void;
  appDetail: IWorkFlowAppDetail;
  init: (forceUpdateFlow?: boolean) => void;
  setActiveTab: (tab: string) => void;
}

interface IFlowBaseProps extends Omit<IProps, 'setActiveTab' | 'onSave'> {
  actionLoading: boolean;
  handlePublish: () => void;
  handleExportSAA: () => void;
}

const lang = $i18n.getCurrentLanguage();

export const FlowBase = memo((props: IFlowBaseProps) => {
  const { actionLoading, init, appDetail, handlePublish, handleExportSAA } = props;
  const setShowTest = useWorkflowAppStore((state) => state.setShowTest);
  const { initDebug } = useInitDebug();
  const portal = useInnerLayout();
  const [showHistoryPanel, setShowHistoryPanel] = useState(false);
  const selectedVersion = useWorkflowAppStore((state) => state.selectedVersion);
  const checkList = useStore((state) => state.checkList);
  const setShowCheckList = useStore((state) => state.setShowCheckList);
  const setSelectedVersion = useWorkflowAppStore(
    (state) => state.setSelectedVersion,
  );
  const { onFlowClearState } = useFlowInteraction();
  const [showGlobalVariableFormModal, setShowGlobalVariableFormModal] =
    useState(false);

  const onSelectVersion = useCallback((version: string) => {
    setSelectedVersion(version);
    onFlowClearState({ readyOnly: version !== 'draft' });
    setShowTest(false);

    if (version === 'draft') {
      init(true);
      return;
    }
    getAppVersionDetail(appDetail.app_id, version).then((res) => {
      try {
        const config = JSON.parse(res.data.config);
        dispatch({
          type: 'update-flow-data',
          data: transformToFlowData({
            nodes: config?.nodes || [],
            edges: config?.edges || [],
          }),
        });
      } catch {}
    });
  }, []);

  return (
    <>
      <div className="h-full flex flex-col">
        <div className="flex-shrink-0">
          <TaskStatus />
        </div>
        <div className="flex flex-1 h-1 relative">
          <FlowAside />
          <div className="relative flex-1">
            <Flow nodeTypes={NODE_COMPONENT_MAP} />
            <FlowTools />
            <FlowPanel>
              <ConfigPanel singleTestPanel={SingleNodeDrawer} />
              <TestPanel />
            </FlowPanel>
          </div>
          {showHistoryPanel && (
            <HistoryPanelComp
              hasInitData
              appDetail={props.appDetail}
              onSelectVersion={onSelectVersion}
              selectedVersion={selectedVersion}
              onClose={() => {
                setShowHistoryPanel(false);
              }}
            />
          )}
        </div>
      </div>
      {portal.rightPortal(
        <div className="flex gap-[8px]">
          <VersionManagerBtn setShowHistoryPanel={setShowHistoryPanel} />
          {selectedVersion === 'draft' ? (
            <>
              <Tooltip
                title={$i18n.get({
                  id: 'main.pages.App.Workflow.index.index.conversationVariablesRecordParameters',
                  dm: '通过会话变量,可以在流程的全生命周期中记录参数信息,并在节点中被引用',
                })}
              >
                <IconButton
                  onClick={() => setShowGlobalVariableFormModal(true)}
                  shape="default"
                  icon={<IconFont type="spark-intervention-line" />}
                />
              </Tooltip>
              <HistoryConfigBtn
                appDetail={props.appDetail}
                onSave={props.init}
              />

              <TaskResultShowBtn />
              <Space.Compact>
                <Button
                  disabled={actionLoading}
                  onClick={() => {
                    setShowTest(true);
                    initDebug(true);
                  }}
                >
                  {$i18n.get({
                    id: 'main.pages.App.Workflow.index.index.test',
                    dm: '测试',
                  })}
                </Button>
                <CheckListBtn />
              </Space.Compact>
              {/* 新增“导出SAA工程代码”按钮 */}
              <Button
                disabled={actionLoading}
                onClick={handleExportSAA}
              >
                导出SAA工程代码
              </Button>
              <Button
                disabled={actionLoading}
                onClick={() => {
                  if (checkList.length) {
                    message.warning(
                      $i18n.get({
                        id: 'main.pages.App.Workflow.index.checkNodeConfiguration',
                        dm: '请先检查节点配置',
                      }),
                    );
                    setShowCheckList(true);
                    return;
                  }
                  handlePublish();
                }}
                type="primary"
              >
                {$i18n.get({
                  id: 'main.pages.App.Workflow.index.index.publish',
                  dm: '发布',
                })}
              </Button>
            </>
          ) : (
            <Button
              icon={<IconFont type="spark-processOutput-line" />}
              onClick={async () => {
                onSelectVersion('draft');
                message.success(
                  $i18n.get({
                    id: 'main.pages.App.AssistantAppEdit.components.AppActions.index.returnedToCurrentVersion',
                    dm: '已回到当前版本',
                  }),
                );
              }}
            >
              {$i18n.get({
                id: 'main.pages.App.AssistantAppEdit.components.AppActions.index.returnToCurrentVersion',
                dm: '回到当前版本',
              })}
            </Button>
          )}
        </div>,
      )}
      {showGlobalVariableFormModal && (
        <GlobalVariableFormModal
          value={
            props.appDetail.config.global_config?.variable_config
              ?.conversation_params
          }
          appDetail={props.appDetail}
          onClose={() => setShowGlobalVariableFormModal(false)}
          onOk={() => {
            setShowGlobalVariableFormModal(false);
            props.init();
          }}
        />
      )}
    </>
  );
});

export const FlowEditor = memo((props: IProps) => {
  const appId = useWorkflowAppStore((state) => state.appId);
  const { initDebug } = useInitDebug();
  const [actionLoading, setActionLoading] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);

  const handlePublish = useCallback(() => {
    setActionLoading(true);
    publishApp(appId)
      .then(() => {
        setShowSuccess(true);
        props.init();
        dispatch({
          type: 'history-panel-fresh',
        });
      })
      .finally(() => {
        setActionLoading(false);
      });
  }, []);

  const handleExportSAA = useCallback(async () => {
    if (!props.appDetail) return;

    console.log(props.appDetail);

    setActionLoading(true);
    try {
      // 准备请求参数
      const params = {
        dependencies: 'spring-ai-alibaba-graph,web,spring-ai-alibaba-starter-dashscope,spring-ai-starter-mcp-client',
        appMode: 'workflow',
        dslDialectType: 'studio',
        type: 'maven-project',
        language: 'java',
        bootVersion: '3.5.0',
        baseDir: 'demo',
        groupId: 'com.example',
        artifactId: 'demo',
        name: 'demo',
        description: 'Demo project for Spring Boot',
        packageName: 'com.example.demo',
        packaging: 'jar',
        javaVersion: '17',
        dsl: JSON.stringify(props.appDetail),
      };

      // 调用转换服务
      const response = await convertDifyToSpringAI(params);

      // 处理 zip 文件下载
      const blob = response.data;
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'spring-ai-alibaba-demo.zip';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      message.success('转换成功！项目文件已开始下载');
    } catch (error) {
      console.error('转换失败:', error);
      message.error(`转换失败：${error.message || '请重试'}`);
    } finally {
      setActionLoading(false); // 重置加载状态
    }
  }, [props.appDetail]);

  const handleSave = useCallback(
    async (data: { nodes: IWorkFlowNode[]; edges: Edge[] }) => {
      setActionLoading(true);
      try {
        await props.onSave(transformToBizData(data));
        initDebug();
      } catch {}
      setActionLoading(false);
    },
    [props.onSave, initDebug],
  );

  const handleAddCustomNode: (
    data: IWorkFlowNode,
  ) => Promise<IWorkFlowNode | null> = useCallback(
    (data) => {
      return new Promise((resolve) => {
        switch (data.type) {
          case 'AppComponent':
            ComponentSelectorModalFuncs.show({
              appCode: appId,
              onCancel: () => resolve(null),
              onOk: async (item) => {
                const res = await getAppComponentInputAndOutputParams(
                  item.code!,
                );
                resolve({
                  ...data,
                  data: {
                    ...data.data,
                    input_params: res.input.map((item) => ({
                      key: item.alias,
                      type: item.type,
                      value_from: 'refer',
                      value: void 0,
                    })),
                    output_params: res.output.map((item) => ({
                      key: item.field,
                      type: item.type,
                      desc: item.description,
                    })),
                    node_param: {
                      ...(data.data.node_param as IAppComponentNodeParam),
                      name: item.name!,
                      code: item.code!,
                      type: item.type!,
                      output_type: res.output_type,
                    },
                  },
                });
              },
            });
            break;
          case 'MCP':
            MCPToolSelectModalFuncs.show({
              onCancel: () => resolve(null),
              onOk: (tool, server) => {
                resolve({
                  ...data,
                  data: {
                    ...data.data,
                    node_param: {
                      ...(data.data.node_param as IMCPNodeParam),
                      tool_name: tool.name,
                      server_code: server.server_code,
                      server_name: server.name,
                    },
                    input_params: getMCPNodeInputParams(tool),
                  },
                });
              },
            });
            break;
          case 'Plugin':
            ToolSelectorModalFuncs.show({
              onCancel: () => resolve(null),
              onOk: (tools) => {
                resolve({
                  ...data,
                  data: {
                    ...data.data,
                    input_params: (tools[0].config?.input_params || []).map(
                      (item) => ({
                        key: item.key,
                        type: item.type as IValueType,
                        value_from: 'refer',
                        value: void 0,
                      }),
                    ),
                    output_params: (tools[0].config?.output_params || []).map(
                      (item) => ({
                        key: item.key,
                        type: item.type as IValueType,
                        desc: item.description,
                      }),
                    ),
                    node_param: {
                      ...(data.data.node_param as IPluginNodeParam),
                      tool_id: tools[0].tool_id as string,
                      tool_name: tools[0].name,
                      plugin_id: tools[0].plugin_id as string,
                      plugin_name: tools[0].name,
                    },
                  },
                });
              },
            });
            break;
        }
      });
    },
    [appId],
  );

  const handleClickAction = (val: string) => {
    props.setActiveTab('channel');
    setShowSuccess(false);
    if (val === 'comp') {
      setTimeout(() => {
        channelConfigEventBus.emit('openCompCfg');
      }, 200);
    }
  };

  return (
    <WorkflowContextProvider
      locale={lang}
      initialState={{
        nodeSchemaMap: NODE_SCHEMA_MAP,
        getConfigPanel,
        onDebounceChange: handleSave,
        onAddCustomNode: handleAddCustomNode,
      }}
    >
      <ReactFlowProvider>
        <FlowBase
          appDetail={props.appDetail}
          actionLoading={actionLoading}
          handlePublish={handlePublish}
          handleExportSAA={handleExportSAA}
          init={props.init}
        />

        {showSuccess && (
          <PublishAppSuccessModal
            onClickAction={handleClickAction}
            onClose={() => setShowSuccess(false)}
          />
        )}
      </ReactFlowProvider>
    </WorkflowContextProvider>
  );
});

function Workflow() {
  const [activeTab, setActiveTab] = useState('config');
  const [state, setState] = useSetState({
    loading: true,
    appDetail: void 0 as IWorkFlowAppDetail | undefined,
    showEditNameModal: false,
    autoSaveTime: '',
    show: false,
  });
  const { initGlobalVariableList } = useGlobalVariableList();
  const cacheAppDetail = useRef<IWorkFlowAppDetail | undefined>(void 0);
  const selectedVersion = useWorkflowAppStore((state) => state.selectedVersion);
  const { id } = useParams();

  const initDetail = useCallback(
    async (forceUpdateFlow = false) => {
      if (!id) {
        setState({
          loading: false,
        });
        return;
      }
      const appDetail = (await getAppDetail(id)) as IWorkFlowAppDetail;
      cacheAppDetail.current = appDetail;
      setState({
        loading: false,
        appDetail,
      });

      initGlobalVariableList(
        appDetail.config.global_config?.variable_config?.conversation_params,
      );
      if (!appDetail.config || !forceUpdateFlow) return;
      try {
        setTimeout(() => {
          dispatch({
            type: 'update-flow-data',
            data: transformToFlowData({
              nodes: appDetail.config?.nodes || [],
              edges: appDetail.config?.edges || [],
            }),
          });
        }, 100);
      } catch {
        setState({
          loading: false,
        });
      }
    },
    [id],
  );

  useMount(() => {
    initDetail(true);
  });

  const handleSaveFlowData = useCallback((data: IBizFlowData) => {
    if (!cacheAppDetail.current) return Promise.resolve();
    return updateApp({
      app_id: cacheAppDetail.current.app_id,
      name: cacheAppDetail.current.name,
      type: cacheAppDetail.current.type,
      config: {
        ...cacheAppDetail.current.config,
        ...data,
      },
    }).then((res) => {
      const appStatus =
        cacheAppDetail.current?.status === 'published'
          ? 'published_editing'
          : cacheAppDetail.current?.status;
      setState({
        autoSaveTime: dayjs().format('HH:mm:ss'),
        appDetail: {
          ...(cacheAppDetail.current as IWorkFlowAppDetail),
          status: appStatus as IAppStatus,
        },
      });
      return res;
    });
  }, []);

  if (!state.loading && !state.appDetail) {
    return (
      <div className="loading-center">
        <Empty
          description={$i18n.get({
            id: 'main.pages.App.Workflow.index.index.applicationDoesNotExist',
            dm: '应用不存在',
          })}
        />
      </div>
    );
  }

  return (
    <React.Fragment>
      <div className="relative h-full">
        <InnerLayout
          loading={state.loading}
          fullScreen={true}
          activeTab={activeTab}
          onTabChange={(val) => {
            setActiveTab(val);
            if (val === 'config') initDetail(true);
          }}
          breadcrumbLinks={[
            {
              title: $i18n.get({
                id: 'main.pages.App.Workflow.index.index.applicationManagement',
                dm: '应用管理',
              }),
              path: '/app/workflow',
            },
            {
              title: (
                <Flex align="center" gap={8}>
                  <Typography.Text
                    ellipsis={{
                      tooltip: renderTooltip(state.appDetail?.name || '', {
                        getPopupContainer: () => document.body,
                      }),
                    }}
                  >
                    <span className="text-[16px]">
                      {state.appDetail?.name?.trim()}
                    </span>
                  </Typography.Text>
                  {selectedVersion === 'draft' && (
                    <IconFont
                      onClick={() => {
                        setState({ showEditNameModal: true });
                      }}
                      type="spark-edit-line"
                    />
                  )}
                </Flex>
              ),
            },
          ]}
          left={
            !!state.appDetail && (
              <AppStatusBar
                appStatus={state.appDetail.status}
                autoSaveTime={state.autoSaveTime}
              />
            )
          }
          tabs={[
            {
              label: $i18n.get({
                id: 'main.pages.App.Workflow.index.index.canvasConfiguration',
                dm: '画布配置',
              }),
              key: 'config',
              children: state.appDetail && (
                <FlowEditor
                  init={initDetail}
                  appDetail={state.appDetail}
                  onSave={handleSaveFlowData}
                  setActiveTab={setActiveTab}
                />
              ),
            },
            {
              label: $i18n.get({
                id: 'main.pages.App.Workflow.index.publishChannel',
                dm: '发布渠道',
              }),
              key: 'channel',
              children: state.appDetail && (
                <ChannelConfig
                  type={state.appDetail.type}
                  app_id={state.appDetail.app_id}
                  status={state.appDetail.status}
                />
              ),
            },
          ]}
        />
      </div>

      {state.showEditNameModal && state.appDetail && (
        <EditNameModal
          name={state.appDetail.name}
          app_id={state.appDetail.app_id}
          description={state.appDetail.description}
          onClose={() => {
            setState({ showEditNameModal: false });
          }}
          onOk={() => {
            setState({ showEditNameModal: false });
            initDetail(true);
          }}
        />
      )}
    </React.Fragment>
  );
}

export default memo(() => {
  const { id } = useParams();
  return (
    <WorkflowAppProvider initialState={{ appId: id }}>
      <Workflow />
    </WorkflowAppProvider>
  );
});
