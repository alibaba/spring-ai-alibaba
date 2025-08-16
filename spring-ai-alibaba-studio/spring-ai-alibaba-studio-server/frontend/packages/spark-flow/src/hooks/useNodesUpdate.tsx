import { IWorkFlowNodeData } from '@/types/work-flow';
import { useStoreApi } from '@xyflow/react';
import { useCallback } from 'react';
import { useFlowSave } from './useFlowSave';
import { useNodesReadOnly } from './useWorkFlow';

type NodeDataUpdatePayload = {
  id: string;
  data: Partial<IWorkFlowNodeData>;
};

export const useNodeDataUpdate = () => {
  const store = useStoreApi();
  const { handleSaveFlowDraft } = useFlowSave();
  const { getNodesReadOnly } = useNodesReadOnly();

  const handleNodeDataUpdate = useCallback(
    ({ id, data }: NodeDataUpdatePayload) => {
      if (getNodesReadOnly()) return;

      const { nodes, setNodes } = store.getState();
      const newNodes = nodes.map((node) => {
        if (node.id === id) {
          return {
            ...node,
            data: { ...node.data, ...data },
          };
        }
        return node;
      });
      setNodes(newNodes);
      handleSaveFlowDraft();
    },
    [store, handleSaveFlowDraft],
  );

  return {
    handleNodeDataUpdate,
  };
};
