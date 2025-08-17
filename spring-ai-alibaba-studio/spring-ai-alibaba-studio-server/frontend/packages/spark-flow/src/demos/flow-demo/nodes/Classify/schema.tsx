import $i18n from '@/i18n';
import { INodeSchema } from '@/types/work-flow';
import { SHORT_MEMORY_CONFIG_DEFAULT } from '../../constant';
import { IClassifierNodeParam } from '../../types/flow';

export const ClassifySchema: INodeSchema = {
  type: 'Classifier',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.intentionClassification',
    dm: '意图分类',
  }),
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.intentionClassificationNode',
    dm: '意图分类节点',
  }),
  iconType: 'spark-effciency-line',
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.logic',
    dm: '逻辑',
  }),
  defaultParams: {
    input_params: [
      {
        key: 'content',
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

      short_memory: SHORT_MEMORY_CONFIG_DEFAULT,
      instruction: '',
      mode_switch: 'efficient',
    } as IClassifierNodeParam,
    output_params: [
      {
        key: 'output',
        type: 'Object',
        desc: $i18n.get({
          id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.outputResult',
          dm: '输出结果',
        }),
        properties: [
          {
            key: 'thought',
            type: 'String',
            desc: $i18n.get({
              id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.thinkingProcess',
              dm: '思考过程',
            }),
          },
          {
            key: 'subject',
            type: 'String',
            desc: $i18n.get({
              id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.hitTheme',
              dm: '命中主题',
            }),
          },
        ],
      },
    ],
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--spark-ant-color-orange-hover)',
};
