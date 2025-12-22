import $i18n from '@/i18n';
import { Button } from '@spark-ai/design';
import { useStore } from '@spark-ai/flow';
import { Tooltip } from 'antd';
import { memo, useMemo } from 'react';

interface IVersionManageBtnProps {
  setShowHistoryPanel: (show: boolean) => void;
}

export default memo(function VersionManagerBtn(props: IVersionManageBtnProps) {
  const taskStore = useStore((state) => state.taskStore);
  const isFlushing = useMemo(() => {
    return ['executing', 'pause'].includes(taskStore?.task_status || '');
  }, [taskStore?.task_status]);

  return (
    <Tooltip
      trigger={'hover'}
      title={
        isFlushing
          ? $i18n.get({
              id: 'main.pages.App.AssistantAppEdit.components.AppActions.index.dialogProcessingProhibitedSwitchVersion',
              dm: '对话进行中，禁止切换版本',
            })
          : $i18n.get({
              id: 'main.components.HistoryPanel.index.historyVersion',
              dm: '历史版本',
            })
      }
    >
      <Button
        iconType="spark-auditLog-line"
        onClick={() => {
          props.setShowHistoryPanel(true);
        }}
        disabled={isFlushing}
      >
        {$i18n.get({
          id: 'main.pages.App.AssistantAppEdit.components.AppActions.index.versionManagement',
          dm: '版本管理',
        })}
      </Button>
    </Tooltip>
  );
});
