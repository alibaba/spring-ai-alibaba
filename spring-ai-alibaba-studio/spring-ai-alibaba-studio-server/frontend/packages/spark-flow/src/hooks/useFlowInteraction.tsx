import { useStore } from '@/flow/context';
import { getIteratorNodeSize, layoutFlow } from '@/utils';
import {
  Edge,
  Node,
  OnSelectionChangeFunc,
  useReactFlow,
  useStoreApi,
} from '@xyflow/react';
import { useCallback } from 'react';
import { useFlowDebugInteraction } from './useFlowDebugInteraction';
import { useFlowSave } from './useFlowSave';

export const useFlowInteraction = () => {
  const reactFlow = useReactFlow();
  const store = useStoreApi();
  const { clearTaskStore } = useFlowDebugInteraction();
  const setReadyOnly = useStore((state) => state.setReadyOnly);
  const setSelectedNode = useStore((state) => state.setSelectedNode);
  const nodeSchemaMap = useStore((state) => state.nodeSchemaMap);
  const { handleSaveFlowDraft } = useFlowSave();

  const onLayout = useCallback(async () => {
    const { nodes, edges, setNodes } = store.getState();
    const subFlowNodesMap: Record<string, Node[]> = {};
    const globalNodes: Node[] = [];
    nodes.forEach((node) => {
      if (node.parentId) {
        if (!subFlowNodesMap[node.parentId]) {
          subFlowNodesMap[node.parentId] = [];
        }
        subFlowNodesMap[node.parentId].push(node);
      } else {
        globalNodes.push(node);
      }
    });
    const globalEdges: Edge[] = edges.filter((edge) =>
      globalNodes.some(
        (node) => node.id === edge.source || node.id === edge.target,
      ),
    );

    const subFlow = Object.keys(subFlowNodesMap).map((parentNodeId) => {
      const subFlowNodes = subFlowNodesMap[parentNodeId];
      return {
        nodes: subFlowNodes,
        edges: edges.filter((edge) =>
          subFlowNodes.some(
            (node) => node.id === edge.source || node.id === edge.target,
          ),
        ),
      };
    });

    Promise.all([
      layoutFlow({ nodes: globalNodes, edges: globalEdges }),
      ...subFlow.map((subFlow) => layoutFlow({ ...subFlow, isSubFlow: true })),
    ]).then((layoutedFlows) => {
      let newNodes: Node[] = [];
      layoutedFlows.forEach((flow) => {
        newNodes = [...newNodes, ...flow.layoutedNodes];
      });
      newNodes = newNodes.map((node) => {
        if (nodeSchemaMap[node.type as string].isGroup) {
          const { width, height } = getIteratorNodeSize(
            newNodes.filter((n) => n.parentId === node.id),
          );
          return {
            ...node,
            width,
            height,
          };
        }
        return node;
      });
      setNodes(newNodes);
      setTimeout(autoFitView, 200);
      handleSaveFlowDraft();
    });
  }, [store, nodeSchemaMap, handleSaveFlowDraft]);

  const focusElement = useCallback(
    ({ nodeId }: { nodeId: string }) => {
      const rightPanelWidth =
        document.querySelector('.spark-flow-panel-group')?.clientWidth || 0;
      const { nodes, setNodes } = store.getState();
      const targetNode = nodes.find((item) => item.id === nodeId);
      if (!targetNode) return;
      const parentNode = targetNode.parentId
        ? nodes.find((item) => item.id === targetNode.parentId) || {
            position: { x: 0, y: 0 },
          }
        : { position: { x: 0, y: 0 } };

      reactFlow.setCenter(
        targetNode.position.x +
          parentNode.position.x +
          rightPanelWidth / 2 +
          (targetNode.width || 0) / 2,
        targetNode.position.y +
          parentNode.position.y +
          (targetNode.height || 0) / 2,
        {
          zoom:
            targetNode.width! > 1000 || targetNode.height! > 1000 ? 0.45 : 0.75,
          duration: 400,
        },
      );

      setNodes(
        nodes.map((item) => ({
          ...item,
          selected: item.id === nodeId,
        })),
      );
    },
    [store],
  );

  const autoFitView = useCallback(() => {
    reactFlow.fitView({
      duration: 400,
    });
  }, [reactFlow, store]);

  const onSelectionChange: OnSelectionChangeFunc = useCallback(
    ({ nodes: selectedNodes }) => {
      const { nodes, setNodes } = store.getState();
      const newNodes = nodes.map((node) => {
        return {
          ...node,
          selected: selectedNodes.some(
            (selectedNode) => selectedNode.id === node.id,
          ),
        };
      });
      setNodes(newNodes);
    },
    [store],
  );

  const onFlowClearState = useCallback(
    (options: { readyOnly?: boolean }) => {
      clearTaskStore();
      setReadyOnly(options.readyOnly || false);
      setSelectedNode(null);
    },
    [clearTaskStore, setReadyOnly, setSelectedNode],
  );

  return {
    onSelectionChange,
    onFlowClearState,
    autoFitView,
    focusElement,
    onLayout,
  };
};
