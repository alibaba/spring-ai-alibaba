import { useStore } from '@/flow/context';
import { IWorkFlowNode } from '@/types/work-flow';
import { useStoreApi } from '@xyflow/react';
import { debounce } from 'lodash-es';
import { useCallback } from 'react';
import { useHistory } from './useHistory';

export const useFlowSave = () => {
  const store = useStoreApi();
  const onFlowDataChange = useStore((state) => state.onChange);
  const onFlowDataDebounceChange = useStore((state) => state.onDebounceChange);
  const { addHistoryStep } = useHistory();

  const handleSaveFlowDraft = useCallback(
    debounce(() => {
      const { nodes, edges } = store.getState();

      addHistoryStep({
        nodes: nodes as IWorkFlowNode[],
        edges,
      });

      if (!onFlowDataDebounceChange) return;
      onFlowDataDebounceChange({ nodes: nodes as IWorkFlowNode[], edges });
    }, 300),
    [store, addHistoryStep, onFlowDataDebounceChange],
  );

  const onFlowChange = useCallback(() => {
    const { nodes, edges } = store.getState();
    onFlowDataChange({ nodes: nodes as IWorkFlowNode[], edges });
    handleSaveFlowDraft();
  }, [handleSaveFlowDraft, store, onFlowDataChange]);

  return {
    handleSaveFlowDraft,
    onFlowChange,
  };
};
