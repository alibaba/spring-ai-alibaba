import $i18n from '@/i18n';
import { ISelectedModelParams } from '@/types/modelService';
import {
  IVarTreeItem,
  OutputParamsTree,
  VarInputTextArea,
  filterVarItemsByType,
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
} from '@spark-ai/flow';
import { Flex } from 'antd';
import { memo, useCallback, useMemo } from 'react';
import ErrorCatchForm from '../../components/ErrorCatchForm';
import InfoIcon from '../../components/InfoIcon';
import ModelConfigFormWrap from '../../components/ModelConfigFormWrap';
import RetryForm from '../../components/RetryForm';
import ShortMemoryForm from '../../components/ShortMemoryForm';
import {
  LLM_NODE_OUTPUT_PARAMS_DEFAULT,
  LLM_WITH_REASONING_NODE_OUTPUT_PARAMS_DEFAULT,
} from '../../constant';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import { ILLMNodeData, ILLMNodeParam } from '../../types';
import { getDefaultValueSchemaFromOutputParams } from '../APINode/panel';

export default memo((props: { id: string; data: ILLMNodeData }) => {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const { getVariableList } = useNodesOutputParams();
  const globalVariableList = useWorkflowAppStore(
    (state) => state.globalVariableList,
  );
  const nodes = useReactFlowStore((store) => store.nodes);
  const edges = useReactFlowStore((store) => store.edges);
  const { nodesReadOnly } = useNodesReadOnly();

  const flowVariableList = useMemo(() => {
    return getVariableList({
      nodeId: props.id,
    });
  }, [props.id, nodes, edges]);

  const variableList = useMemo(() => {
    return [...globalVariableList, ...flowVariableList];
  }, [globalVariableList, flowVariableList]);

  const changeNodeParam = useCallback(
    (payload: Partial<ILLMNodeParam>) => {
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

  const variableListByArrayType = useMemo(() => {
    const list: IVarTreeItem[] = [];
    variableList.forEach((item) => {
      const params = filterVarItemsByType(item.children, [
        'Array<String>',
        'Array<Object>',
      ]);
      if (!params.length) return;
      list.push({
        ...item,
        children: params,
      });
    });
    return list;
  }, [variableList]);

  const fileVariableList = useMemo(() => {
    const list: IVarTreeItem[] = [];
    variableList.forEach((item) => {
      const subList = filterVarItemsByType(item.children, [
        'File',
        'Array<File>',
      ]);
      if (subList.length > 0) {
        list.push({
          ...item,
          children: subList,
        });
      }
    });
    return list;
  }, [variableList]);

  const changeLLMConfig = useCallback(
    (
      payload: ISelectedModelParams,
      options?: { isSupportReasoning: boolean; isSupportVision: boolean },
    ) => {
      const extraParam = options
        ? {
            output_params: options.isSupportReasoning
              ? LLM_WITH_REASONING_NODE_OUTPUT_PARAMS_DEFAULT
              : LLM_NODE_OUTPUT_PARAMS_DEFAULT,
          }
        : {};
      handleNodeDataUpdate({
        id: props.id,
        data: {
          node_param: {
            ...props.data.node_param,
            model_config: {
              ...props.data.node_param.model_config,
              ...payload,
            },
          },
          ...extraParam,
        },
      });
    },
    [props.data.node_param.model_config, changeNodeParam],
  );

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.LLM.panel.modelSelection',
              dm: '模型选择',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.LLM.panel.configureModel',
                dm: '请自行配置模型，根据业务场景选择即可。',
              })}
            />
          </div>
          <ModelConfigFormWrap
            disabled={nodesReadOnly}
            variableList={fileVariableList}
            value={props.data.node_param.model_config}
            onChange={changeLLMConfig}
          />
        </Flex>
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.LLM.panel.prompt',
              dm: '提示词',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.LLM.panel.systemInstruction',
                dm: '为模型提供系统级的指令，如人设、约束等。',
              })}
            />
          </div>
          <VarInputTextArea
            disabled={nodesReadOnly}
            onChange={(val) =>
              changeNodeParam({
                sys_prompt_content: val,
              })
            }
            value={props.data.node_param.sys_prompt_content}
            variableList={variableList}
            maxLength={Number.MAX_SAFE_INTEGER}
          />
        </Flex>
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.LLM.panel.userPrompt',
              dm: '用户提示词',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.LLM.panel.interactionContent',
                dm: '用户和模型的交互内容，如要求、指令等。',
              })}
            />
          </div>
          <VarInputTextArea
            disabled={nodesReadOnly}
            onChange={(val) =>
              changeNodeParam({
                prompt_content: val,
              })
            }
            value={props.data.node_param.prompt_content}
            variableList={variableList}
            maxLength={Number.MAX_SAFE_INTEGER}
          />
        </Flex>
        <ShortMemoryForm
          disabled={nodesReadOnly}
          onChange={(val) => changeNodeParam({ short_memory: val })}
          value={props.data.node_param.short_memory}
          variableList={variableListByArrayType}
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.LLM.panel.output',
              dm: '输出',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.LLM.panel.outputContent',
                dm: '模型运行结束后的输出内容。',
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
          onChange={(val) => {
            changeNodeParam({
              try_catch_config: val,
            });
          }}
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
