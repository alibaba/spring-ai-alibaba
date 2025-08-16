import React, { memo } from 'react';
import './index.less';

export type ILinearGradientSvgProps = {
  id: string;
  startColor: string;
  stopColor: string;
  position: {
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
  };
  colorList: Array<{ color: string; opacity?: number; offset: number }>;
  animated?: boolean;
};

const LinearGradientSvg = ({
  id,
  position,
  colorList,
  animated = false,
}: ILinearGradientSvgProps) => {
  const { sourceX, sourceY, targetX, targetY } = position;

  return (
    <defs>
      <linearGradient
        className={animated ? 'spark-flow-animate-gradient-edge' : ''}
        id={id}
        gradientUnits="userSpaceOnUse"
        x1={sourceX}
        y1={sourceY}
        x2={targetX}
        y2={targetY}
      >
        {colorList.map((item, index) => (
          <stop
            key={index}
            offset={`${item.offset}%`}
            style={{
              stopColor: item.color,
              stopOpacity: item.opacity || 1,
            }}
          />
        ))}
      </linearGradient>
    </defs>
  );
};

export default memo(LinearGradientSvg);
