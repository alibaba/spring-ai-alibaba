import React, { memo } from 'react';
import { IJudgeNodeData } from '../../types/flow';

export default memo(function JudgePanel(props: {
  id: string;
  data: IJudgeNodeData;
}) {
  return (
    <>
      <div className="spark-flow-panel-form-section">{props.id}</div>
    </>
  );
});
