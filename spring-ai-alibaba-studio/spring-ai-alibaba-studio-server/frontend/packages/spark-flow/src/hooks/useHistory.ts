import { useStore } from '@/flow/context';
import { Edge, useStoreApi } from '@xyflow/react';
import { useCallback } from 'react';
import { IWorkFlowNode } from '../types/work-flow';

interface IHistoryStep {
  edges: Edge[];
  nodes: IWorkFlowNode[];
}

export const useHistory = () => {
  const historySteps = useStore((state) => state.historySteps);
  const setHistorySteps = useStore((state) => state.setHistorySteps);
  const futureSteps = useStore((state) => state.futureSteps);
  const setFutureSteps = useStore((state) => state.setFutureSteps);
  const onDebounceChange = useStore((state) => state.onDebounceChange);
  const store = useStoreApi();

  const canUndo = historySteps.length > 1;

  const canRedo = futureSteps.length > 0;

  const initHistoryStep = useCallback(
    (step: IHistoryStep) => {
      setHistorySteps([step]);
      setFutureSteps([]);
    },
    [setHistorySteps, setFutureSteps],
  );

  const addHistoryStep = useCallback(
    (step: IHistoryStep) => {
      setHistorySteps([...historySteps, step]);
      setFutureSteps([]);
    },
    [historySteps, setHistorySteps, setFutureSteps],
  );

  const undo = useCallback(() => {
    if (!canUndo) return null;
    const removedStep = historySteps[historySteps.length - 1];
    const newHistorySteps = historySteps.slice(0, -1);
    const currentStep = newHistorySteps[newHistorySteps.length - 1];

    setHistorySteps(newHistorySteps);
    setFutureSteps([removedStep, ...futureSteps]);

    const { setNodes, setEdges } = store.getState();
    setNodes(currentStep.nodes);
    setEdges(currentStep.edges);
    onDebounceChange?.({
      nodes: currentStep.nodes,
      edges: currentStep.edges,
    });

    return currentStep;
  }, [historySteps, futureSteps, setHistorySteps, setFutureSteps, canUndo]);

  const redo = useCallback(() => {
    if (!canRedo) return null;

    const nextStep = futureSteps[0];
    const newFutureSteps = futureSteps.slice(1);

    setFutureSteps(newFutureSteps);
    setHistorySteps([...historySteps, nextStep]);

    const { setNodes, setEdges } = store.getState();

    setNodes(nextStep.nodes);
    setEdges(nextStep.edges);
    onDebounceChange?.({
      nodes: nextStep.nodes,
      edges: nextStep.edges,
    });

    return nextStep;
  }, [historySteps, futureSteps, setHistorySteps, setFutureSteps, canRedo]);

  return {
    canUndo,
    canRedo,
    addHistoryStep,
    undo,
    redo,
    initHistoryStep,
  };
};
