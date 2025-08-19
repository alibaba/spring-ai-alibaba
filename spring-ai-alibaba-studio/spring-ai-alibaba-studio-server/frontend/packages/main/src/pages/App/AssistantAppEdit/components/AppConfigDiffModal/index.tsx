import $i18n from '@/i18n';
import { compareArrays } from '@/pages/App/utils';
import { getModelDetail } from '@/services/modelService';
import { IAssistantConfigWithInfos } from '@/types/appManage';
import { IModel } from '@/types/modelService';
import { PluginTool } from '@/types/plugin';
import { Button, Modal } from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { Spin, Table } from 'antd';
import { useContext, useMemo } from 'react';
import {
  queryComponentsByCodes,
  queryKnowledgeListByCode,
  queryMCPsByCodes,
  queryToolsByCode,
} from '../..';
import { AssistantAppContext } from '../../AssistantAppContext';
import styles from './index.module.less';

interface IProps {
  onCancel: () => void;
  onOk: () => void;
  code: string;
  prevConfig: IAssistantConfigWithInfos;
  handleReset: (app: any) => void;
}

interface IDiffConfigItem {
  title: string;
  onlineCfg?: string | number;
  draftCfg?: string | number;
  code: string;
}

const needCheckCfgList = [
  {
    label: $i18n.get({
      id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.modelModelSelection',
      dm: '模型/模型选择',
    }),
    code: 'modelSelect',
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.modelParameterConfigurationTemperatureCoefficient',
      dm: '模型/参数配置/温度系数',
    }),
    code: 'temperature',
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.modelParameterConfigurationMaxReplyLength',
      dm: '模型/参数配置/最长回复长度',
    }),
    code: 'maxTokens',
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.modelParameterConfigurationContextTurns',
      dm: '模型/参数配置/携带上下文轮数',
    }),
    code: 'dialogRound',
  },
  { label: 'Prompt', code: 'instructions' },
  {
    label: $i18n.get({
      id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.knowledgeRetrievalEnhancementSwitch',
      dm: '知识检索增强开关',
    }),
    code: 'enableSearch',
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.knowledgeRetrievalEnhancementSelectedKnowledgeBase',
      dm: '知识检索增强/所选知识库',
    }),
    code: 'knowledgeBaseCodeList',
  },
  {
    label: $i18n.get({
      id: 'main.pages.Component.index.plugin',
      dm: '插件',
    }),
    code: 'plugins',
  },
  {
    label: $i18n.get({
      id: 'main.components.MCPSelectorComp.index.mcpService',
      dm: 'MCP服务',
    }),
    code: 'mcp_servers',
  },
  {
    label: $i18n.get({
      id: 'main.pages.Component.index.intelligentAgent',
      dm: '智能体',
    }),
    code: 'agent_components',
  },
  {
    label: $i18n.get({
      id: 'main.pages.Component.index.workflow',
      dm: '工作流',
    }),
    code: 'workflow_components',
  },
];

export default function AppConfigDiffModal(props: IProps) {
  const { appState } = useContext(AssistantAppContext);
  const [state, setState] = useSetState({
    loading: true,
    diffList: [] as Array<IDiffConfigItem>,
    publishConfig: null as any,
  });

  const initConfig = async () => {
    const { prevConfig } = props;
    let publishConfig = appState.appBasicConfig?.pub_config;
    let tools: PluginTool[] = [];
    let model: IModel | undefined = undefined;
    if (publishConfig?.model && publishConfig.model_provider) {
      model = (
        await getModelDetail(publishConfig.model_provider, publishConfig.model)
      ).data;
    }
    if (process.env.BACK_END === 'java') {
      tools = await queryToolsByCode(publishConfig?.tools);
    }
    const mcp_servers = await queryMCPsByCodes(
      publishConfig?.mcp_servers?.map((item) => item.id) || [],
    );
    const agent_components = await queryComponentsByCodes(
      publishConfig?.agent_components,
    );
    const workflow_components = await queryComponentsByCodes(
      publishConfig?.workflow_components,
    );
    const knowledgeBaseList = await queryKnowledgeListByCode(
      publishConfig?.file_search?.kb_ids,
    );
    const publishConfigWithInfos: IAssistantConfigWithInfos = {
      ...publishConfig,
      tools,
      mcp_servers,
      agent_components,
      workflow_components,
      model,
      file_search: {
        ...publishConfig?.file_search,
        kbs: knowledgeBaseList,
      },
    };

    const diffList: IDiffConfigItem[] = [];
    const prevJsonCfg = prevConfig;
    const nowJsonCfg = publishConfigWithInfos;
    for (let i = 0; i < needCheckCfgList.length; i++) {
      const item = needCheckCfgList[i];
      switch (item.code) {
        case 'modelSelect':
          if (prevJsonCfg.model?.model_id === nowJsonCfg.model?.model_id)
            continue;
          diffList.push({
            title: item.label,
            code: item.code,
            draftCfg: prevJsonCfg.model?.name,
            onlineCfg: nowJsonCfg.model?.name,
          });
          break;
        case 'instructions':
          if (prevJsonCfg.instructions === nowJsonCfg.instructions) continue;
          diffList.push({
            title: item.label,
            code: item.code,
            draftCfg: prevJsonCfg.instructions,
            onlineCfg: nowJsonCfg.instructions,
          });
          break;
        case 'enableSearch':
          if (
            (prevJsonCfg.file_search?.enable_search || false) ===
            (nowJsonCfg.file_search?.enable_search || false)
          )
            continue;
          diffList.push({
            title: item.label,
            code: item.code,
            draftCfg: prevJsonCfg.file_search?.enable_search
              ? $i18n.get({
                  id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.enable',
                  dm: '开启',
                })
              : $i18n.get({
                  id: 'main.pages.Knowledge.Detail.components.FileList.index.close',
                  dm: '关闭',
                }),
            onlineCfg: nowJsonCfg.file_search?.enable_search
              ? $i18n.get({
                  id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.enable',
                  dm: '开启',
                })
              : $i18n.get({
                  id: 'main.pages.Knowledge.Detail.components.FileList.index.close',
                  dm: '关闭',
                }),
          });
          break;
        case 'knowledgeBaseCodeList': {
          const prevKnowledgeBaseCodeList: string[] = [];
          const prevKnowledgeBaseNameList: string[] = [];
          const nowKnowledgeBaseCodeList: string[] = [];
          const nowKnowledgeBaseNameList: string[] = [];
          prevJsonCfg.file_search?.kbs?.forEach((item) => {
            prevKnowledgeBaseCodeList.push(item.kb_id);
            prevKnowledgeBaseNameList.push(item.name);
          });
          nowJsonCfg.file_search?.kbs?.forEach((item) => {
            nowKnowledgeBaseCodeList.push(item.kb_id);
            nowKnowledgeBaseNameList.push(item.name);
          });
          if (
            compareArrays(prevKnowledgeBaseCodeList, nowKnowledgeBaseCodeList)
          )
            continue;
          diffList.push({
            title: item.label,
            code: item.code,
            draftCfg: prevKnowledgeBaseNameList.join('，'),
            onlineCfg: nowKnowledgeBaseNameList.join('，'),
          });
          break;
        }
        case 'plugins': {
          const prevToolCodes: string[] = [];
          const prevToolNames: string[] = [];
          const nowToolCodes: string[] = [];
          const nowToolNames: string[] = [];
          prevJsonCfg.tools?.forEach((item) => {
            prevToolCodes.push(item.tool_id!);
            prevToolNames.push(item.name);
          });
          nowJsonCfg.tools?.forEach((item) => {
            nowToolCodes.push(item.tool_id!);
            nowToolNames.push(item.name);
          });
          if (compareArrays(prevToolCodes, nowToolCodes)) continue;
          diffList.push({
            title: item.label,
            code: item.code,
            draftCfg: prevToolNames.join('，'),
            onlineCfg: nowToolNames.join('，'),
          });
          break;
        }
        case 'mcp_servers': {
          const prevMCPServerCodes = prevJsonCfg.mcp_servers?.map(
            (item) => item.server_code,
          );
          const nowMCPServerCodes = nowJsonCfg.mcp_servers?.map(
            (item) => item.server_code,
          );
          if (compareArrays(prevMCPServerCodes, nowMCPServerCodes)) continue;
          const prevMCPServerNames = prevJsonCfg.mcp_servers?.map(
            (item) => item.name,
          );
          const nowMCPServerNames = nowJsonCfg.mcp_servers?.map(
            (item) => item.name,
          );
          diffList.push({
            title: item.label,
            code: item.code,
            draftCfg: prevMCPServerNames?.join('，'),
            onlineCfg: nowMCPServerNames?.join('，'),
          });
          break;
        }
        default: {
          break;
        }
      }
    }
    setState({
      diffList,
      publishConfig,
      loading: false,
    });
  };

  useMount(() => {
    if (
      appState.appStatus === 'draft' ||
      appState.appStatus === 'published_editing'
    ) {
      initConfig();
    } else {
      setState({
        loading: false,
      });
    }
  });

  const renderDiffName = useMemo(() => {
    return (
      <>
        <div>
          {$i18n.get(
            {
              id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.adjustedConfigurationItems',
              dm: '已调整配置项（{var1}个）',
            },
            { var1: state.diffList.length },
          )}
        </div>
        <div className={styles.desc}>
          {state.diffList.map((item) => item.title).join('、')}
        </div>
      </>
    );
  }, [state.diffList]);

  return (
    <Modal
      onCancel={props.onCancel}
      open
      title={$i18n.get({
        id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.releaseVersion',
        dm: '发布版本',
      })}
      width={981}
      footer={
        <>
          <Button onClick={props.onCancel}>
            {$i18n.get({
              id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.cancelPublish',
              dm: '取消发布',
            })}
          </Button>
          <Button onClick={() => props.onOk()} type="primary">
            {$i18n.get({
              id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.confirmPublish',
              dm: '确认发布',
            })}
          </Button>
        </>
      }
    >
      {state.loading ? (
        <Spin className={styles.loading} spinning />
      ) : (
        <>
          {renderDiffName}
          {(appState.appStatus === 'draft' ||
            appState.appStatus === 'published_editing') && (
            <>
              <div className={styles.title}>
                {$i18n.get({
                  id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.modificationDetails',
                  dm: '修改详情',
                })}
              </div>
              <div className={styles.table}>
                <Table
                  columns={[
                    {
                      title: $i18n.get({
                        id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.configurationItem',
                        dm: '配置项',
                      }),
                      dataIndex: 'title',
                      width: 240,
                    },
                    {
                      title: $i18n.get({
                        id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.publishedVersion',
                        dm: '已发布版本',
                      }),
                      dataIndex: 'onlineCfg',
                      render: (val) => (
                        <span className={styles.configLabel}>{val}</span>
                      ),
                    },
                    {
                      title: $i18n.get({
                        id: 'main.pages.App.AssistantAppEdit.components.AppConfigDiffModal.index.pendingPublishVersion',
                        dm: '待发布版本',
                      }),
                      dataIndex: 'draftCfg',
                      render: (val) => (
                        <span className={styles.configLabel}>{val}</span>
                      ),
                    },
                  ]}
                  dataSource={state.diffList}
                  scroll={{ y: 320 }}
                  pagination={{
                    hideOnSinglePage: true,
                    current: 1,
                    pageSize: state.diffList.length,
                    total: state.diffList.length,
                  }}
                />
              </div>
            </>
          )}
        </>
      )}
    </Modal>
  );
}
