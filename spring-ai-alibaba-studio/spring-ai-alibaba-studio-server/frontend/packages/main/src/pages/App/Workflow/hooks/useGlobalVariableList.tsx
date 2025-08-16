import $i18n from '@/i18n';
import { IGlobalVariableItem } from '@/types/appManage';
import { useCallback } from 'react';
import { useWorkflowAppStore } from '../context/WorkflowAppProvider';

export const useGlobalVariableList = () => {
  const globalVariableList = useWorkflowAppStore(
    (state) => state.globalVariableList,
  );
  const setGlobalVariableList = useWorkflowAppStore(
    (state) => state.setGlobalVariableList,
  );

  const initGlobalVariableList = useCallback(
    (list: IGlobalVariableItem[]) => {
      setGlobalVariableList(
        !list?.length
          ? []
          : [
              {
                label: $i18n.get({
                  id: 'main.pages.App.Workflow.hooks.useGlobalVariableList.index.conversationVariable',
                  dm: '会话变量',
                }),
                nodeId: 'conversation',
                nodeType: 'conversation',
                children: list.map((item) => ({
                  label: item.key,
                  value: `\${conversation.${item.key}}`,
                  type: item.type,
                })),
              },
            ],
      );
    },
    [setGlobalVariableList],
  );

  return {
    globalVariableList,
    initGlobalVariableList,
  };
};
