import CustomIcon from '@/components/CustomIcon';
import { useFlowDebugInteraction } from '@/hooks/useFlowDebugInteraction';
import $i18n from '@/i18n';
import { Button, IconButton } from '@spark-ai/design';
import { CheckListBtn, useStore } from '@spark-ai/flow';
import { Segmented, Tooltip } from 'antd';
import React, { memo } from 'react';
import './index.less';
import LangSelect from './LangSelect';

interface IProps {
  onClickAction: (eventType: string) => void;
}

export default memo(function WorkFlowHeader(props: IProps) {
  const { updateTaskStore, clearTaskStore } = useFlowDebugInteraction();
  const setShowResults = useStore((store) => store.setShowResults);
  const showResults = useStore((store) => store.showResults);
  const taskStore = useStore((store) => store.taskStore);
  return (
    <div className="spark-flow-header flex gap-[16px] items-center">
      <div className="spark-flow-header-item"></div>
      <div className="spark-flow-header-tabs">
        <Segmented
          options={[
            {
              label: $i18n.get({
                id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.canvasConfiguration',
                dm: '画布',
              }),
              value: 'config',
            },
            {
              label: $i18n.get({
                id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.publishChannels',
                dm: '发布',
              }),
              value: 'channel',
            },
          ]}
        />
      </div>
      <div className="spark-flow-header-item flex gap-[12px] justify-end">
        <Button
          onClick={() => {
            setShowResults(true);
            updateTaskStore({
              task_id: '4ede2bf9-0715-4398-90ea-d492542f7a0a',
              request_id: 'ed0d9119-6c88-4661-a753-5f03f05a8e35',
              task_status: 'success',
              task_results: [
                {
                  node_type: 'End',
                  node_name: $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.end',
                    dm: '结束',
                  }),
                  node_id: 'End_Gf7f',
                  node_content: $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.hello',
                    dm: '你好',
                  }),
                  node_status: 'success',
                },
              ],

              task_exec_time: '1670ms',
              node_results: [
                {
                  input: $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.helloSysQueryUser',
                    dm: '{"sys":{"query":"你好"},"user":{}}',
                  }),
                  output: $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.helloSysQueryUser',
                    dm: '{"sys":{"query":"你好"},"user":{}}',
                  }),
                  batches: [],
                  is_multi_branch: false,
                  node_id: 'Start_Ek0w',
                  node_name: $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.start',
                    dm: '开始',
                  }),
                  node_type: 'Start',
                  node_status: 'success',
                  node_exec_time: '3ms',
                  output_type: 'json',
                  is_batch: false,
                },
                {
                  input: $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.startInput',
                    dm: '{"input":{"provider":"87a5a76e","modelId":"qwen-max","messages":[{"message_type":"SYSTEM","metadata":{"messageType":"SYSTEM"},"text":"你是一个小助理"},{"message_type":"USER","metadata":{"messageType":"USER"},"media":[],"text":"你好"}],"params":{}}}',
                  }),

                  output: $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.startOutput',
                    dm: '{"output":"你好！有什么我能帮助你的吗？"}',
                  }),
                  usages: [
                    {
                      prompt_tokens: 18,
                      completion_tokens: 8,
                      total_tokens: 26,
                    },
                  ],

                  batches: [],
                  is_multi_branch: false,
                  node_id: 'LLM_pBjJ',
                  node_name: $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.largeModel1',
                    dm: '大模型1',
                  }),
                  node_type: 'LLM',
                  node_status: 'executing',
                  node_exec_time: '1273ms',
                  short_memory: {
                    round: 20,
                    current_self_chat_messages: [
                      {
                        role: 'user',
                        content: $i18n.get({
                          id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.hello',
                          dm: '你好',
                        }),
                      },
                      {
                        role: 'assistant',
                        content: $i18n.get({
                          id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.helloWhatCanIHelpYou',
                          dm: '你好！有什么我能帮助你的吗？',
                        }),
                      },
                    ],
                  },
                  output_type: 'json',
                  is_batch: false,
                },
                {
                  output: $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.hello',
                    dm: '你好',
                  }),
                  batches: [],
                  is_multi_branch: false,
                  node_id: 'End_Gf7f',
                  node_name: $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.end',
                    dm: '结束',
                  }),
                  node_type: 'End',
                  node_status: 'success',
                  node_exec_time: '3ms',
                  output_type: 'text',
                  is_batch: false,
                },
              ],
            });
          }}
        >
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.test',
            dm: '测试',
          })}
        </Button>
        <Button onClick={clearTaskStore}>
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.clear',
            dm: '清除',
          })}
        </Button>
        <Tooltip
          title={$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.displayTestResults',
            dm: '展示测试结果',
          })}
        >
          {!!taskStore && !showResults && (
            <IconButton
              icon={<CustomIcon type="spark-visable-line" />}
              onClick={() => {
                setShowResults(true);
              }}
            />
          )}
        </Tooltip>
        <Button onClick={() => props.onClickAction('publish')} type="primary">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.components.WorkFlowHeader.index.publish',
            dm: '发布',
          })}
        </Button>
        <CheckListBtn />
        <LangSelect />
      </div>
    </div>
  );
});
