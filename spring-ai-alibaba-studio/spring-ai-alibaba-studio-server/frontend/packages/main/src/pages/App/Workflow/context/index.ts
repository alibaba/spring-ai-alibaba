import { IValueType, IVarTreeItem } from '@spark-ai/flow';
import { createStore } from 'zustand';

export interface IWorkflowDebugInputParamItem {
  key: string;
  value: any;
  desc?: string;
  required?: boolean;
  source: 'user' | 'sys';
  type: IValueType;
}

export interface IWorkflowAppState {
  appId: string;
  showTest: boolean;
  setAppId: (appId: string) => void;
  setShowTest: (show: boolean) => void;
  /* TestWindow input parameters */
  debugInputParams: IWorkflowDebugInputParamItem[];
  setDebugInputParams: (params: IWorkflowDebugInputParamItem[]) => void;
  globalVariableList: IVarTreeItem[];
  setGlobalVariableList: (list: IVarTreeItem[]) => void;
  selectedVersion: string;
  setSelectedVersion: (version: string) => void;
}

export const createWorkflowAppStore = (
  initialState: Partial<IWorkflowAppState>,
) => {
  return createStore<IWorkflowAppState>((set) => ({
    appId: '',
    showTest: false,
    debugInputParams: [],
    setAppId: (appId) => set({ appId }),
    setShowTest: (show) => set({ showTest: show }),
    setDebugInputParams: (params) => set({ debugInputParams: params }),
    globalVariableList: [],
    setGlobalVariableList: (list) => set({ globalVariableList: list }),
    selectedVersion: 'draft',
    setSelectedVersion: (version) => set({ selectedVersion: version }),
    ...initialState,
  }));
};
