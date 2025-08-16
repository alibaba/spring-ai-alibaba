import $i18n from '@/i18n';
import {
  DragPanel,
  PanelContainer,
  SelectWithDesc,
  useFlowDebugInteraction,
  useStore,
} from '@spark-ai/flow';
import { message } from 'antd';
import { useMemo, useState } from 'react';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import ChatTestPanel from '../ChatTestPanel';
import TaskTestPanel from '../TaskTestPanel';
import styles from './index.module.less';

const TEST_OPTIONS = [
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.components.TestPanel.index.textChat',
      dm: '文本对话',
    }),
    value: 'chat',
    desc: $i18n.get({
      id: 'main.pages.App.Workflow.components.TestPanel.index.chatBasedInteraction',
      dm: '基于LLM的对话型交互，适合进行复杂的多轮对话',
    }),
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.components.TestPanel.index.textGeneration',
      dm: '文本生成',
    }),
    value: 'task',
    desc: $i18n.get({
      id: 'main.pages.App.Workflow.components.TestPanel.index.singleRoundInteraction',
      dm: '面向单轮任务的生成式交互，适合进行信息抽取和文本创作',
    }),
  },
];

export default function TestPanel() {
  const showTest = useWorkflowAppStore((state) => state.showTest);
  const setShowTest = useWorkflowAppStore((state) => state.setShowTest);
  const [testType, setTestType] = useState('task');
  const taskStore = useStore((state) => state.taskStore);
  const { clearTaskStore } = useFlowDebugInteraction();

  const memoTitle = useMemo(() => {
    return (
      <div className="flex gap-[8px] items-center">
        <span>
          {$i18n.get({
            id: 'main.pages.App.Workflow.components.TestPanel.index.test',
            dm: '测试',
          })}
        </span>
        <SelectWithDesc
          className={styles.testTypeSelector}
          onChange={(val) => {
            if (
              taskStore?.task_status === 'executing' ||
              taskStore?.task_status === 'pause'
            ) {
              message.warning(
                $i18n.get({
                  id: 'main.pages.App.Workflow.components.TestPanel.executingWorkflow',
                  dm: '正在执行工作流，请先停止',
                }),
              );
              return;
            }
            setTestType(val);
          }}
          value={testType}
          variant="borderless"
          options={TEST_OPTIONS}
          popupMatchSelectWidth={false}
        />
      </div>
    );
  }, [testType, taskStore?.task_status]);

  const handleClose = () => {
    if (
      taskStore?.task_status === 'executing' ||
      taskStore?.task_status === 'pause'
    ) {
      message.warning(
        $i18n.get({
          id: 'main.pages.App.Workflow.components.TestPanel.executingWorkflow',
          dm: '正在执行工作流，请先停止',
        }),
      );
      return;
    }
    setShowTest(false);
    clearTaskStore();
  };

  if (!showTest) return null;

  return (
    <DragPanel minWidth={500} defaultWidth={500} maxWidth={600}>
      <PanelContainer noPadding onClose={handleClose} title={memoTitle}>
        {testType === 'chat' ? <ChatTestPanel /> : <TaskTestPanel />}
      </PanelContainer>
    </DragPanel>
  );
}
