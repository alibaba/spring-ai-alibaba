import { IWorkFlowTaskProcess } from '@/types/work-flow';
import { useCallback } from 'react';
import { useStore } from '../flow/context';
import { useEdgesInteraction } from './useEdgesInteraction';

export const useFlowDebugInteraction = () => {
  const setTaskStore = useStore((s) => s.setTaskStore);
  const { updateEdgeByNodeResults, hiddenEdgeStatus } = useEdgesInteraction();

  const updateTaskStore = useCallback(
    (val: IWorkFlowTaskProcess) => {
      setTaskStore(val);
      updateEdgeByNodeResults(val.node_results);
    },
    [setTaskStore, updateEdgeByNodeResults],
  );

  const clearTaskStore = useCallback(() => {
    setTaskStore(void 0);
    hiddenEdgeStatus();
  }, [setTaskStore, hiddenEdgeStatus]);

  return { updateTaskStore, clearTaskStore };
};
