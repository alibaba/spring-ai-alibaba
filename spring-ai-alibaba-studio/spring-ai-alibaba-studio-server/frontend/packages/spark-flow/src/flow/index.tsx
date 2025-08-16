import CustomConnectionLine from '@/components/CustomConnectionLine';
import FlowBaseEdge from '@/components/FlowBaseEdge';
import FlowMiniMap from '@/components/FlowMiniMap';
import { useNodesReadOnly } from '@/hooks';
import { useEdgesInteraction } from '@/hooks/useEdgesInteraction';
import { useFlowInteraction } from '@/hooks/useFlowInteraction';
import useFlowKeyPress from '@/hooks/useFlowKeyPress';
import { useHistory } from '@/hooks/useHistory';
import { useNodesInteraction } from '@/hooks/useNodesInteraction';
import $i18n from '@/i18n';
import { IWorkFlowNode } from '@/types/work-flow';
import { getCommonConfig } from '@spark-ai/design';
import {
  Background,
  Edge,
  NodeProps,
  OnBeforeDelete,
  ReactFlow,
  useEdgesState,
  useNodesState,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { message } from 'antd';
import classNames from 'classnames';
import React, { memo, useCallback, useMemo } from 'react';
import useBus from 'use-bus';
import './animate.css';
import { useStore } from './context';
import './index.less';

export interface IFlowProps {
  nodeTypes: Record<string, React.ComponentType<NodeProps<IWorkFlowNode>>>;
  onlyRenderVisibleElements?: boolean;
}

const Flow = memo((props: IFlowProps) => {
  const [nodes, setNodes] = useNodesState<IWorkFlowNode>([]);
  const [edges, setEdges] = useEdgesState<Edge>([]);
  const { onDrop, onDragOver } = useNodesInteraction();
  const { autoFitView } = useFlowInteraction();
  const { onConnect, onNodeDrag, onNodesChange, onNodeClick } =
    useNodesInteraction();
  useFlowKeyPress();
  const { onEdgeEnter, onEdgeLeave, onEdgesChange, onReconnect } =
    useEdgesInteraction();
  const nodeSchemaMap = useStore((state) => state.nodeSchemaMap);
  const { initHistoryStep } = useHistory();
  const interactiveMode = useStore((state) => state.interactiveMode);
  const { nodesReadOnly } = useNodesReadOnly();

  const handleUpdateFlowData = (event: { type: string }) => {
    const data: { nodes: IWorkFlowNode[]; edges: Edge[] } = (event as any).data;
    setNodes(data.nodes);
    setEdges(data.edges);
    initHistoryStep({
      nodes: data.nodes,
      edges: data.edges,
    });
    setTimeout(() => {
      autoFitView();
    }, 200);
  };

  const onBeforeDelete: OnBeforeDelete<IWorkFlowNode, Edge> = useCallback(
    (willDeleteData) => {
      const allowDeleteNodes: IWorkFlowNode[] = [];
      let showTip = false;
      willDeleteData.nodes.forEach((node) => {
        if (
          nodeSchemaMap[node.type].isSystem &&
          !willDeleteData.nodes.some((item) => item.id === node.parentId)
        ) {
          showTip = true;
          return;
        }
        allowDeleteNodes.push(node);
      });

      if (showTip) {
        message.warning(
          $i18n.get({
            id: 'spark-flow.flow.index.systemNodeCannotBeDeleted',
            dm: '系统节点不允许删除',
          }),
        );
      }

      return Promise.resolve({
        nodes: allowDeleteNodes,
        edges: willDeleteData.edges,
      });
    },
    [nodeSchemaMap],
  );

  const memoInteractionProps = useMemo(() => {
    return interactiveMode === 'touch'
      ? {
          panOnDrag: false,
          zoomOnScroll: true,
          panOnScroll: true,
        }
      : {
          panOnDrag: true,
        };
  }, [interactiveMode]);

  useBus('update-flow-data', handleUpdateFlowData);

  return (
    <div
      className={classNames('spark-flow-container', {
        'spark-flow-container-readonly': nodesReadOnly,
      })}
    >
      <ReactFlow
        proOptions={{ hideAttribution: true }}
        connectionLineComponent={CustomConnectionLine}
        nodes={nodes}
        edges={edges}
        colorMode={getCommonConfig().isDarkMode ? 'dark' : 'light'}
        nodeTypes={props.nodeTypes}
        onDragOver={onDragOver}
        onNodeDrag={onNodeDrag}
        onNodeClick={onNodeClick}
        onNodesChange={onNodesChange}
        onBeforeDelete={onBeforeDelete}
        onDrop={onDrop}
        onConnect={onConnect}
        onEdgeMouseEnter={onEdgeEnter}
        onEdgeMouseLeave={onEdgeLeave}
        onEdgesChange={onEdgesChange}
        noWheelClassName="nowheel"
        {...memoInteractionProps}
        nodesDraggable={!nodesReadOnly}
        nodesConnectable={!nodesReadOnly}
        elementsSelectable={!nodesReadOnly}
        edgesFocusable={!nodesReadOnly}
        edgesReconnectable={!nodesReadOnly}
        onReconnect={onReconnect}
        onlyRenderVisibleElements={props.onlyRenderVisibleElements}
        edgeTypes={{
          default: FlowBaseEdge,
        }}
      >
        <FlowMiniMap />
        <Background />
      </ReactFlow>
    </div>
  );
});

export default Flow;
