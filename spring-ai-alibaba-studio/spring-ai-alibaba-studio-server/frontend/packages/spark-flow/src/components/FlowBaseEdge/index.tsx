import { useNodesReadOnly } from '@/hooks';
import { IEdgeData } from '@/types/work-flow';
import { getTypeFromId } from '@/utils';
import { getCommonConfig } from '@spark-ai/design';
import {
  BaseEdge,
  EdgeLabelRenderer,
  EdgeProps,
  getBezierPath,
} from '@xyflow/react';
import React, { memo, useMemo, useState } from 'react';
import CustomIcon from '../CustomIcon';
import { PopoverNodeMenu } from '../NodeMenu';
import './index.less';
import LinearGradientSvg, {
  ILinearGradientSvgProps,
} from './LinearGradientSvg';

export default memo(function FlowBaseEdge({
  id,
  animated,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  source,
  sourceHandleId,
  target,
  targetHandleId,
  data,
  selected,
}: EdgeProps) {
  const edgeData: IEdgeData = data || {};
  const { nodesReadOnly } = useNodesReadOnly();
  const [open, setOpen] = useState(false);

  /* Check coordinate values, set to 0 if NaN */
  const safeSourceX = isNaN(sourceX) ? 0 : sourceX;
  const safeSourceY = isNaN(sourceY) ? 0 : sourceY;
  const safeTargetX = isNaN(targetX) ? 0 : targetX;
  const safeTargetY = isNaN(targetY) ? 0 : targetY;

  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX: safeSourceX + 2,
    sourceY: safeSourceY,
    sourcePosition,
    targetX: safeTargetX - 2,
    targetY: safeTargetY,
    targetPosition,
  });

  const sourceType = useMemo(() => {
    return getTypeFromId(source);
  }, [source]);

  const targetType = useMemo(() => {
    return getTypeFromId(target);
  }, [target]);

  const colorList: ILinearGradientSvgProps['colorList'] = useMemo(() => {
    if (edgeData._source_node_status && edgeData._target_node_status) {
      if (
        edgeData._source_node_status === 'success' &&
        edgeData._target_node_status === 'success'
      ) {
        return [
          { color: '#30A46C', opacity: 0.5, offset: 0 },
          { color: '#30A46C', opacity: 0, offset: 100 },
        ];
      }
      if (
        edgeData._source_node_status === 'success' &&
        edgeData._target_node_status === 'fail'
      ) {
        return [
          { color: '#30A46C', opacity: 1, offset: 0 },
          { color: '#E8D207', opacity: 1, offset: 64 },
          { color: '#F23139', opacity: 1, offset: 100 },
        ];
      }

      if (
        edgeData._source_node_status === 'success' ||
        (edgeData._source_node_status === 'executing' &&
          edgeData._target_node_status === 'executing')
      ) {
        return [
          { color: '#30A46C', opacity: 1, offset: 0 },
          { color: '#87B0F7', opacity: 1, offset: 50 },
          { color: '#615CED', opacity: 1, offset: 100 },
        ];
      }
    }
    if (edgeData._hover) {
      return [
        { color: '#8977FE', opacity: 0.3, offset: 0 },
        { color: '#8977FE', opacity: 0.9, offset: 100 },
      ];
    }
    if (selected)
      return [
        { color: '#624AFF', offset: 0, opacity: 0.5 },
        { color: '#624AFF', offset: 100 },
      ];
    return [
      { color: `var(--${getCommonConfig().antPrefix}-color-mauve)`, offset: 0 },
      {
        color: `var(--${getCommonConfig().antPrefix}-color-mauve)`,
        offset: 100,
      },
    ];
  }, [edgeData, selected]);

  const isAnimated = useMemo(() => {
    if (
      edgeData._source_node_status === 'executing' &&
      edgeData._target_node_status === 'executing'
    )
      return true;
    return (
      edgeData._source_node_status === 'success' &&
      !['fail', 'success']?.includes(edgeData._target_node_status || '')
    );
  }, [edgeData]);

  const markerEndColor = useMemo(() => {
    if (edgeData._source_node_status && edgeData._target_node_status) {
      if (
        edgeData._source_node_status === 'success' &&
        edgeData._target_node_status === 'success'
      ) {
        return {
          stroke: '#30A46C',
        };
      }
      if (
        edgeData._source_node_status === 'success' &&
        edgeData._target_node_status === 'fail'
      ) {
        return {
          stroke: '#F23139',
        };
      }

      if (edgeData._source_node_status === 'success') {
        return {
          stroke: '#615CED',
        };
      }
    }
    if (edgeData._hover) {
      return {
        stroke: '#8977FE',
        opacity: '0.9',
      };
    }

    if (selected)
      return {
        stroke: '#615CED',
      };

    return {
      stroke: `var(--${getCommonConfig().antPrefix}-color-mauve)`,
    };
  }, [
    edgeData._hover,
    selected,
    getCommonConfig,
    edgeData._source_node_status,
    edgeData._target_node_status,
  ]);

  return (
    <>
      {!animated && (
        <defs>
          <marker
            id={`${id}-arrow`}
            markerWidth="20"
            markerHeight="20"
            viewBox="-10 -10 20 20"
            markerUnits="strokeWidth"
            orient="auto-start-reverse"
            refX="0"
            refY="0"
          >
            <polyline
              strokeLinecap="round"
              strokeLinejoin="round"
              fill="none"
              points="-5,-4 0,0 -5,4"
              strokeWidth={1}
              {...markerEndColor}
            />
          </marker>
        </defs>
      )}
      <LinearGradientSvg
        id={id}
        startColor="red"
        stopColor="blue"
        colorList={colorList}
        position={{
          sourceX: safeSourceX,
          sourceY: safeSourceY,
          targetX: safeTargetX,
          targetY: safeTargetY,
        }}
        animated={isAnimated}
      />
      <BaseEdge
        id={id}
        path={edgePath}
        style={{
          stroke: `url(#${id})`,
          strokeWidth: 1.5,
        }}
        markerEnd={`url(#${id}-arrow)`}
      />
      {(edgeData._hover || open) && !nodesReadOnly && (
        <EdgeLabelRenderer>
          <PopoverNodeMenu
            onOpenChange={setOpen}
            source={{
              id: source,
              type: sourceType,
              handleId: sourceHandleId || '',
            }}
            target={{
              id: target,
              type: targetType,
              handleId: targetHandleId || '',
            }}
          >
            <div
              style={{
                transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
                zIndex: edgeData._hover ? 1000 : 0,
              }}
              className="spark-flow-edge-btn nodrag nopan flex-center"
            >
              <CustomIcon type="spark-plus-line" />
            </div>
          </PopoverNodeMenu>
        </EdgeLabelRenderer>
      )}
    </>
  );
});
