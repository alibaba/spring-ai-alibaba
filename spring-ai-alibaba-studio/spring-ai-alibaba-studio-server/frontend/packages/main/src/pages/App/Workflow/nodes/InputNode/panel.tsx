import $i18n from '@/i18n';
import {
  CustomOutputsFormWrap,
  useNodeDataUpdate,
  useNodesReadOnly,
} from '@spark-ai/flow';
import { Flex } from 'antd';
import { memo } from 'react';
import InfoIcon from '../../components/InfoIcon';
import { IInputNodeData } from '../../types';

export default memo(function InputPanel({
  id,
  data,
}: {
  id: string;
  data: IInputNodeData;
}) {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const { nodesReadOnly } = useNodesReadOnly();
  return (
    <div className="spark-flow-panel-form-section">
      <Flex vertical gap={12}>
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'main.pages.App.Workflow.nodes.InputNode.panel.input',
            dm: '输入',
          })}

          <InfoIcon
            tip={$i18n.get({
              id: 'main.pages.App.Workflow.nodes.InputNode.panel.injectContentIntoProcess',
              dm: '输入需要注入流程的内容。',
            })}
          />
        </div>
        <div className="spark-flow-panel-form-second-title">
          {$i18n.get({
            id: 'main.pages.App.Workflow.nodes.InputNode.panel.customVariables',
            dm: '自定义变量',
          })}
        </div>
        <CustomOutputsFormWrap
          readyOnly={nodesReadOnly}
          value={data.output_params}
          onChange={(output_params) => {
            handleNodeDataUpdate({
              id,
              data: {
                output_params,
              },
            });
          }}
        />
      </Flex>
    </div>
  );
});
