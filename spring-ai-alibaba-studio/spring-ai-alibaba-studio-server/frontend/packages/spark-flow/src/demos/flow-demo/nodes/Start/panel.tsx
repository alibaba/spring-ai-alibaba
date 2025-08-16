import $i18n from '@/i18n';
import { IconFont } from '@spark-ai/design';
import { CustomOutputsFormWrap, useNodeDataUpdate } from '@spark-ai/flow';
import React, { memo } from 'react';
import { IStartNodeData } from '../../types/flow';

export default memo((props: { id: string; data: IStartNodeData }) => {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  return (
    <>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Start.panel.input',
            dm: '输入',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <div className="spark-flow-panel-form-second-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Start.panel.customVariables',
            dm: '自定义变量',
          })}
        </div>
        <CustomOutputsFormWrap
          value={props.data.output_params}
          onChange={(output_params) => {
            handleNodeDataUpdate({
              id: props.id,
              data: {
                output_params,
              },
            });
          }}
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-second-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Start.panel.variable',
            dm: '内置变量',
          })}
        </div>
        <CustomOutputsFormWrap
          readyOnly
          value={[
            {
              key: 'query',
              type: 'String',
              desc: $i18n.get({
                id: 'spark-flow.demos.spark-flow-1.nodes.Start.panel.userQuery',
                dm: '用户的query',
              }),
            },
          ]}
        />
      </div>
    </>
  );
});
