import { useStore } from '@/flow/context';
import { MiniMap } from '@xyflow/react';
import React, { memo } from 'react';
import './index.less';

export default memo(function FlowMiniMap() {
  const showMiniMap = useStore((state) => state.showMiniMap);

  if (!showMiniMap) return null;
  return (
    <MiniMap
      ariaLabel={null}
      pannable
      draggable
      zoomable
      style={{ width: 220, height: 120 }}
      className="spark-flow-mini-map"
    />
  );
});
