import $i18n from '@/i18n';
import {
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
  VarInputTextArea,
} from '@spark-ai/flow';
import { Flex, Switch } from 'antd';
import { memo, useCallback, useMemo } from 'react';
import InfoIcon from '../../components/InfoIcon';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import { IOutputNodeData, IOutputNodeParam } from '../../types';

export default memo((props: { id: string; data: IOutputNodeData }) => {
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
    (payload: Partial<IOutputNodeParam>) => {
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
    <div className="spark-flow-panel-form-section">
      <Flex vertical gap={12}>
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'main.pages.App.Workflow.nodes.Output.panel.outputContent',
            dm: '输出内容',
          })}
        </div>
        <VarInputTextArea
          variableList={variableList}
          value={props.data.node_param.output}
          onChange={(val) => changeNodeParam({ output: val })}
          disabled={nodesReadOnly}
          maxLength={Number.MAX_SAFE_INTEGER}
        />
      </Flex>
      <div className="flex-justify-between w-full">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'main.pages.App.Workflow.nodes.Output.panel.streamOutput',
            dm: '流式输出',
          })}

          <InfoIcon
            tip={$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Output.panel.streamMode',
              dm: '开启后，节点的来源于大模型的输出内容将用流式呈现。',
            })}
          />
        </div>
        <Switch
          disabled={nodesReadOnly}
          checked={props.data.node_param.stream_switch}
          onChange={(val) => {
            changeNodeParam({
              stream_switch: val,
            });
          }}
        />
      </div>
    </div>
  );
});
