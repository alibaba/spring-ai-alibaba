import React, { createContext, useContext, useRef } from 'react';
import { useStore } from 'zustand';
import { createWorkflowAppStore, IWorkflowAppState } from './index';

type WorkflowAppStore = ReturnType<typeof createWorkflowAppStore>;

interface WorkflowAppContextProps {
  children: React.ReactNode;
  initialState?: Partial<IWorkflowAppState>;
}

const WorkflowContext = createContext<WorkflowAppStore | null | undefined>(
  null,
);

export const WorkflowAppProvider: React.FC<WorkflowAppContextProps> = ({
  children,
  initialState = {},
}) => {
  const storeRef = useRef<WorkflowAppStore>();

  if (!storeRef.current)
    storeRef.current = createWorkflowAppStore(initialState);

  return (
    <WorkflowContext.Provider value={storeRef.current}>
      {children}
    </WorkflowContext.Provider>
  );
};

export function useWorkflowAppStore<T>(
  selector: (state: IWorkflowAppState) => T,
) {
  const store = useContext(WorkflowContext);
  if (!store) {
    throw new Error(
      'useWorkflowContext must be used within a WorkflowProvider',
    );
  }
  return useStore(store, selector);
}
