import { initWorkFlowDebug } from '@/services/workflow';
import { useCallback } from 'react';
import { useWorkflowAppStore } from '../context/WorkflowAppProvider';

export const useInitDebug = () => {
  const appId = useWorkflowAppStore((store) => store.appId);
  const showTest = useWorkflowAppStore((store) => store.showTest);
  const setDebugInputParams = useWorkflowAppStore(
    (store) => store.setDebugInputParams,
  );
  const initDebug = useCallback(
    (force = false) => {
      if (!appId || (!showTest && !force)) return;
      initWorkFlowDebug({
        app_id: appId,
      }).then((res) => {
        setDebugInputParams(res);
      });
    },
    [appId, showTest],
  );

  return {
    initDebug,
  };
};
