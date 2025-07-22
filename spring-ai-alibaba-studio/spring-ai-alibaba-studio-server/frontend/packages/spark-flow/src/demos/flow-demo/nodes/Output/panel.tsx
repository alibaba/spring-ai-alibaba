import $i18n from '@/i18n';
import { IconFont } from '@spark-ai/design';
import { useNodeDataUpdate } from '@spark-ai/flow';
import { Switch } from 'antd';
import React, { memo, useCallback } from 'react';
import { IOutputNodeData, IOutputNodeParam } from '../../types/flow';

export default memo((props: { id: string; data: IOutputNodeData }) => {
  const { handleNodeDataUpdate } = useNodeDataUpdate();

  const changeNodeParam = useCallback(
    (payload: Partial<IOutputNodeParam>) => {
      handleNodeDataUpdate({
        id: props.id,
        data: {
          node_param: {
            ...props.data.node_param,
            ...payload,
          },
        },
      });
    },
    [props.data.node_param],
  );

  return (
    <div className="spark-flow-panel-form-section">
      <div className="spark-flow-panel-form-title">
        {$i18n.get({
          id: 'spark-flow.demos.spark-flow-1.nodes.Output.panel.outputContent',
          dm: '输出内容',
        })}

        <IconFont type="spark-info-line" />
      </div>
      <div className="flex-justify-between w-full">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Output.panel.streamOutput',
            dm: '流式输出',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <Switch
          checked={props.data.node_param.stream_switch}
          onChange={(val) => {
            changeNodeParam({
              stream_switch: val,
            });
          }}
        />
      </div>
    </div>
  );
});
