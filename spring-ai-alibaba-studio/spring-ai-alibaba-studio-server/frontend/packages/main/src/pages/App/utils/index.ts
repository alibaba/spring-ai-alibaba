import $i18n from '@/i18n';
import { IAppType } from '@/services/appComponent';
import { IWorkFlowConfig } from '@/types/appManage';
import uniqueId from '@/utils/uniqueId';
import { IEndNodeParam } from '@spark-ai/flow';

const generateWorkflowConfig = () => {
  return {
    nodes: [
      {
        id: `Start_${uniqueId(4)}`,
        name: $i18n.get({
          id: 'main.pages.App.utils.index.start',
          dm: '开始',
        }),
        type: 'Start',
        position: {
          x: 0,
          y: 0,
        },
        config: {
          input_params: [],
          output_params: [
            {
              key: 'city',
              type: 'String',
              desc: $i18n.get({
                id: 'main.pages.App.utils.index.city',
                dm: '城市',
              }),
            },
            {
              key: 'date',
              type: 'String',
              desc: $i18n.get({
                id: 'main.pages.App.utils.index.date',
                dm: '日期',
              }),
            },
          ],

          node_param: {},
        },
        width: 320,
      },
      {
        id: `End_${uniqueId(4)}`,
        name: $i18n.get({
          id: 'main.pages.App.utils.index.end',
          dm: '结束',
        }),
        type: 'End',
        position: {
          x: 450,
          y: 0,
        },
        config: {
          input_params: [],
          output_params: [],
          node_param: {
            output_type: 'text',
            text_template: '',
            json_params: [],
            stream_switch: false,
          } as IEndNodeParam,
        },
        width: 320,
      },
    ],

    edges: [],
    global_config: {
      history_config: {
        history_max_round: 5,
        history_switch: false,
      },
      variable_config: {
        conversation_params: [],
      },
    },
  } as IWorkFlowConfig;
};

const generateAgentConfig = () => {
  return {
    instructions: '', // system prompt
    tools: {}, // plugin tools
    file_search: {},
    mcp_servers: [], // mcp servers
    agent_components: [], // agent components
    workflow_components: [], // workflow components
  };
};

export const initAppConfig = (appType: IAppType) => {
  switch (appType) {
    // workflow app
    case 'workflow':
      return generateWorkflowConfig();
    // agent app
    case 'basic':
      return generateAgentConfig();
    default:
      return {};
  }
};

/**
 * check whether two arrays are equal
 * @param arr1
 * @param arr2
 * @returns
 */
export function compareArrays(arr1 = [] as any[], arr2 = [] as any[]) {
  if (arr1.length !== arr2.length) {
    return false;
  }
  for (let i = 0; i < arr1.length; i++) {
    if (arr1[i] !== arr2[i]) {
      return false;
    }
  }
  return JSON.stringify(arr1) === JSON.stringify(arr2);
}
