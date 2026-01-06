import { IWorkFlowNodeResultItem } from '@/types/work-flow';
import {
  applyEdgeChanges,
  Edge,
  EdgeMouseHandler,
  OnEdgesChange,
  OnReconnect,
  reconnectEdge,
  useStoreApi,
} from '@xyflow/react';
import { useCallback } from 'react';
import { useFlowSave } from './useFlowSave';

export const useEdgesInteraction = () => {
  const store = useStoreApi();
  const { handleSaveFlowDraft } = useFlowSave();

  const updateEdgeByNodeResults = useCallback(
    (val: IWorkFlowNodeResultItem[]) => {
      const { edges, setEdges } = store.getState();
      const cacheNodeResult: Record<string, IWorkFlowNodeResultItem> = {};
      const newEdges: Edge[] = edges.map((item) => {
        const { source, target } = item;
        const sourceNodeResult =
          cacheNodeResult[source] || val.find((v) => v.node_id === source);
        const targetNodeResult =
          cacheNodeResult[target] || val.find((v) => v.node_id === target);
        if (!cacheNodeResult[source]) {
          cacheNodeResult[source] = sourceNodeResult;
        }
        if (!cacheNodeResult[target]) {
          cacheNodeResult[target] = targetNodeResult;
        }
        if (!!sourceNodeResult && !!targetNodeResult)
          return {
            ...item,
            animated: targetNodeResult.node_status === 'executing',
            data: {
              ...item.data,
              _source_node_status: sourceNodeResult.node_status,
              _target_node_status: targetNodeResult.node_status,
            },
          };
        return {
          ...item,
          animated: false,
          data: {
            ...item.data,
            _source_node_status: undefined,
            _target_node_status: undefined,
          },
        };
      });
      setEdges(newEdges);
    },
    [store],
  );

  const hiddenEdgeStatus = useCallback(() => {
    const { edges, setEdges } = store.getState();
    const newEdges = edges.map((item) => {
      return {
        ...item,
        animated: false,
        data: {
          ...item.data,
          _source_node_status: undefined,
          _target_node_status: undefined,
        },
      };
    });
    setEdges(newEdges);
  }, [store]);

  const setEdgeData = useCallback(
    (edgeId: string, payload: Record<string, any>) => {
      const { edges, setEdges } = store.getState();
      const newEdges = edges.map((item) => {
        if (item.id === edgeId) {
          return {
            ...item,
            data: {
              ...(item.data || {}),
              ...payload,
            },
          };
        }
        return item;
      });
      setEdges(newEdges);
    },
    [store],
  );

  const onEdgeEnter: EdgeMouseHandler = useCallback(
    (_, edge) => {
      setEdgeData(edge.id, { _hover: true });
    },
    [store],
  );

  const onEdgeLeave: EdgeMouseHandler = useCallback(
    (_, edge) => {
      setEdgeData(edge.id, { _hover: false });
    },
    [store],
  );

  const onEdgesChange = useCallback<OnEdgesChange>(
    (changes) => {
      const { edges, setEdges } = store.getState();
      const newEdges = applyEdgeChanges(changes, edges);
      setEdges(newEdges);
      if (changes.some((change) => change.type === 'remove'))
        handleSaveFlowDraft();
    },
    [store, handleSaveFlowDraft],
  );

  const onReconnect: OnReconnect = useCallback(
    (oldEdge, newConnection) => {
      const { edges, setEdges } = store.getState();
      setEdges(reconnectEdge(oldEdge, newConnection, edges));
      handleSaveFlowDraft();
    },
    [store, handleSaveFlowDraft],
  );

  const handleRemoveEdgeByTargetId = useCallback(
    (targetId: string) => {
      const { edges, setEdges } = store.getState();
      setEdges(edges.filter((item) => item.sourceHandle !== targetId));
      handleSaveFlowDraft();
    },
    [store, handleSaveFlowDraft],
  );

  return {
    onEdgeEnter,
    onEdgeLeave,
    onEdgesChange,
    updateEdgeByNodeResults,
    hiddenEdgeStatus,
    onReconnect,
    handleRemoveEdgeByTargetId,
  };
};
