import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import {
  SELECTED_MODEL_PARAMS_DEFAULT,
  SHORT_MEMORY_CONFIG_DEFAULT,
} from '../../constant';
import { IClassifierNodeData, IClassifierNodeParam } from '../../types';
import {
  checkLLMData,
  checkShortMemory,
  getVariablesFromText,
  transformInputParams,
} from '../../utils';

const checkClassifyNodeDataValid = (data: IClassifierNodeData) => {
  const errorMsg: { label: string; error: string }[] = [];
  if (!data.input_params[0].value) {
    errorMsg.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.Classifier.schema.inputVariable',
        dm: '输入变量',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.Classifier.schema.notNull',
        dm: '不能为空',
      }),
    });
  }

  checkLLMData(data.node_param.model_config, errorMsg);

  if (
    data.node_param.conditions.some(
      (item) => !item.subject && item.id !== 'default',
    )
  ) {
    errorMsg.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.Classifier.schema.intentionClassification',
        dm: '意图分类',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.Classifier.schema.notNull',
        dm: '不能为空',
      }),
    });
  }
  checkShortMemory(data.node_param.short_memory, errorMsg);
  return errorMsg;
};

const getClassifyVariables = (data: IClassifierNodeData) => {
  const variableKeyMap: Record<string, boolean> = {};
  const { input_params = [] } = data;
  transformInputParams(input_params, variableKeyMap);
  getVariablesFromText(data.node_param.instruction, variableKeyMap);
  if (data.node_param.model_config.vision_config.enable) {
    transformInputParams(
      data.node_param.model_config.vision_config.params,
      variableKeyMap,
    );
  }

  data.node_param.conditions.forEach((item) => {
    getVariablesFromText(item.subject, variableKeyMap);
  });

  return Object.keys(variableKeyMap);
};

export const ClassifierSchema: INodeSchema = {
  type: 'Classifier',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Classifier.schema.intentionClassification',
    dm: '意图分类',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Classifier.schema.callLargeModelForIntentionJudgment',
    dm: '调用大模型，根据设定进行意图判断以决定分支执行。',
  }),
  iconType: 'spark-effciency-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Classifier.schema.logic',
    dm: '逻辑',
  }),
  defaultParams: {
    input_params: [
      {
        key: 'input',
        type: 'String',
        value_from: 'refer',
        value: void 0,
      },
    ],

    node_param: {
      conditions: [
        {
          id: 'default',
          subject: '',
        },
      ],

      model_config: SELECTED_MODEL_PARAMS_DEFAULT,
      short_memory: SHORT_MEMORY_CONFIG_DEFAULT,
      instruction: '',
      mode_switch: 'efficient',
    } as IClassifierNodeParam,
    output_params: [
      {
        key: 'subject',
        type: 'String',
        desc: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.Classifier.schema.hitSubject',
          dm: '命中主题',
        }),
      },
      {
        key: 'thought',
        type: 'String',
        desc: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.Classifier.schema.thinkingProcess',
          dm: '思考过程',
        }),
      },
    ],
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-orange-hover)',
  checkValid: (data) => checkClassifyNodeDataValid(data as IClassifierNodeData),
  getRefVariables: (data) => getClassifyVariables(data as IClassifierNodeData),
};
