import $i18n from '@/i18n';
import { type INodeSchema } from '@spark-ai/flow';
import { RETRY_CONFIG_DEFAULT, TRY_CATCH_CONFIG_DEFAULT } from '../../constant';
import { IApiNodeParam } from '../../types/flow';

export const APISchema: INodeSchema = {
  type: 'API',
  title: 'API',
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.apiNode',
    dm: 'API节点',
  }),
  iconType: 'spark-api-line',
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.tool',
    dm: '工具',
  }),
  defaultParams: {
    input_params: [],
    output_params: [
      {
        key: 'output',
        type: 'String',
        desc: $i18n.get({
          id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.outputResult',
          dm: '输出结果',
        }),
      },
    ],
    node_param: {
      output_type: 'primitive',
      authorization: {
        auth_type: 'BearerAuth',
        auth_config: {
          token: '',
        },
      },
      headers: [
        {
          key: 'header1',
          value_from: 'refer',
          value: void 0,
          type: 'String',
        },
      ],

      method: 'get',
      params: [],
      body: {
        type: 'raw',
        data: '',
      },
      timeout: {
        read: 4,
        connect: 4,
        write: 4,
      },
      retry_config: RETRY_CONFIG_DEFAULT,
      try_catch_config: TRY_CATCH_CONFIG_DEFAULT,
      url: '',
    } as IApiNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--spark-ant-color-blue-hover)',
};
