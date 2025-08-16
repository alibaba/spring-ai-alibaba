import { useMount } from 'ahooks';
import React, { createContext, useContext, useRef, useState } from 'react';
import { useStore as useZustandStore } from 'zustand';
import $i18n from '../i18n';
import { createWorkflowStore, IWorkFlowStore } from '../store';

type WorkflowStore = ReturnType<typeof createWorkflowStore>;
export const WorkflowContext = createContext<WorkflowStore | null>(null);

type WorkflowProviderProps = {
  children: React.ReactNode;
  initialState?: Partial<IWorkFlowStore>;
  locale?: 'en' | 'zh' | 'ja' | string;
};

const nowLang = $i18n.getCurrentLanguage();

export const WorkflowContextProvider = ({
  children,
  initialState,
  locale,
}: WorkflowProviderProps) => {
  const storeRef = useRef<WorkflowStore>();
  const [key, setKey] = useState<string>(nowLang || 'defaultKey');

  if (!storeRef.current) storeRef.current = createWorkflowStore(initialState);

  useMount(() => {
    if (locale === nowLang) return;
    if (locale) {
      $i18n.setCurrentLanguage(locale);
      setKey(locale);
    }
  });

  return (
    <WorkflowContext.Provider value={storeRef.current}>
      <React.Fragment key={key}>{children}</React.Fragment>
    </WorkflowContext.Provider>
  );
};

export function useStore<T>(selector: (state: IWorkFlowStore) => T): T {
  const store = useContext(WorkflowContext);
  if (!store) throw new Error('Missing WorkflowContext.Provider in the tree');

  return useZustandStore(store, selector);
}

export const useWorkflowStore = () => {
  return useContext(WorkflowContext)!;
};
