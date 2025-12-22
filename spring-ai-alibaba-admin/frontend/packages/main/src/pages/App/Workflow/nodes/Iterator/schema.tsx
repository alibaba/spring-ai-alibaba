import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import { IIteratorNodeData, IIteratorNodeParam } from '../../types';
import {
  checkInputParams,
  getParentNodeVariableList,
  transformBranchVariables,
  transformInputParams,
} from '../../utils';

const validateIteratorNodeData = (data: IIteratorNodeData) => {
  const errorMsg: { label: string; error: string }[] = [];
  if (
    data.node_param.iterator_type === 'byArray' &&
    !data.node_param.variable_parameters.filter((item) => !item.value).length
  ) {
    errorMsg.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.Iterator.schema.input',
        dm: '输入',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.Iterator.schema.notNull',
        dm: '不能为空',
      }),
    });
  }

  checkInputParams(data.node_param.variable_parameters, errorMsg, {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Iterator.schema.intermediateVariable',
      dm: '中间变量',
    }),
    disableCheckEmptyList: true,
  });

  checkInputParams(data.output_params, errorMsg, {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Iterator.schema.output',
      dm: '输出',
    }),
    checkValue: true,
  });
  return errorMsg;
};

const getIteratorRefVariables = (data: IIteratorNodeData) => {
  const variableKeyMap: Record<string, boolean> = {};
  if (data.node_param.iterator_type === 'byArray') {
    transformInputParams(data.input_params, variableKeyMap);
  }

  transformInputParams(data.node_param.variable_parameters, variableKeyMap);

  transformBranchVariables(data.node_param.terminations, variableKeyMap, {
    disableShowLeft: true,
  });

  return Object.keys(variableKeyMap);
};

export const IteratorSchema: INodeSchema = {
  type: 'Iterator',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Iterator.schema.loop',
    dm: '循环',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Iterator.schema.repeatTasksBySettingLogic',
    dm: '通过在画布中设定逻辑并配置规则，重复执行一系列任务。',
  }),
  iconType: 'spark-cycleDiagram-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Iterator.schema.logic',
    dm: '逻辑',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {
      iterator_type: 'byArray',
      count_limit: 100,
      variable_parameters: [],
      terminations: [],
    } as IIteratorNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-orange-hover)',
  isGroup: true,
  checkValid: (data) => validateIteratorNodeData(data as IIteratorNodeData),
  getRefVariables: (data) => getIteratorRefVariables(data as IIteratorNodeData),
  getParentNodeVariableList,
};
