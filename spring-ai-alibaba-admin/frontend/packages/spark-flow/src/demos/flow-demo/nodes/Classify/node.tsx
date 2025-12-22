import SourceHandle from '@/components/CustomHandle/SourceHandle';
import $i18n from '@/i18n';
import { IWorkFlowNode } from '@/types/work-flow';
import { BaseNode } from '@spark-ai/flow';
import { NodeProps } from '@xyflow/react';
import React, { memo } from 'react';
import { IClassifierNodeParam } from '../../types/flow';

export default memo(function Classifier(props: NodeProps<IWorkFlowNode>) {
  const nodeParam = props.data.node_param as IClassifierNodeParam;
  return (
    <BaseNode disableShowSourceHandle {...props}>
      {nodeParam.conditions.map((item) =>
        item.id === 'default' ? null : (
          <div
            key={item.id}
            className="spark-flow-judge-branch flex-justify-between"
          >
            <span>
              {item.id === 'default'
                ? $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.nodes.Classify.node.default',
                    dm: '默认',
                  })
                : item.subject ||
                  $i18n.get({
                    id: 'spark-flow.demos.spark-flow-1.nodes.Classify.node.noIntentionConfigured',
                    dm: '暂未配置意图',
                  })}
            </span>
            <SourceHandle
              className="spark-flow-judge-branch-handle"
              nodeType={props.type}
              nodeId={props.id}
              handleId={`${props.id}_${item.id}`}
            />
          </div>
        ),
      )}
      <div className="spark-flow-judge-branch flex-justify-between">
        <span>
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Classify.node.default',
            dm: '默认',
          })}
        </span>
        <SourceHandle
          className="spark-flow-judge-branch-handle"
          nodeType={props.type}
          nodeId={props.id}
          handleId={`${props.id}_default`}
        />
      </div>
    </BaseNode>
  );
});
