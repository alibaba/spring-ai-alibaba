import $i18n from '@/i18n';
import { IconFont } from '@spark-ai/design';
import {
  OutputParamsTree,
  VarInputTextArea,
  useNodeDataUpdate,
  useNodesOutputParams,
  useReactFlowStore,
} from '@spark-ai/flow';
import React, { memo, useCallback, useMemo } from 'react';
import ErrorCatchForm from '../../components/ErrorCatchForm';
import ModelConfigForm from '../../components/ModelConfigForm';
import RetryForm from '../../components/RetryForm';
import ShortMemoryForm from '../../components/ShortMemoryForm';
import { ILLMNodeData, ILLMNodeParam } from '../../types/flow';

export default memo((props: { id: string; data: ILLMNodeData }) => {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const { getVariableList } = useNodesOutputParams();
  const nodes = useReactFlowStore((store) => store.nodes);
  const edges = useReactFlowStore((store) => store.edges);

  const variableList = useMemo(() => {
    return getVariableList({
      nodeId: props.id,
    });
  }, [props.id, nodes, edges]);

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
    [props.data.node_param],
  );

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.LLM.panel.modelSelection',
            dm: '模型选择',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <ModelConfigForm
          onChange={(payload) => {
            changeNodeParam({
              model_config: {
                ...props.data.node_param.model_config,
                ...payload,
              },
            });
          }}
          value={props.data.node_param.model_config}
        />

        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.LLM.panel.prompt',
            dm: '提示词',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <VarInputTextArea
          onChange={(val) => changeNodeParam({ sys_prompt_content: val })}
          value={props.data.node_param.sys_prompt_content}
          variableList={variableList}
        />
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.LLM.panel.userPrompt',
            dm: '用户提示词',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <VarInputTextArea
          onChange={(val) => changeNodeParam({ prompt_content: val })}
          value={props.data.node_param.prompt_content}
          variableList={variableList}
        />
        <ShortMemoryForm
          onChange={(val) => changeNodeParam({ short_memory: val })}
          value={props.data.node_param.short_memory}
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.LLM.panel.output',
            dm: '输出',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <OutputParamsTree data={props.data.output_params} />
      </div>
      <div className="spark-flow-panel-form-section">
        <RetryForm
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
          value={props.data.node_param.try_catch_config}
          onChange={(val) =>
            changeNodeParam({
              try_catch_config: val,
            })
          }
        />
      </div>
    </>
  );
});
