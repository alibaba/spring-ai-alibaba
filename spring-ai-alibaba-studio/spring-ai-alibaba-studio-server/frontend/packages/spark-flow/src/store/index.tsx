import { IVarTreeItem } from '@/components/VariableTreeSelect';
import { Edge, Node } from '@xyflow/react';
import { debounce } from 'lodash-es';
import { createStore } from 'zustand/vanilla';
import {
  ICheckListItem,
  INodeSchema,
  IWorkFlowNode,
  IWorkFlowTaskProcess,
} from '../types/work-flow';

export interface IWorkFlowStore {
  showResults: boolean;
  setShowResults: (val: boolean) => void;
  taskStore?: IWorkFlowTaskProcess;
  setTaskStore: (val: IWorkFlowTaskProcess | undefined) => void;
  debounceSaveDraft: (fn: () => void) => void;
  hiddenMenu: boolean;
  setHiddenMenu: (val: boolean) => void;
  selectedNode: IWorkFlowNode | null;
  setSelectedNode: (val: IWorkFlowNode | null) => void;
  variableTree: Array<IVarTreeItem>;
  setVariableTree: (val: Array<IVarTreeItem>) => void;
  checkList: Array<ICheckListItem>;
  setCheckList: (val: Array<ICheckListItem>) => void;
  nodeSchemaMap: Record<string, INodeSchema>;
  setNodeSchema: (val: Record<string, INodeSchema>) => void;
  getConfigPanel: (val: IWorkFlowNode) => React.ReactElement | null;
  onDebounceChange?: (data: { nodes: IWorkFlowNode[]; edges: Edge[] }) => void;
  onChange: (data: { nodes: Node[]; edges: Edge[] }) => void;
  readyOnly: boolean;
  setReadyOnly: (val: boolean) => void;
  showSingleTest: boolean;
  setShowSingleTest: (val: boolean) => void;
  onAddCustomNode: (data: IWorkFlowNode) => Promise<IWorkFlowNode | null>;
  showMiniMap: boolean;
  setShowMiniMap: (val: boolean) => void;
  futureSteps: Array<{ edges: Edge[]; nodes: IWorkFlowNode[] }>;
  setFutureSteps: (
    val: Array<{ edges: Edge[]; nodes: IWorkFlowNode[] }>,
  ) => void;
  historySteps: Array<{ edges: Edge[]; nodes: IWorkFlowNode[] }>;
  setHistorySteps: (
    val: Array<{ edges: Edge[]; nodes: IWorkFlowNode[] }>,
  ) => void;
  interactiveMode: 'touch' | 'mouse';
  setInteractiveMode: (val: 'touch' | 'mouse') => void;
  isDragging: boolean;
  setIsDragging: (val: boolean) => void;
  showCheckList: boolean;
  setShowCheckList: (val: boolean) => void;
}

export const createWorkflowStore = (
  initialState: Partial<IWorkFlowStore> = {},
) => {
  return createStore<IWorkFlowStore>((set) => ({
    showResults: false,
    setShowResults: (val) => set(() => ({ showResults: val })),
    readyOnly: false,
    setReadyOnly: (val) => set(() => ({ readyOnly: val })),
    taskStore: void 0,
    setTaskStore: (val) => set(() => ({ taskStore: val })),
    debounceSaveDraft: debounce((syncWorkflowDraft) => {
      syncWorkflowDraft();
    }, 5000),
    hiddenMenu: false,
    setHiddenMenu: (val) => set(() => ({ hiddenMenu: val })),
    selectedNode: null,
    setSelectedNode: (val: IWorkFlowNode | null) =>
      set(() => ({ selectedNode: val })),
    variableTree: [],
    setVariableTree: (val) => set(() => ({ variableTree: val })),
    checkList: [],
    setCheckList: (val) => set(() => ({ checkList: val })),
    nodeSchemaMap: {},
    setNodeSchema: (val) => set(() => ({ nodeSchemaMap: val })),
    getConfigPanel: () => null,
    onChange: () => {},
    showSingleTest: false,
    setShowSingleTest: (val) => set(() => ({ showSingleTest: val })),
    onAddCustomNode: () => Promise.resolve(null),
    showMiniMap: false,
    setShowMiniMap: (val) => set(() => ({ showMiniMap: val })),
    futureSteps: [],
    setFutureSteps: (val) => set(() => ({ futureSteps: val })),
    historySteps: [],
    setHistorySteps: (val) => set(() => ({ historySteps: val })),
    interactiveMode: (localStorage.getItem('spark-flow-interactive-mode') ||
      'mouse') as IWorkFlowStore['interactiveMode'],
    setInteractiveMode: (val) => {
      localStorage.setItem('spark-flow-interactive-mode', val);
      set(() => ({ interactiveMode: val }));
    },
    isDragging: false,
    setIsDragging: (val) => set(() => ({ isDragging: val })),
    showCheckList: false,
    setShowCheckList: (val) => set(() => ({ showCheckList: val })),
    ...initialState,
  }));
};
