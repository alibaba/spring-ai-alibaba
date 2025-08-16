import $i18n from '@/i18n';
import { INodeDataOutputParamItem, IValueTypeOption } from '@/types/work-flow';

export const WorkflowRunningStatus = {
  Running: 'executing',
  Done: 'success',
  Failed: 'fail',
  Paused: 'paused',
  Stop: 'stop',
};

export const ITERATION_PADDING = {
  top: 72,
  right: 60,
  bottom: 60,
  left: 32,
};

export const NEW_NODE_PADDING = {
  x: 100,
};

export const FILE_PROPERTIES: INodeDataOutputParamItem[] = [
  {
    type: 'String',
    key: 'type',
  },
  {
    type: 'Number',
    key: 'size',
  },
  {
    key: 'name',
    type: 'String',
  },
  {
    key: 'mimeType',
    type: 'String',
  },
  {
    key: 'source',
    type: 'String',
  },
  {
    key: 'url',
    type: 'String',
  },
];

export const VALUE_TYPE_OPTIONS: IValueTypeOption[] = [
  {
    label: 'Array',
    value: 'Array',
    children: [
      { label: 'Array<Object>', value: 'Array<Object>' },
      { label: 'Array<File>', value: 'Array<File>' },
      { label: 'Array<String>', value: 'Array<String>' },
      { label: 'Array<Number>', value: 'Array<Number>' },
      { label: 'Array<Boolean>', value: 'Array<Boolean>' },
    ],
  },
  { label: 'Object', value: 'Object' },
  { label: 'File', value: 'File' },
  { label: 'String', value: 'String' },
  { label: 'Number', value: 'Number' },
  { label: 'Boolean', value: 'Boolean' },
];

export const OPERATOR_OPTS_MAP = {
  Number: [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.equal',
        dm: '等于',
      }),
      value: 'equals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEqual',
        dm: '不等于',
      }),
      value: 'notEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.greaterThan',
        dm: '大于',
      }),
      value: 'greater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.greaterThanOrEqual',
        dm: '大于等于',
      }),
      value: 'greaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lessThan',
        dm: '小于',
      }),
      value: 'less',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lessThanOrEqual',
        dm: '小于等于',
      }),
      value: 'lessAndEqual',
    },
  ],

  'Array<Boolean>': [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.contains',
        dm: '包含',
      }),
      value: 'contains',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notContains',
        dm: '不包含',
      }),
      value: 'notContains',
    },
  ],

  Boolean: [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.equal',
        dm: '等于',
      }),
      value: 'equals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEqual',
        dm: '不等于',
      }),
      value: 'notEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.isTrue',
        dm: '为true',
      }),
      value: 'isTrue',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.isFalse',
        dm: '为false',
      }),
      value: 'isFalse',
    },
  ],

  'Array<File>': [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
  ],

  'Array<Object>': [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
  ],

  String: [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.equal',
        dm: '等于',
      }),
      value: 'equals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEqual',
        dm: '不等于',
      }),
      value: 'notEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.contains',
        dm: '包含',
      }),
      value: 'contains',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notContains',
        dm: '不包含',
      }),
      value: 'notContains',
    },
  ],

  File: [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
  ],

  'Array<String>': [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.contains',
        dm: '包含',
      }),
      value: 'contains',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notContains',
        dm: '不包含',
      }),
      value: 'notContains',
    },
  ],

  'Array<Number>': [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.contains',
        dm: '包含',
      }),
      value: 'contains',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notContains',
        dm: '不包含',
      }),
      value: 'notContains',
    },
  ],

  Object: [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.contains',
        dm: '包含',
      }),
      value: 'contains',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notContains',
        dm: '不包含',
      }),
      value: 'notContains',
    },
  ],
};
