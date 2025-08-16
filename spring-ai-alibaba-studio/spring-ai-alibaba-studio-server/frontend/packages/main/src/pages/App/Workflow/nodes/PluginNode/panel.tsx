import $i18n from '@/i18n';
import { getTool } from '@/services/plugin';
import { PluginTool } from '@/types/plugin';
import { Empty } from '@spark-ai/design';
import {
  CustomInputsControl,
  IValueType,
  OutputParamsTree,
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
} from '@spark-ai/flow';
import { useSetState } from 'ahooks';
import { Flex, Spin } from 'antd';
import { memo, useCallback, useEffect, useMemo } from 'react';
import ErrorCatchForm from '../../components/ErrorCatchForm';
import InfoIcon from '../../components/InfoIcon';
import RetryForm from '../../components/RetryForm';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import { IPluginNodeData, IPluginNodeParam } from '../../types';
import { getDefaultValueSchemaFromOutputParams } from '../APINode/panel';

export default memo(function PluginNodePanel(props: {
  id: string;
  data: IPluginNodeData;
}) {
  const [state, setState] = useSetState({
    loading: true,
    detail: null as PluginTool | null,
  });
  const { getVariableList } = useNodesOutputParams();
  const globalVariableList = useWorkflowAppStore(
    (state) => state.globalVariableList,
  );
  const nodes = useReactFlowStore((store) => store.nodes);
  const edges = useReactFlowStore((store) => store.edges);
  const { nodesReadOnly } = useNodesReadOnly();

  const { handleNodeDataUpdate } = useNodeDataUpdate();

  const flowVariableList = useMemo(() => {
    return getVariableList({
      nodeId: props.id,
    });
  }, [props.id, nodes, edges]);

  const variableList = useMemo(() => {
    return [...globalVariableList, ...flowVariableList];
  }, [globalVariableList, flowVariableList]);

  useEffect(() => {
    if (!props.data.node_param.tool_id) {
      setState({
        loading: false,
        detail: null,
      });
      return;
    }

    getTool(props.data.node_param.plugin_id, props.data.node_param.tool_id)
      .then((res) => {
        setState({
          loading: false,
          detail: res?.data as PluginTool,
        });
        if (res?.data) {
          handleNodeDataUpdate({
            id: props.id,
            data: {
              input_params: (res.data.config?.input_params || []).map(
                (item) =>
                  props.data.input_params.find(
                    (vItem) => vItem.key === item.key,
                  ) || {
                    key: item.key,
                    type: item.type as IValueType,
                    value_from: 'refer',
                    value: void 0,
                  },
              ),
              output_params: (res.data.config?.output_params || []).map(
                (item) => ({
                  key: item.key,
                  type: item.type as IValueType,
                  desc: item.description,
                }),
              ),
              node_param: {
                ...props.data.node_param,
                plugin_name: res.data.name,
                tool_id: res.data.tool_id as string,
                tool_name: res.data.name as string,
              },
            },
          });
        }
      })
      .catch(() => {
        setState({
          loading: false,
        });
      });
  }, [props.data.node_param.tool_id, props.data.node_param.plugin_id]);

  const changeNodeParam = useCallback(
    (payload: Partial<IPluginNodeParam>) => {
      handleNodeDataUpdate({
        id: props.id,
        data: {
          node_param: {
            ...props.data.node_param,
            ...payload,
          },
        },
      });
    },
    [props.data.node_param, handleNodeDataUpdate],
  );

  if (state.loading) return <Spin spinning className="loading-center" />;

  if (!state.detail)
    return (
      <div className="loading-center">
        <Empty
          title={$i18n.get({
            id: 'main.pages.App.Workflow.nodes.PluginNode.panel.toolNotFound',
            dm: '未找到工具',
          })}
          description={$i18n.get({
            id: 'main.pages.App.Workflow.nodes.PluginNode.panel.toolNotExist',
            dm: '工具不存在，请删除后重新添加',
          })}
        />
      </div>
    );

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.PluginNode.panel.input',
              dm: '输入',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.PluginNode.panel.inputVariables',
                dm: '输入本节点需要处理的变量，用于识别需要处理的内容，支持引用前置。',
              })}
            />
          </div>
          <CustomInputsControl
            disabledKey
            disabled={nodesReadOnly}
            onChange={(payload) => {
              handleNodeDataUpdate({
                id: props.id,
                data: {
                  input_params: payload,
                },
              });
            }}
            value={props.data.input_params}
            variableList={variableList}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.PluginNode.panel.output',
              dm: '输出',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.PluginNode.panel.outputVariables',
                dm: '输出本节点处理结果的变量，用于后续节点识别和处理本节点的处理结果。',
              })}
            />
          </div>
          <OutputParamsTree data={props.data.output_params} />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <RetryForm
          disabled={nodesReadOnly}
          value={props.data.node_param.retry_config}
          onChange={(val) =>
            changeNodeParam({
              retry_config: val,
            })
          }
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <ErrorCatchForm
          disabled={nodesReadOnly}
          nodeId={props.id}
          value={props.data.node_param.try_catch_config}
          onChange={(val) =>
            changeNodeParam({
              try_catch_config: val,
            })
          }
          onChangeType={(type) => {
            const params =
              type === 'failBranch'
                ? {
                    default_values: getDefaultValueSchemaFromOutputParams(
                      props.data.output_params,
                    ),
                  }
                : {};
            changeNodeParam({
              try_catch_config: {
                ...props.data.node_param.try_catch_config,
                strategy: type,
                ...params,
              },
            });
          }}
        />
      </div>
    </>
  );
});
