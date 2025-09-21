import $i18n from '@/i18n';
import {
  JudgeForm,
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
} from '@spark-ai/flow';
import { memo, useCallback, useMemo } from 'react';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import { IJudgeNodeData, IJudgeNodeParam } from '../../types';

export default memo(function JudgePanel(props: {
  id: string;
  data: IJudgeNodeData;
}) {
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
    (payload: Partial<IJudgeNodeParam>) => {
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
    [props.data.node_param, handleNodeDataUpdate, props.id],
  );

  return (
    <>
      <JudgeForm
        leftVariableList={variableList}
        rightVariableList={variableList}
        value={props.data.node_param.branches}
        onChange={(val) => changeNodeParam({ branches: val })}
        disabled={nodesReadOnly}
      />

      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'main.pages.App.Workflow.nodes.Judge.panel.other',
            dm: '其他',
          })}
        </div>
      </div>
    </>
  );
});
