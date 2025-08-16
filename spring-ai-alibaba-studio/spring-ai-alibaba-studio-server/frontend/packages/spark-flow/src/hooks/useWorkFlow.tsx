import { useCallback } from 'react';
import { WorkflowRunningStatus } from '../constant';
import { useStore, useWorkflowStore } from '../flow/context';

export const useNodesReadOnly = () => {
  const workflowStore = useWorkflowStore();
  const taskStore = useStore((s) => s.taskStore);
  const readyOnly = useStore((s) => s.readyOnly);

  const getNodesReadOnly = useCallback(() => {
    const { taskStore } = workflowStore.getState();

    return (
      [WorkflowRunningStatus.Running, WorkflowRunningStatus.Paused].includes(
        taskStore?.task_status || '',
      ) || readyOnly
    );
  }, [workflowStore, readyOnly]);

  return {
    nodesReadOnly:
      [WorkflowRunningStatus.Running, WorkflowRunningStatus.Paused].includes(
        taskStore?.task_status || '',
      ) || readyOnly,
    getNodesReadOnly,
  };
};
