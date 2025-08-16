import { ISparkChatRef } from '@/pages/App/AssistantAppEdit/components/SparkChat';
import {
  IAssistantAppDetailWithInfos,
  IAssistantConfig,
  IAssistantConfigWithInfos,
} from '@/types/appManage';
import { SetState } from 'ahooks/lib/useSetState';
import { DebouncedFunc } from 'lodash-es';
import React from 'react';
import { IAssistantAppEditState } from '.';

export const AssistantAppContext = React.createContext<{
  appState: IAssistantAppEditState;
  setAppState: SetState<IAssistantAppEditState>;
  onAppConfigChange: (payload: Partial<IAssistantConfigWithInfos>) => void;
  onAppChange: (
    payload: Partial<IAssistantAppDetailWithInfos>,
    disableSave?: boolean,
  ) => void;
  autoSave: DebouncedFunc<() => void>;
  appCode?: string;
  getSaveData: () => IAssistantConfig | null;
  refreshAppDetail: () => Promise<void>;
  sparkChatComponentRef: React.MutableRefObject<ISparkChatRef | null>;
  // @ts-ignore
}>({});
