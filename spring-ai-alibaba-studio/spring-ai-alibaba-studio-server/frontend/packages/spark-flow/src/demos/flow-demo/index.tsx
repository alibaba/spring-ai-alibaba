import $i18n from '@/i18n';
import { bailianTheme, ConfigProvider } from '@spark-ai/design';
import {
  ConfigPanel,
  Flow,
  FlowAside,
  FlowPanel,
  FlowTools,
  ReactFlowProvider,
  TaskStatus,
  WorkflowContextProvider,
} from '@spark-ai/flow';
import { useMount } from 'ahooks';
import { Modal } from 'antd';
import enUS from 'antd/locale/en_US';
import jaJP from 'antd/locale/ja_JP';
import zhCN from 'antd/locale/zh_CN';
import React, { memo, useState } from 'react';
import { dispatch } from 'use-bus';
import { iconFontUrl } from './components/Icon';
import SingleNodeDrawer from './components/SingleNodeDrawer';
import TestPanel from './components/TestPanel';
import WorkFlowHeader from './components/WorkFlowHeader';
import getConfigPanel from './nodes/configPanelsMap';
import NODE_COMPONENT_MAP from './nodes/constant';
import { NODE_SCHEMA_MAP } from './nodes/nodeSchema';
import { transformToFlowData } from './uitls/transform';

// Get current language preset
const langPreset = $i18n.getCurrentLanguage();

const SparkFlow = () => {
  const [openTestPanel, setOpenTestPanel] = useState(false);

  const locale = {
    zh: zhCN,
    en: enUS,
    ja: jaJP,
  }[langPreset];

  useMount(() => {
    setTimeout(() => {
      dispatch({
        type: 'update-flow-data',
        data: transformToFlowData({
          nodes: [
            {
              id: 'Start_Ek0w',
              name: $i18n.get({
                id: 'spark-flow.demos.spark-flow-1.index.start',
                dm: '开始',
              }),
              config: {
                input_params: [],
                output_params: [
                  {
                    key: 'city',
                    type: 'String',
                    desc: $i18n.get({
                      id: 'spark-flow.demos.spark-flow-1.index.city',
                      dm: '城市',
                    }),
                  },
                  {
                    key: 'date',
                    type: 'String',
                    desc: $i18n.get({
                      id: 'spark-flow.demos.spark-flow-1.index.date',
                      dm: '日期',
                    }),
                  },
                ],

                node_param: {},
              },
              position: {
                x: 0,
                y: 0,
              },
              width: 320,
              type: 'Start',
            },
            {
              id: 'End_Gf7f',
              name: $i18n.get({
                id: 'spark-flow.demos.spark-flow-1.index.end',
                dm: '结束',
              }),
              config: {
                input_params: [],
                output_params: [],
                node_param: {
                  output_type: 'text',
                  text_template: '${sys.query}',
                  json_params: [],
                  stream_switch: false,
                },
              },
              position: {
                x: 840,
                y: 0,
              },
              width: 320,
              type: 'End',
            },
            {
              id: 'LLM_pBjJ',
              name: $i18n.get({
                id: 'spark-flow.demos.spark-flow-1.index.largeModel1',
                dm: '大模型1',
              }),
              config: {
                input_params: [],
                output_params: [
                  {
                    key: 'output',
                    type: 'String',
                    desc: $i18n.get({
                      id: 'spark-flow.demos.spark-flow-1.index.textOutput',
                      dm: '文本输出',
                    }),
                  },
                ],

                node_param: {
                  sys_prompt_content: $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.index.youAreAAssistant',
                    dm: '你是一个小助理',
                  }),
                  prompt_content: '${sys.query}',
                  short_memory: {
                    enabled: true,
                    type: 'self',
                    round: 29,
                    param: {
                      key: 'historyList',
                      type: 'Array<String>',
                      value_from: 'refer',
                    },
                  },
                  model_config: {
                    model_id: 'qwen-max',
                    model_name: '',
                    provider: '87a5a76e',
                    params: [],
                  },
                  retry_config: {
                    retry_enabled: true,
                    max_retries: 3,
                    retry_interval: 6900,
                  },
                  try_catch_config: {
                    strategy: 'noop',
                    default_values: [
                      {
                        key: 'output',
                        type: 'String',
                        desc: $i18n.get({
                          id: 'spark-flow.demos.spark-flow-1.index.textOutput',
                          dm: '文本输出',
                        }),
                      },
                    ],
                  },
                },
              },
              position: {
                x: 420,
                y: 0,
              },
              width: 320,
              type: 'LLM',
            },
          ],

          edges: [
            {
              id: 'Start_Ek0w-Start_Ek0w-LLM_pBjJ-LLM_pBjJ',
              source: 'Start_Ek0w',
              target: 'LLM_pBjJ',
              source_handle: 'Start_Ek0w',
              target_handle: 'LLM_pBjJ',
            },
            {
              id: 'LLM_pBjJ-LLM_pBjJ-End_Gf7f-End_Gf7f',
              source: 'LLM_pBjJ',
              target: 'End_Gf7f',
              source_handle: 'LLM_pBjJ',
              target_handle: 'End_Gf7f',
            },
          ],
        }),
      });
    }, 1000);
  });

  const handleClickAction = (eventType: string) => {
    switch (eventType) {
      case 'test':
        setOpenTestPanel(true);
        break;
    }
  };
  return (
    <ConfigProvider
      {...bailianTheme}
      prefix="spark-flow"
      prefixCls="spark-ant"
      locale={locale}
      iconfont={iconFontUrl}
      style={{
        height: '100vh',
        width: '100vw',
      }}
    >
      <WorkflowContextProvider
        initialState={{
          nodeSchemaMap: NODE_SCHEMA_MAP,
          getConfigPanel: getConfigPanel,
          onAddCustomNode: (data) => {
            return new Promise((resolve) => {
              Modal.confirm({
                title: $i18n.get({
                  id: 'spark-flow.demos.spark-flow-1.index.addNode',
                  dm: '添加节点',
                }),
                content: $i18n.get({
                  id: 'spark-flow.demos.spark-flow-1.index.enterNodeName',
                  dm: '请输入节点名称',
                }),
                onOk: () => {
                  resolve(data);
                },
              });
            });
          },
        }}
      >
        <ReactFlowProvider>
          <div className="flex flex-col h-full w-full">
            <WorkFlowHeader onClickAction={handleClickAction} />
            <TaskStatus />
            <div className="flex flex-1 h-[1px] relative">
              <FlowAside />
              <div className="relative flex-1">
                <Flow nodeTypes={NODE_COMPONENT_MAP} />
                <FlowTools />
                <FlowPanel>
                  <ConfigPanel singleTestPanel={SingleNodeDrawer} />
                  <TestPanel open={openTestPanel} setOpen={setOpenTestPanel} />
                </FlowPanel>
              </div>
            </div>
          </div>
        </ReactFlowProvider>
      </WorkflowContextProvider>
    </ConfigProvider>
  );
};

export default memo(SparkFlow);
