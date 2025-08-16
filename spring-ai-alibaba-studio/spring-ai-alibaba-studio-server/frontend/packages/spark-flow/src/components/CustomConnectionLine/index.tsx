import { getCommonConfig } from '@spark-ai/design';
import { ConnectionLineComponentProps } from '@xyflow/react';
import React, { memo } from 'react';

export default memo(function ({
  fromX,
  fromY,
  toX,
  toY,
}: ConnectionLineComponentProps) {
  const controlPoint1X = fromX + (toX - fromX) * 0.5;
  const controlPoint1Y = fromY;
  const controlPoint2X = fromX + (toX - fromX) * 0.5;
  const controlPoint2Y = toY;

  return (
    <g>
      <path
        fill="none"
        stroke={`var(--${getCommonConfig().antPrefix}-color-primary)`}
        strokeWidth={1.5}
        className="animated"
        d={`M${fromX},${fromY} C ${controlPoint1X} ${controlPoint1Y} ${controlPoint2X} ${controlPoint2Y} ${toX},${toY}`}
      />
      <circle
        cx={toX}
        cy={toY}
        fill={`var(--${getCommonConfig().antPrefix}-color-bg-base)`}
        r={3}
        stroke={`var(--${getCommonConfig().antPrefix}-color-primary)`}
        strokeWidth={1.5}
      />
    </g>
  );
});
