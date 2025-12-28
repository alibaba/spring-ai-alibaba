import { ITERATION_PADDING, NEW_NODE_PADDING } from '@/constant';
import { useStore } from '@/flow/context';
import $i18n from '@/i18n';
import {
  IPointItem,
  IWorkFlowNode,
  IWorkFlowNodeData,
} from '@/types/work-flow';
import {
  copyNodeConfig,
  copySubFlowNodeConfig,
  generateEdge,
  generateUniqueName,
  getIteratorNodeSize,
} from '@/utils';
import uniqueId from '@/utils/uniqueId';
import type {
  Connection,
  Edge,
  Node,
  NodeMouseHandler,
  OnNodeDrag,
  OnNodesChange,
} from '@xyflow/react';
import {
  applyNodeChanges,
  getOutgoers,
  useReactFlow,
  useStoreApi,
} from '@xyflow/react';
import { message } from 'antd';
import { useCallback } from 'react';
import { useFlowInteraction } from './useFlowInteraction';
import { useFlowSave } from './useFlowSave';

export const useNodesInteraction = () => {
  const { handleSaveFlowDraft } = useFlowSave();
  const store = useStoreApi();
  const setSelectedNode = useStore((state) => state.setSelectedNode);
  const selectedNode = useStore((state) => state.selectedNode);
  const nodeSchemaMap = useStore((state) => state.nodeSchemaMap);
  const onAddCustomNode = useStore((state) => state.onAddCustomNode);
  const { screenToFlowPosition, getNode } = useReactFlow();
  const { focusElement } = useFlowInteraction();
  const setIsDragging = useStore((state) => state.setIsDragging);

  const onConnect = useCallback(
    (connection: Connection) => {
      const { edges, nodes, setEdges } = store.getState();
      const targetNode = nodes.find((node) => node.id === connection.target);
      const sourceNode = nodes.find((node) => node.id === connection.source);
      if (
        (sourceNode?.parentId || targetNode?.parentId) &&
        sourceNode?.parentId !== targetNode?.parentId
      ) {
        message.warning(
          $i18n.get({
            id: 'spark-flow.hooks.useNodesInteraction.sameSubCanvasOnly',
            dm: '只能同一子画布中的节点相连',
          }),
        );
        return;
      }
      if (targetNode?.id === connection.source) {
        message.warning(
          $i18n.get({
            id: 'spark-flow.hooks.useNodesInteraction.cannotConnectToSelf',
            dm: '不能与自身相连',
          }),
        );
        return;
      }
      const hasCycle = (node: Node, visited = new Set()) => {
        if (visited.has(node.id)) return false;

        visited.add(node.id);

        for (const outgoer of getOutgoers(node, nodes, edges)) {
          if (outgoer.id === connection.source) return true;
          if (hasCycle(outgoer, visited)) return true;
        }
      };

      if (targetNode && hasCycle(targetNode)) {
        message.warning(
          $i18n.get({
            id: 'spark-flow.hooks.useNodesInteraction.cannotFormLoop',
            dm: '不能形成环路',
          }),
        );
        return;
      }
      const newEdge = generateEdge(connection, !!sourceNode?.parentId);
      setEdges([...edges, newEdge]);
      handleSaveFlowDraft();
    },
    [store, handleSaveFlowDraft],
  );

  const generateNewNode = useCallback(
    (
      {
        type,
        parentId,
        size = 'normal',
        disableScreenToFlowPosition = false,
        disableUniqueName = false,
      }: {
        type: string;
        parentId?: string;
        size?: 'normal' | 'small';
        disableScreenToFlowPosition?: boolean;
        disableUniqueName?: boolean;
      },
      position: { x: number; y: number },
    ): IWorkFlowNode => {
      let newPosition = { ...position };
      const { nodes } = store.getState();
      if (!disableScreenToFlowPosition) {
        newPosition = screenToFlowPosition(newPosition);

        if (parentId) {
          const parentNode = nodes.find((item) => item.id === parentId);
          if (parentNode) {
            newPosition.x = newPosition.x - parentNode.position.x;
            newPosition.y = newPosition.y - parentNode.position.y;
          }
        }
      }

      const nodeInfo = nodeSchemaMap[type];
      const boundsParams =
        type === 'Iterator' || type === 'Parallel'
          ? {
              width: 1080,
              height: 360,
            }
          : {
              width: size === 'normal' ? 320 : 200,
            };
      return {
        id: `${type}_${uniqueId(4)}`,
        parentId,
        type,
        position: newPosition,
        zIndex: parentId ? 1002 : void 0,
        ...boundsParams,
        data: {
          label: disableUniqueName
            ? nodeInfo.title
            : generateUniqueName(
                nodeInfo.title,
                (nodes as IWorkFlowNode[]).map((item) => item.data.label),
              ),
          ...((nodeInfo.defaultParams || {}) as Pick<
            IWorkFlowNodeData,
            'input_params' | 'output_params' | 'node_param'
          >),
        },
      };
    },
    [store, nodeSchemaMap],
  );

  const handleNodeClick = useCallback(
    (node: IWorkFlowNode) => {
      if (!nodeSchemaMap[node.type].notAllowConfig) {
        setSelectedNode(node);
        setTimeout(() => {
          focusElement({ nodeId: node.id });
        }, 200);
      }
    },
    [nodeSchemaMap, focusElement, setSelectedNode],
  );

  const handleNodeClickByNodeId = useCallback(
    (nodeId: string) => {
      const node = store.getState().nodes.find((item) => item.id === nodeId);
      if (node) {
        handleNodeClick(node as IWorkFlowNode);
      }
    },
    [handleNodeClick, store],
  );

  const addNewNodeCallback = useCallback(
    ({
      newNodes,
      parentId,
    }: {
      newNodes: IWorkFlowNode[];
      parentId?: string;
    }) => {
      const { nodes, setNodes } = store.getState();
      if (!parentId) {
        setNodes([
          ...nodes.map((item) => ({ ...item, selected: false })),
          ...newNodes.map((item, index) => ({
            ...item,
            selected: index === 0,
          })),
        ]);
      } else {
        const subFlowNodes = nodes.filter((item) => item.parentId === parentId);
        const { width, height } = getIteratorNodeSize([
          ...subFlowNodes,
          ...newNodes,
        ]);
        setNodes([
          ...nodes.map((item) => {
            if (item.id === parentId) {
              return {
                ...item,
                width: item.width! < width ? width : item.width,
                height: item.height! < height ? height : item.height,
                selected: false,
              };
            }
            return {
              ...item,
              selected: false,
            };
          }),
          ...newNodes.map((item, index) => ({
            ...item,
            selected: index === 0,
          })),
        ]);
      }
      setSelectedNode(newNodes[0]);
      handleNodeClick(newNodes[0]);
    },
    [store, handleNodeClick],
  );

  const onAddNewNode = useCallback(
    async (
      { type, parentId }: { type: string; parentId?: string },
      position: { x: number; y: number },
    ) => {
      const nodeInfo = nodeSchemaMap[type];
      const newNodes: IWorkFlowNode[] = [];
      let newNode = generateNewNode({ type, parentId }, position);
      if (nodeInfo.isGroup) {
        if (parentId) {
          message.warning(
            $i18n.get({
              id: 'spark-flow.hooks.useNodesInteraction.subCanvasCannotNest',
              dm: '子画布不能互相嵌套',
            }),
          );
          return;
        }
        newNodes.push(
          generateNewNode(
            {
              type: `${type}Start`,
              size: 'small',
              disableUniqueName: true,
              parentId: newNode.id,
              disableScreenToFlowPosition: true,
            },
            {
              x: 50,
              y: 152,
            },
          ),
        );
        newNodes.push(
          generateNewNode(
            {
              type: `${type}End`,
              size: 'small',
              disableUniqueName: true,
              parentId: newNode.id,
              disableScreenToFlowPosition: true,
            },
            {
              x: 700,
              y: 152,
            },
          ),
        );
      }
      try {
        if (nodeInfo.customAdd) {
          newNode = (await onAddCustomNode(newNode)) as IWorkFlowNode;
        }
      } catch {}
      if (!newNode) return;
      addNewNodeCallback({
        newNodes: [newNode, ...newNodes],
        parentId,
      });
      handleSaveFlowDraft();
    },
    [
      store,
      handleSaveFlowDraft,
      nodeSchemaMap,
      onAddCustomNode,
      addNewNodeCallback,
    ],
  );

  const onDrop = useCallback(
    (event: React.DragEvent<HTMLDivElement>, parentId?: string) => {
      setIsDragging(false);
      event.preventDefault();
      const type = event.dataTransfer.getData('application/reactflow');

      if (typeof type === 'undefined' || !type) {
        return;
      }

      onAddNewNode({ type, parentId }, { x: event.clientX, y: event.clientY });
    },
    [onAddNewNode],
  );

  const onDragOver = useCallback((event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const handleSelectNode = useCallback(
    (nodeId: string) => {
      const { nodes, setNodes } = store.getState();
      const newNodes = nodes.map((item) => {
        if (item.id === nodeId) return { ...item, selected: true };
        return item;
      });
      setNodes(newNodes);
    },
    [store],
  );

  const onAddNewNodeWithSource = useCallback(
    async (
      { type, parentId }: { type: string; parentId?: string },
      source: IPointItem,
      target?: IPointItem,
    ) => {
      const { nodes, setNodes } = store.getState();
      const sourceNode = nodes.find((item) => item.id === source.id);
      const nodeInfo = nodeSchemaMap[type];
      const newNodes: IWorkFlowNode[] = [];

      if (!target) {
        let newNode = generateNewNode(
          { type, parentId, disableScreenToFlowPosition: true },
          {
            x:
              sourceNode?.position.x! + sourceNode?.width! + NEW_NODE_PADDING.x,
            y: sourceNode?.position.y || 0,
          },
        );
        if (nodeInfo.isGroup) {
          /* Iteration start and batch body nodes need to add loop start/end nodes by default; */
          newNodes.push(
            generateNewNode(
              {
                type: `${type}Start`,
                size: 'small',
                parentId: newNode.id,
                disableScreenToFlowPosition: true,
                disableUniqueName: true,
              },
              {
                x: 50,
                y: 152,
              },
            ),
          );
          newNodes.push(
            generateNewNode(
              {
                type: `${type}End`,
                size: 'small',
                parentId: newNode.id,
                disableScreenToFlowPosition: true,
                disableUniqueName: true,
              },
              {
                x: 700,
                y: 152,
              },
            ),
          );
        }
        try {
          if (nodeInfo.customAdd) {
            newNode = (await onAddCustomNode(newNode)) as IWorkFlowNode;
          }
        } catch {}
        if (!newNode) return;
        newNode.selected = true;

        addNewNodeCallback({
          newNodes: [newNode, ...newNodes],
          parentId,
        });
        onConnect({
          source: source.id,
          sourceHandle: source.handleId,
          target: newNode.id,
          targetHandle: newNode.id,
        });
      } else {
        const targetNode = nodes.find((item) => item.id === target.id);
        if (!targetNode) return;
        let x =
          (targetNode.position.x - sourceNode?.position.x!) / 2 +
          sourceNode?.position.x!;
        const y =
          (targetNode.position.y - sourceNode?.position.y!) / 2 +
          sourceNode?.position.y!;
        if (
          x <=
          sourceNode?.position.x! + sourceNode?.width! + NEW_NODE_PADDING.x
        ) {
          x = sourceNode?.position.x! + sourceNode?.width! + NEW_NODE_PADDING.x;
        }
        let newNode = generateNewNode(
          { type, parentId, disableScreenToFlowPosition: true },
          { x, y },
        );
        try {
          if (nodeInfo.customAdd) {
            newNode = (await onAddCustomNode(newNode)) as IWorkFlowNode;
          }
        } catch {}
        if (nodeInfo.isGroup) {
          /* Iteration start and batch body nodes need to add loop start/end nodes by default; */
          newNodes.push(
            generateNewNode(
              {
                type: `${type}Start`,
                size: 'small',
                parentId: newNode.id,
                disableScreenToFlowPosition: true,
                disableUniqueName: true,
              },
              {
                x: 50,
                y: 152,
              },
            ),
          );
          newNodes.push(
            generateNewNode(
              {
                type: `${type}End`,
                size: 'small',
                parentId: newNode.id,
                disableScreenToFlowPosition: true,
                disableUniqueName: true,
              },
              {
                x: 700,
                y: 152,
              },
            ),
          );
        }
        const { edges, setEdges } = store.getState();
        /* Remove the edge between source and target */
        const newEdges = edges.filter(
          (item) =>
            item.sourceHandle !== source.handleId ||
            item.targetHandle !== target.handleId,
        );

        let tempNodes: IWorkFlowNode[] = (nodes as IWorkFlowNode[]).map(
          (item) => {
            if (item.id === target.id) {
              let newX = item.position.x;
              /* Check if there are overlapping nodes */
              if (
                newNode.position.x + newNode.width! + NEW_NODE_PADDING.x >
                item.position.x
              ) {
                newX = newNode.position.x + newNode.width! + NEW_NODE_PADDING.x;
              }
              return {
                ...item,
                selected: false,
                position: {
                  x: newX,
                  y: item.position.y,
                },
              };
            }
            return { ...item, selected: false };
          },
        );
        tempNodes = tempNodes.concat(newNode, ...newNodes);

        if (parentId) {
          tempNodes = tempNodes.map((item) => {
            if (item.id === parentId) {
              const newBounds = getIteratorNodeSize(
                tempNodes.filter((vItem) => vItem.parentId === parentId),
              );
              return {
                ...item,
                width:
                  newBounds.width > item.width! ? newBounds.width : item.width,
                height:
                  newBounds.height > item.height!
                    ? newBounds.height
                    : item.height,
              };
            }
            return item;
          });
        }

        let sourceHandle = newNode.id;
        if (newNode.type === 'Classifier') {
          sourceHandle = `${newNode.id}_${newNode.data.node_param.conditions[0].id}`;
        }
        if (newNode.type === 'Judge') {
          sourceHandle = `${newNode.id}_${newNode.data.node_param.branches[0].id}`;
        }

        newEdges.push(
          generateEdge(
            {
              source: source.id,
              sourceHandle: source.handleId,
              target: newNode.id,
              targetHandle: newNode.id,
            },
            !!parentId,
          ),
        );

        if (sourceHandle) {
          newEdges.push(
            generateEdge(
              {
                source: newNode.id,
                sourceHandle: sourceHandle,
                target: target.id,
                targetHandle: target.handleId,
              },
              !!parentId,
            ),
          );
        }
        setNodes(tempNodes);
        setEdges([...newEdges]);
        handleNodeClick(newNode);
      }
      handleSaveFlowDraft();
    },
    [store, getNode, handleSaveFlowDraft],
  );

  const onNodeDrag = useCallback<OnNodeDrag>(
    (e, _: Node, dragNodes: Node[]) => {
      e.stopPropagation();
      const { nodes, setNodes } = store.getState();
      const needFreshParentNodeIdsMap: Record<string, boolean> = {};
      let newNodes = nodes.map((item) => {
        const targetNode = dragNodes.find((vItem) => vItem.id === item.id);
        if (!targetNode) return item;
        if (
          targetNode.parentId &&
          !needFreshParentNodeIdsMap[targetNode.parentId]
        ) {
          needFreshParentNodeIdsMap[targetNode.parentId] = true;
        }
        return {
          ...item,
          position: item.parentId
            ? {
                x:
                  targetNode.position.x <= ITERATION_PADDING.left
                    ? ITERATION_PADDING.left
                    : targetNode.position.x,
                y:
                  targetNode.position.y <= ITERATION_PADDING.top
                    ? ITERATION_PADDING.top
                    : targetNode.position.y,
              }
            : targetNode.position,
        };
      });
      newNodes = newNodes.map((item) => {
        if (needFreshParentNodeIdsMap[item.id]) {
          const subFlowNodes = newNodes.filter((vItem) => {
            return vItem.parentId === item.id;
          });
          const { width, height } = getIteratorNodeSize(subFlowNodes);
          return {
            ...item,
            width,
            height,
          };
        }
        return item;
      });
      setNodes(newNodes);
      handleSaveFlowDraft();
    },
    [store, handleSaveFlowDraft],
  );

  const onNodeClick = useCallback<NodeMouseHandler<IWorkFlowNode>>(
    (_, node) => {
      handleNodeClick(node);
    },
    [handleNodeClick],
  );

  const onNodeResize = useCallback(
    (
      resizeBound: { width: number; height: number; x: number; y: number },
      nodeId: string,
    ) => {
      const { nodes, setNodes } = store.getState();
      const minBoundSize = getIteratorNodeSize(
        nodes.filter((item) => item.parentId === nodeId),
      );

      let newWidth =
        resizeBound.width < minBoundSize.width
          ? minBoundSize.width
          : resizeBound.width;
      let newHeight =
        resizeBound.height < minBoundSize.height
          ? minBoundSize.height
          : resizeBound.height;
      const newNodes = nodes.map((item) => {
        if (item.id === nodeId)
          return {
            ...item,
            width: newWidth,
            height: newHeight,
            position: { x: resizeBound.x, y: resizeBound.y },
          };
        return item;
      });
      setNodes(newNodes);
    },
    [store],
  );

  const onNodesChange = useCallback<OnNodesChange>(
    (changes) => {
      if (!['remove', 'select', 'dimensions'].includes(changes[0].type)) return;
      const { nodes, setNodes } = store.getState();
      if (changes[0].type === 'dimensions') {
        /* Override the dimensions update method; */
        return setNodes(
          nodes.map((item) => {
            /* @ts-ignore */
            const targetChange = changes.find((vItem) => vItem.id === item.id);
            /* @ts-ignore */
            if (!targetChange || !targetChange?.dimensions) return item;
            return {
              ...item,
              measured: {
                /* @ts-ignore */
                width: targetChange.dimensions.width,
                /* @ts-ignore */
                height: targetChange.dimensions.height,
              },
            };
          }),
        );
      }
      const newNodes = applyNodeChanges(changes, nodes);
      setNodes(newNodes);
      if (changes[0].type === 'remove') handleSaveFlowDraft();
    },
    [store, handleSaveFlowDraft, applyNodeChanges],
  );

  const onNodeDelete = useCallback(
    (nodeId: string) => {
      const { nodes, setNodes, setEdges, edges } = store.getState();
      const newNodes = nodes.filter(
        (item) => item.id !== nodeId && item.parentId !== nodeId,
      );
      /* If the selected node is deleted, deselect it */
      if (selectedNode?.id === nodeId) setSelectedNode(null);
      setNodes(newNodes);
      /* Remove disconnected edges */
      setEdges(
        edges.filter(
          (item) => item.source !== nodeId && item.target !== nodeId,
        ),
      );
      handleSaveFlowDraft();
    },
    [store, selectedNode, handleSaveFlowDraft],
  );

  const onNodeCopy = useCallback(
    (nodeId: string) => {
      const nodes = store.getState().nodes as IWorkFlowNode[];
      const { setNodes, edges, setEdges } = store.getState();
      const node = nodes.find((item) => item.id === nodeId) as IWorkFlowNode;
      if (node) {
        const newNodes: IWorkFlowNode[] = [];
        const newEdges: Edge[] = [];
        /* Generate a new node */
        const newNode = copyNodeConfig(node, nodes);
        newNodes.push(newNode);

        if (nodeSchemaMap[node.type].isGroup) {
          /* When copying group nodes, sub-canvas information needs to be copied in bulk */
          const subNodes = nodes.filter((item) => item.parentId === nodeId);
          const newSubNodesMap: Record<string, IWorkFlowNode> = {};
          const subEdges = edges.filter((item) =>
            subNodes.some((vItem) =>
              [item.source, item.target].includes(vItem.id),
            ),
          );
          subNodes.forEach((item) => {
            const newSubNode = copySubFlowNodeConfig(
              {
                ...item,
                parentId: newNode.id,
              },
              [...nodes, ...subNodes],
            );
            newSubNodesMap[item.id] = newSubNode;
            newNodes.push(newSubNode);
          });
          subEdges.forEach((item) => {
            const { source, target, sourceHandle } = item;
            const newSourceNode = newSubNodesMap[source];
            const newTargetNode = newSubNodesMap[target];
            const newEdge = generateEdge(
              {
                source: newSourceNode.id,
                sourceHandle: (sourceHandle || '').replace(
                  source,
                  newSourceNode.id,
                ),
                target: newTargetNode.id,
                targetHandle: newTargetNode.id,
              },
              true,
            );
            newEdges.push(newEdge);
          });
        }
        setNodes([
          ...nodes.map((item) => ({ ...item, selected: false })),
          ...newNodes,
        ]);
        if (newEdges.length) setEdges([...edges, ...newEdges]);
        handleNodeClick(newNode);
        handleSaveFlowDraft();
        message.success(
          $i18n.get({
            id: 'spark-flow.hooks.useNodesInteraction.copySuccess',
            dm: '复制成功',
          }),
        );
      }
    },
    [store, nodeSchemaMap, handleSaveFlowDraft],
  );

  const updateParentNodeSize = useCallback(
    (nodeId: string) => {
      const { nodes, setNodes } = store.getState();
      setNodes(
        nodes.map((item) => {
          if (item.id === nodeId) {
            const parentNodeBounds = getIteratorNodeSize(
              nodes.filter((vItem) => vItem.parentId === item.id),
            );
            return {
              ...item,
              width: parentNodeBounds.width,
              height: parentNodeBounds.height,
            };
          }

          return item;
        }),
      );
    },
    [store],
  );

  return {
    onConnect,
    onAddNewNode,
    onNodeDrag,
    onAddNewNodeWithSource,
    onNodesChange,
    onNodeClick,
    handleNodeClick,
    handleSelectNode,
    onNodeResize,
    onNodeDelete,
    onNodeCopy,
    handleNodeClickByNodeId,
    updateParentNodeSize,
    onDrop,
    onDragOver,
  };
};
