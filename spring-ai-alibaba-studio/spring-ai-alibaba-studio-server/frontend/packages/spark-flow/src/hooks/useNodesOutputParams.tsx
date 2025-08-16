import { IVarItem, IVarTreeItem } from '@/components/VariableTreeSelect';
import { useStore } from '@/flow/context';
import $i18n from '@/i18n';
import {
  INodeDataOutputParamItem,
  IValueType,
  IWorkFlowNode,
} from '@/types/work-flow';
// import { getParentNodeVariableList } from '@/utils';
import { getNeighborNodes } from '@/utils/workFlow';
import { useStoreApi } from '@xyflow/react';
import { useCallback } from 'react';

interface IQueryVariableListParams {
  type?: IValueType;
  scope?: 'sub' | 'global';
  nodeId: string;
  disableShowParentParams?: boolean;
  disableShowVariableParameters?: boolean;
}

export const buildOutputParamsTree = ({
  outputParams,
  nodeId,
  parentKey = '',
  parentType = '',
  arrayPathStack = [] as string[],
  arrayWrap = false,
  flat = false,
}: {
  outputParams: Array<INodeDataOutputParamItem>;
  nodeId: string;
  parentKey?: string;
  parentType?: string;
  arrayPathStack?: string[];
  arrayWrap?: boolean;
  flat?: boolean;
}) => {
  const list: IVarItem[] = [];
  const flatList: IVarItem[] = [];
  const keySet = new Set<string>();

  const processItem = (item: INodeDataOutputParamItem) => {
    if (!item.key || keySet.has(item.key)) return;
    keySet.add(item.key);

    const itemKey = parentKey ? `${parentKey}.${item.key}` : item.key;
    const itemArrayPathStack = [...arrayPathStack];
    if (parentType === 'Array<Object>') {
      itemArrayPathStack.push(item.key);
    }

    /* Build value format */
    let valueFormat = '';

    if (itemArrayPathStack.length > 0) {
      /* Handle multi-level nested Array<Object> */
      const baseProperty = itemKey.split('.')[0];
      let nestedPath = itemArrayPathStack[itemArrayPathStack.length - 1];
      for (let i = itemArrayPathStack.length - 2; i >= 0; i--) {
        nestedPath = `${itemArrayPathStack[i]}.[${nestedPath}]`;
      }
      valueFormat = `\${${nodeId}.${baseProperty}.[${nestedPath}]}`;
    } else {
      /* Normal property */
      if (arrayWrap) {
        /* If arrayWrap is true, wrap all properties as array structure */
        const pathParts = itemKey.split('.');
        if (pathParts.length === 1) {
          /* Single-level property, e.g. ${Script_id.result} -> ${Script_id.[result]} */
          valueFormat = `\${${nodeId}.[${itemKey}]}`;
        } else {
          /* Multi-level property, e.g. ${Script_id.result.name} -> ${Script_id.[result.name]} */
          const baseProperty = pathParts[0];
          const nestedPath = pathParts.slice(1).join('.');
          valueFormat = `\${${nodeId}.${baseProperty}.[${nestedPath}]}`;
        }
      } else {
        valueFormat = `\${${nodeId}.${itemKey}}`;
      }
    }

    /* Generate new type based on arrayWrap and original type */
    let newType: IValueType;
    if (arrayWrap) {
      newType = `Array<${item.type || 'String'}>` as IValueType;
    } else {
      newType = item.type as IValueType;
    }

    const varItem: IVarItem = {
      label: item.key,
      value: valueFormat,
      type: newType,
    };

    if (flat) {
      flatList.push(varItem);
    }

    if (
      item.properties &&
      item.type === 'Object' &&
      item.properties.length > 0
    ) {
      const childItems = buildOutputParamsTree({
        outputParams: item.properties,
        nodeId,
        parentKey: itemKey,
        parentType: item.type,
        arrayPathStack: itemArrayPathStack,
        arrayWrap,
        flat,
      });

      if (childItems.length > 0) {
        if (!flat) {
          varItem.children = childItems;
          list.push(varItem);
        }
      } else if (!flat) {
        list.push(varItem);
      }
    } else if (!flat) {
      list.push(varItem);
    }
  };

  outputParams.forEach(processItem);

  return flat ? flatList : list;
};

export const filterVarItemsByType = (
  items: IVarItem[],
  types: IValueType[],
): IVarItem[] => {
  return items.reduce<IVarItem[]>((filtered, item) => {
    /* If the type of the current item matches, add it to the result */
    if (types.includes(item.type as IValueType)) {
      filtered.push(item);
    }

    /* If there are children, recursively process the children */
    if (item.children && item.children.length > 0) {
      const filteredChildren = filterVarItemsByType(item.children, types);
      if (filteredChildren.length > 0) {
        /* If there are matches in the children, keep the current item and update its children */
        filtered.push({
          ...item,
          children: filteredChildren,
        });
      }
    }

    return filtered;
  }, []);
};

export const useNodesOutputParams = () => {
  const store = useStoreApi();
  const nodeSchemaMap = useStore((state) => state.nodeSchemaMap);

  const getOutputParamsFromNodesMap = useCallback(
    (
      nodesMap: Record<string, IWorkFlowNode>,
      options: { arrayWrap?: boolean } = {},
    ) => {
      const outputParams: Array<IVarTreeItem> = [];
      Object.keys(nodesMap).forEach((nodeId) => {
        const node = nodesMap[nodeId];
        if (node.data.output_params?.length > 0) {
          const varItems = buildOutputParamsTree({
            outputParams: node.data.output_params,
            nodeId,
            ...options,
          });

          if (varItems.length > 0) {
            outputParams.push({
              label: node.data.label || node.id,
              nodeId: node.id,
              nodeType: node.type,
              children: varItems,
            });
          }
        }
      });
      return outputParams;
    },
    [store],
  );

  const getSystemVariableList = useCallback(() => {
    return [
      {
        label: $i18n.get({
          id: 'spark-flow.hooks.useNodesOutputParams.builtinVariable',
          dm: '内置变量',
        }),
        nodeId: 'sys',
        nodeType: 'sys',
        children: [
          {
            label: 'query',
            value: '${sys.query}',
            type: 'String',
          },
          {
            label: 'history_list',
            value: '${sys.history_list}',
            type: 'Array<String>',
          },
        ],
      },
    ] as IVarTreeItem[];
  }, []);

  const getSubNodesVariableList = useCallback(
    (subNodes: IWorkFlowNode[]) => {
      const nodesMap = subNodes.reduce(
        (acc, node) => {
          acc[node.id] = node;
          return acc;
        },
        {} as Record<string, IWorkFlowNode>,
      );

      const variableList = getOutputParamsFromNodesMap(nodesMap, {
        arrayWrap: true,
      });
      return variableList;
    },
    [store, getOutputParamsFromNodesMap],
  );

  const getVariableList = useCallback(
    (params: IQueryVariableListParams) => {
      const { nodeId } = params;
      const { edges, nodes } = store.getState();
      const targetNode = nodes.find((node) => node.id === nodeId);
      if (!targetNode) return [];
      const globalNodes: IWorkFlowNode[] = [];
      const subNodes: IWorkFlowNode[] = [];
      (nodes as IWorkFlowNode[]).forEach((node) => {
        if (!node.parentId) {
          /* Nodes without parentId belong to global nodes */
          globalNodes.push(node);
        } else if (node.parentId === targetNode.parentId) {
          subNodes.push(node);
        }
      });

      /* Get predecessor nodes */
      let neighborNodesMap: Record<string, IWorkFlowNode> = {};
      let parentVariableList: IVarTreeItem[] = [];
      if (targetNode.parentId) {
        /* If it is a sub-node, get the predecessor nodes of the parent node or the predecessor nodes in the sub-canvas */
        const parentNode = nodes.find(
          (node) => node.id === targetNode.parentId,
        );
        if (parentNode) {
          if (
            !params.disableShowParentParams &&
            !!nodeSchemaMap[parentNode.type as string]
              ?.getParentNodeVariableList
          ) {
            parentVariableList =
              nodeSchemaMap[
                parentNode.type as string
              ].getParentNodeVariableList?.(parentNode as IWorkFlowNode, {
                disableShowVariableParameters:
                  params.disableShowVariableParameters,
              }) || [];
          }

          neighborNodesMap = {
            ...neighborNodesMap,
            ...getNeighborNodes({
              nodes: globalNodes,
              edges,
              nodeId: parentNode.id,
            }),
            ...getNeighborNodes({
              nodes: subNodes,
              edges,
              nodeId: targetNode.id,
            }),
          };
        }
      } else {
        neighborNodesMap = {
          ...neighborNodesMap,
          ...getNeighborNodes({
            nodes: globalNodes,
            edges,
            nodeId: targetNode.id,
          }),
        };
      }

      return [
        ...parentVariableList,
        ...getSystemVariableList(),
        ...getOutputParamsFromNodesMap(neighborNodesMap),
      ];
    },
    [store, getOutputParamsFromNodesMap, nodeSchemaMap],
  );

  const getSubNodesVariables = useCallback(
    (nodeId: string) => {
      const { nodes } = store.getState();
      return getSubNodesVariableList(
        nodes.filter((item) => item.parentId === nodeId) as IWorkFlowNode[],
      );
    },
    [store],
  );

  return {
    getVariableList,
    getSubNodesVariables,
    getSystemVariableList,
    getOutputParamsFromNodesMap,
  };
};
