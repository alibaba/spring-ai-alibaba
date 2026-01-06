import $i18n from '@/i18n';
import { IconButton, IconFont } from '@spark-ai/design';
import { useStore } from '@spark-ai/flow';
import { Tooltip } from 'antd';
import { memo } from 'react';

export default memo(function TaskResultShowBtn() {
  const taskStore = useStore((store) => store.taskStore);
  const showResults = useStore((store) => store.showResults);
  const setShowResults = useStore((store) => store.setShowResults);
  if (!taskStore || showResults) return null;
  return (
    <Tooltip
      title={$i18n.get({
        id: 'main.pages.App.Workflow.components.TaskResultShowBtn.index.showTestResult',
        dm: '展示测试结果',
      })}
    >
      <IconButton
        icon={<IconFont type="spark-visable-line" />}
        onClick={() => {
          setShowResults(true);
        }}
      />
    </Tooltip>
  );
});
