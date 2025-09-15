import $i18n from '@/i18n';
import { Button, IconFont, Select } from '@spark-ai/design';
import type { IValueType, IVarItem, IVarTreeItem } from '@spark-ai/flow';
import {
  uniqueId,
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
  VariableFormComp,
  variableFromLabelRender,
  VariableSelector,
} from '@spark-ai/flow';
import { Flex, Space } from 'antd';
import { memo, useCallback, useMemo } from 'react';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import {
  IIteratorNodeParam,
  IVariableAssignNodeData,
  IVariableAssignNodeParam,
} from '../../types';

const VALUE_FROM_OPTIONS = [
  {
    label: (
      <div className="flex items-center gap-[8px]">
        <IconFont size="small" type="spark-quotation-line" />
        {$i18n.get({
          id: 'spark-flow.components.CustomInputsControl.index.reference',
          dm: '引用',
        })}
      </div>
    ),
    value: 'refer',
  },
  {
    label: (
      <div className="flex items-center gap-[8px]">
        <IconFont size="small" type="spark-edit-line" />
        {$i18n.get({
          id: 'spark-flow.components.CustomInputsControl.index.input',
          dm: '输入',
        })}
      </div>
    ),
    value: 'input',
  },
  {
    label: (
      <div className="flex items-center gap-[8px]">
        <IconFont size="small" type="spark-clear-line" />
        {$i18n.get({
          id: 'spark-flow.components.CustomInputsControl.index.clear',
          dm: '清空',
        })}
      </div>
    ),
    value: 'clear',
  },
];

export default memo(function VariableAssignPanel({
  id,
  parentId,
  data,
}: {
  id: string;
  parentId?: string;
  data: IVariableAssignNodeData;
}) {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const globalVariableList = useWorkflowAppStore(
    (state) => state.globalVariableList,
  );
  const { getVariableList } = useNodesOutputParams();

  const nodes = useReactFlowStore((store) => store.nodes);
  const edges = useReactFlowStore((store) => store.edges);
  const { nodesReadOnly } = useNodesReadOnly();

  const variableList = useMemo(() => {
    const list = getVariableList({
      nodeId: id,
      disableShowVariableParameters: !!parentId,
    });
    return list;
  }, [id, nodes, edges, parentId]);

  const changeNodeParam = useCallback(
    (payload: Partial<IVariableAssignNodeParam>) => {
      handleNodeDataUpdate({
        id,
        data: {
          node_param: {
            ...data.node_param,
            ...payload,
          },
        },
      });
    },
    [data, id],
  );

  const changeInputs = useCallback(
    (inputs: IVariableAssignNodeParam['inputs']) => {
      changeNodeParam({
        inputs,
      });
    },
    [data, id],
  );

  const handleAdd = () => {
    const newInputs: IVariableAssignNodeParam['inputs'] = [
      ...data.node_param.inputs,
      {
        id: uniqueId(4),
        left: {
          value_from: 'refer',
          type: 'String',
        },
        right: {
          value_from: 'refer',
          type: 'String',
        },
      },
    ];

    changeInputs(newInputs);
  };

  const removeInput = useCallback(
    (id: string) => {
      const newInputs = data.node_param.inputs.filter((item) => item.id !== id);
      changeInputs(newInputs);
    },
    [data, id],
  );

  const leftVariables = useMemo(() => {
    if (!parentId) return globalVariableList;
    const parentNode = nodes.find((item) => item.id === parentId);
    if (parentNode?.type !== 'Iterator' || !parentNode)
      return globalVariableList;
    const list: IVarTreeItem[] = [...globalVariableList];
    const params: IVarItem[] = [];
    (
      parentNode.data.node_param as IIteratorNodeParam
    ).variable_parameters?.forEach((item) => {
      if (!item.value) return;
      params.push({
        label: item.key,
        value: `\${${parentNode.id}.${item.key}}`,
        type: item.type as IValueType,
      });
    });
    if (!params.length) return list;
    list.push({
      label: parentNode.data.label as string,
      nodeId: parentNode.id,
      nodeType: parentNode.type as string,
      children: params,
    });
    return list;
  }, [globalVariableList, parentId, nodes]);

  const changeRowItem = useCallback(
    (id: string, payload: Partial<IVariableAssignNodeParam['inputs'][0]>) => {
      const newInputs = data.node_param.inputs.map((item) =>
        item.id === id ? { ...item, ...payload } : item,
      );
      changeInputs(newInputs);
    },
    [data, id],
  );

  return (
    <div className="spark-flow-panel-form-section">
      <Flex vertical gap={12}>
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'main.pages.App.Workflow.nodes.VariableAssign.panel.setVariable',
            dm: '设置变量',
          })}
        </div>
        <div className="spark-flow-panel-form-title-desc flex gap-[36px]">
          <span style={{ width: '40%' }}>
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.VariableAssign.panel.variable',
              dm: '变量',
            })}
          </span>
          <span>
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.VariableAssign.panel.setValue',
              dm: '设置值',
            })}
          </span>
        </div>
        {data.node_param.inputs.map((item) => (
          <div key={item.id} className="flex gap-[8px] items-center">
            <div style={{ width: '40%' }}>
              <VariableSelector
                disabled={nodesReadOnly}
                variableList={leftVariables}
                value={item.left}
                onChange={(val) => {
                  changeRowItem(item.id, {
                    left: {
                      ...item.left,
                      ...val,
                    },
                    right: {
                      ...item.right,
                      value: void 0,
                      type: val.type,
                    },
                  });
                }}
              />
            </div>
            <IconFont
              className="spark-flow-icon-base-color text-xl"
              type="spark-leftArrow-line"
            />

            <Space.Compact style={{ flex: 1 }}>
              <Select
                disabled={nodesReadOnly}
                style={{ width: 60 }}
                className="flex-shrink-0 spark-flow-variable-from-select"
                value={item.right.value_from}
                onChange={(val) =>
                  changeRowItem(item.id, {
                    right: {
                      ...item.right,
                      value_from: val,
                      value: void 0,
                    },
                  })
                }
                options={VALUE_FROM_OPTIONS}
                labelRender={(props) =>
                  variableFromLabelRender(props.value as string)
                }
                popupMatchSelectWidth={false}
              />
              <VariableFormComp
                disabledType
                isCompact
                disabled={nodesReadOnly}
                data={item.right || 'String'}
                variableList={variableList}
                onChange={(val) => {
                  changeRowItem(item.id, {
                    right: {
                      ...item.right,
                      ...val,
                    },
                  });
                }}
              />
            </Space.Compact>
            <IconFont
              type="spark-delete-line"
              onClick={() => removeInput(item.id)}
              isCursorPointer={!nodesReadOnly}
              className={nodesReadOnly ? 'disabled-icon-btn' : ''}
            />
          </div>
        ))}
        <Button
          type="link"
          onClick={handleAdd}
          disabled={nodesReadOnly}
          size="small"
          className="self-start spark-flow-text-btn"
          icon={<IconFont type="spark-plus-line" />}
        >
          {$i18n.get({
            id: 'main.pages.App.Workflow.nodes.VariableAssign.panel.addVariable',
            dm: '添加变量',
          })}
        </Button>
      </Flex>
    </div>
  );
});
