import $i18n from '@/i18n';
import Welcome from '@/pages/App/AssistantAppEdit/components/SparkChat/components/Welcome';
import {
  createWorkFlowTask,
  getWorkFlowTaskProcess,
  resumeWorkFlowTask,
} from '@/services/workflow';
import {
  ChatAnywhere,
  ChatAnywhereRef,
  createCard,
  DefaultCards,
  TMessage,
  uuid,
} from '@spark-ai/chat';
import { copy, IconButton, IconFont } from '@spark-ai/design';
import {
  IWorkFlowTaskProcess,
  useFlowDebugInteraction,
  useFlowInteraction,
  useStore,
} from '@spark-ai/flow';
import { useMount, useUnmount } from 'ahooks';
import { Flex, message, Tooltip } from 'antd';
import classNames from 'classnames';
import { compact } from 'lodash-es';
import { memo, useRef, useState } from 'react';
import { IWorkflowDebugInputParamItem } from '../../context';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import { NodeResultPanelList } from '../NodeResultPanel';
import { getSparkFlowUsageList, TextCard } from '../TaskTestPanel';
import { InputParamsFormDrawer } from '../TaskTestPanel/InputParamsForm';
import UserInputForm from '../UserInputForm';
import styles from './index.module.less';
const ChatFormCard = (props: any) => {
  return (
    <UserInputForm
      nodeData={props.data}
      onSubmit={(params) => props.context.onInput({ params, type: 'resume' })}
    />
  );
};

const NodeResultPanelListWrap = (props: any) => {
  return (
    <NodeResultPanelList
      data={props.data.results}
      statusInfo={props.data.statusInfo}
    />
  );
};

interface IAnswer extends TMessage {
  task_id: string;
}

export default memo(function ChatTestPanel() {
  const inputParams = useWorkflowAppStore((state) => state.debugInputParams);
  const appId = useWorkflowAppStore((state) => state.appId);
  const setShowResults = useStore((state) => state.setShowResults);
  const taskStore = useStore((state) => state.taskStore);
  const { updateTaskStore } = useFlowDebugInteraction();
  const { focusElement } = useFlowInteraction();
  const [conversationId, setConversationId] = useState(
    void 0 as string | undefined,
  );
  const [loading, setLoading] = useState(false);
  const setSelectedNode = useStore((state) => state.setSelectedNode);
  const [showInputParamsForm, setShowInputParamsForm] = useState(false);
  const cacheAnimateNodeId = useRef('');
  const currentQA = useRef({
    answer: void 0 as IAnswer | void,
    query: {
      id: '',
      cards: [] as TMessage['cards'],
      content: '',
      role: 'user',
      inputs: [] as IWorkflowDebugInputParamItem[],
      msgStatus: 'finished',
    },
  });

  const timer = useRef(null as NodeJS.Timeout | null);
  const isDestroy = useRef(false);
  const chatRef = useRef<ChatAnywhereRef>(null);

  const updateMessage = function (msg: TMessage) {
    const messages = chatRef.current?.updateMessage(msg);
    return messages;
  };

  const clearTimer = () => {
    if (timer.current) {
      clearTimeout(timer.current);
      timer.current = null;
    }
  };

  const generateWelcomeCard = () => {
    if (!chatRef.current) return;
    const welcomeCard = createCard('welcome', {
      modalType: 'textDialog',
    });
    chatRef.current.updateMessage({
      id: 'welcome',
      cards: [welcomeCard],
      role: 'assistant',
      content: '',
    });
  };

  useMount(() => {
    generateWelcomeCard();
  });

  useUnmount(() => {
    isDestroy.current = true;
    clearTimer();
  });

  const onRegenerate = (msg: IAnswer) => {
    chatRef.current?.removeMessage(msg);
    const messages = chatRef.current?.getMessages();
    if (!messages) return;
    const lastQuestion = messages[messages.length - 1];
    onInput({
      query: lastQuestion.content,
      type: 'regenerate',
    });
  };

  const generateFooterCard = (msg: IAnswer, isLatestMsg = true) => {
    return createCard('Footer', {
      right: (
        <DefaultCards.FooterActions
          data={compact([
            {
              icon: (
                <Tooltip
                  placement="top"
                  title={$i18n.get({
                    id: 'main.pages.App.Workflow.components.ChatTestPanel.index.clickCopyRequestId',
                    dm: '点击复制Request ID',
                  })}
                  trigger={'hover'}
                >
                  <IconFont type="spark-debug-line" />
                </Tooltip>
              ),

              label: '',
              onClick: () => {
                copy(msg.task_id);
                message.success(
                  $i18n.get({
                    id: 'main.pages.App.Workflow.components.ChatTestPanel.index.copySuccess',
                    dm: '复制成功',
                  }),
                );
              },
            },
            isLatestMsg && {
              icon: (
                <Tooltip
                  placement="top"
                  title={$i18n.get({
                    id: 'main.pages.App.Workflow.components.ChatTestPanel.index.regenerate',
                    dm: '重新生成',
                  })}
                  trigger={'hover'}
                >
                  <IconFont type="spark-replace-line" size="small"></IconFont>
                </Tooltip>
              ),

              label: '',
              onClick: () => {
                const targetMsg = chatRef.current?.getMessage(msg.id);
                msg && onRegenerate(targetMsg as IAnswer);
              },
            },
          ])}
        />
      ),
    });
  };

  const updateDebugMessages = (data: IWorkFlowTaskProcess) => {
    if (!currentQA.current.answer) return;
    updateTaskStore(data);

    if (['fail', 'success'].includes(data.task_status)) {
      currentQA.current.answer.msgStatus = 'finished';
    }
    if (data.task_status === 'stop') {
      currentQA.current.answer.msgStatus = 'interrupted';
    }

    const { task_results } = data;
    const taskResultCard: TMessage['cards'] = [];
    task_results.forEach((item) => {
      if (item.node_type === 'Input') {
        taskResultCard.push(createCard('formCard', item));
      } else {
        taskResultCard.push(
          createCard('textCard', {
            text: item.node_content || ('' as string),
          }),
        );
      }
    });

    currentQA.current.answer.cards = compact([
      createCard('nodeResultCard', {
        results: data.node_results,
        statusInfo: {
          usages: getSparkFlowUsageList(data),
          status: data.task_status,
          execTime: data.task_exec_time,
        },
      }),
      ...taskResultCard,
      ['fail', 'success', 'stop'].includes(data.task_status) &&
        generateFooterCard(currentQA.current.answer as IAnswer),
    ]);

    updateMessage(currentQA.current.answer as TMessage);

    if (
      task_results.some(
        (item) => item.node_type === 'Input' && item.node_status === 'pause',
      )
    ) {
      setTimeout(() => {
        chatRef.current?.scrollToBottom();
      }, 200);
    }
  };

  const queryTaskStatus = () => {
    clearTimer();
    if (!currentQA.current.answer) return;
    if (!currentQA.current.answer.task_id) return;
    getWorkFlowTaskProcess({
      task_id: currentQA.current.answer?.task_id,
    }).then((res) => {
      if (
        !currentQA.current.answer ||
        currentQA.current.answer.msgStatus === 'interrupted' ||
        isDestroy.current
      )
        return;
      const endNode = res.node_results[res.node_results.length - 1];
      if (endNode.node_id !== cacheAnimateNodeId.current) {
        focusElement({ nodeId: endNode.node_id });
        cacheAnimateNodeId.current = endNode.node_id;
      }
      updateDebugMessages(res);
      if (res.task_status === 'executing') {
        timer.current = setTimeout(() => {
          queryTaskStatus();
        }, 500);
      } else {
        if (res.task_status !== 'pause') {
          setLoading(false);
          chatRef.current?.setLoading(false);
        }
        clearTimer();
      }
    });
  };

  const chat = (task_id: string) => {
    currentQA.current.answer = {
      id: uuid(),
      cards: [],
      task_id,
      content: '',
      msgStatus: 'generating',
      role: 'assistant',
    };
    updateMessage(currentQA.current.answer as TMessage);

    queryTaskStatus();
  };

  const onInput = ({ query, params, type }: any) => {
    setShowResults(true);
    setSelectedNode(null);
    if (type === 'resume') {
      if (!currentQA.current.answer) return;
      resumeWorkFlowTask({
        app_id: appId,
        task_id: currentQA.current.answer.task_id,
        ...params,
      }).then(() => {
        queryTaskStatus();
      });
      return;
    }

    if (!conversationId) {
      chatRef.current?.removeAllMessages();
    }

    if (type === 'regenerate' && currentQA.current.answer) {
      currentQA.current.answer.cards = (
        currentQA.current.answer.cards || []
      ).map((item) => {
        if (item.code === 'Footer') {
          return generateFooterCard(currentQA.current.answer as IAnswer, false);
        }
        return item;
      });
    }

    // clear answer when regenerate
    setLoading(true);
    chatRef.current?.setLoading(true);
    currentQA.current.answer = undefined;
    if (type !== 'regenerate')
      currentQA.current.query = {
        id: uuid(),
        cards: [],
        content: query,
        role: 'user',
        msgStatus: 'finished',
        inputs: inputParams.map((item) => {
          if (item.source === 'sys' && item.key === 'query')
            return {
              ...item,
              value: query,
            };
          return item;
        }),
      };

    updateMessage(currentQA.current.query as TMessage);
    createWorkFlowTask({
      conversation_id: conversationId,
      app_id: appId,
      inputs: currentQA.current.query.inputs,
    }).then((res) => {
      if (conversationId !== res.conversation_id)
        setConversationId(res.conversation_id);
      chat(res.task_id);
    });
  };

  const onStop = () => {
    clearTimer();
    setLoading(false);
    chatRef.current?.setLoading(false);
    if (currentQA.current.answer) {
      const newTaskStore = {
        ...taskStore,
        task_status: 'stop',
        node_results: taskStore?.node_results?.map((item) => {
          return {
            ...item,
            node_status: ['executing', 'pause'].includes(item.node_status)
              ? 'stop'
              : item.node_status,
          };
        }),
        task_results: taskStore?.task_results?.map((item) => {
          return {
            ...item,
            node_status: ['executing', 'pause'].includes(item.node_status)
              ? 'stop'
              : item.node_status,
          };
        }),
      } as IWorkFlowTaskProcess;
      updateDebugMessages(newTaskStore);
    }
  };

  return (
    <>
      <Flex className="h-full" vertical>
        <Flex gap={8} className={styles['header']}>
          <Tooltip
            placement="bottom"
            title={$i18n.get({
              id: 'main.pages.App.Workflow.components.ChatTestPanel.index.paramConfig',
              dm: '入参参数配置',
            })}
          >
            <IconButton
              icon={<IconFont type="spark-setting-line" size="small" />}
              bordered={false}
              onClick={() => setShowInputParamsForm(true)}
            ></IconButton>
          </Tooltip>
          <Tooltip
            title={
              loading
                ? $i18n.get({
                    id: 'main.pages.App.Workflow.components.ChatTestPanel.index.dialogGenerating',
                    dm: '正在进行对话中，请暂停',
                  })
                : $i18n.get({
                    id: 'main.pages.App.Workflow.components.ChatTestPanel.index.clearHistory',
                    dm: '清空记录',
                  })
            }
          >
            <IconButton
              icon={<IconFont type="spark-clear-line" size="small" />}
              bordered={false}
              disabled={loading}
              onClick={() => {
                setConversationId(void 0);
                chatRef.current?.removeAllMessages();
                generateWelcomeCard();
              }}
            />
          </Tooltip>
        </Flex>
        <div className={classNames('flex-1 h-[1px]', styles['chat-container'])}>
          <ChatAnywhere
            cardConfig={{
              nodeResultCard: NodeResultPanelListWrap,
              formCard: ChatFormCard,
              textCard: TextCard,
              welcome: Welcome,
            }}
            uiConfig={{
              background: 'transparent',
            }}
            onInput={onInput}
            onStop={onStop}
            ref={chatRef}
          ></ChatAnywhere>
        </div>
      </Flex>
      {showInputParamsForm && (
        <InputParamsFormDrawer
          disableShowQuery
          onClose={() => setShowInputParamsForm(false)}
        />
      )}
    </>
  );
});
