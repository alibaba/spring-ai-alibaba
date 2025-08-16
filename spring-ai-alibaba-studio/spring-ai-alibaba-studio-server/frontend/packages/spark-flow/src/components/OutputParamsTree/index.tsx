import { INodeDataOutputParamItem } from '@/types/work-flow';
import { Typography } from 'antd';
import React, { memo } from 'react';
import './index.less';

export const OutputParamsTreeNode = memo(
  (props: { data: INodeDataOutputParamItem }) => {
    return (
      <div className="spark-flow-output-params-tree-node">
        <div className="spark-flow-output-params-tree-node-label">
          <Typography.Text ellipsis={{ tooltip: props.data.key }}>
            {props.data.key}
          </Typography.Text>
          <span className="spark-flow-output-params-tree-node-desc">
            {props.data.type}
          </span>
        </div>
        <div className="spark-flow-output-params-tree-node-desc">
          {props.data.desc}
        </div>
        {!!props.data.properties?.length && (
          <div className="spark-flow-output-params-tree-node-properties">
            {props.data.properties.map((property, index) => (
              <OutputParamsTreeNode key={index} data={property} />
            ))}
          </div>
        )}
      </div>
    );
  },
);

export default memo(function OutputParamsTree(props: {
  data: INodeDataOutputParamItem[];
}) {
  return (
    <div>
      {props.data.map((item, index) => (
        <OutputParamsTreeNode key={index} data={item} />
      ))}
    </div>
  );
});
