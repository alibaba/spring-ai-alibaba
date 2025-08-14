import $i18n from '@/i18n';
import { Button, IconFont, Select } from '@spark-ai/design';
import {
  VariableInput,
  VariableTreeSelect,
  uniqueId,
  useNodeDataUpdate,
} from '@spark-ai/flow';
import React, { memo, useCallback } from 'react';
import {
  IVariableAssignNodeData,
  IVariableAssignNodeParam,
} from '../../types/flow';

export default memo(function VariableAssignPanel({
  id,
  data,
}: {
  id: string;
  data: IVariableAssignNodeData;
}) {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
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

  return (
    <div className="spark-flow-panel-form-section">
      <div className="spark-flow-panel-form-title">
        {$i18n.get({
          id: 'spark-flow.demos.spark-flow-1.nodes.VariableAssign.panel.setVariable',
          dm: '设置变量',
        })}

        <IconFont type="spark-info-line" />
      </div>
      <div className="spark-flow-panel-form-title-desc flex gap-9">
        <span style={{ width: 120 }}>
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.VariableAssign.panel.variable',
            dm: '变量',
          })}
        </span>
        <span>
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.VariableAssign.panel.setValue',
            dm: '设置值',
          })}
        </span>
      </div>
      {data.node_param.inputs.map((item) => (
        <div key={item.id} className="flex gap-[8px] items-center">
          <div style={{ width: 120 }}>
            <VariableTreeSelect options={[]}>
              <Select open={false} />
            </VariableTreeSelect>
          </div>
          <IconFont
            className="spark-flow-icon-base-color text-[20px]"
            type="spark-leftArrow-line"
          />

          <VariableInput
            value={item.right.value}
            type={item.right.type}
            onChange={() => {}}
          />

          <IconFont
            isCursorPointer
            type="spark-delete-line"
            onClick={() => removeInput(item.id)}
          />
        </div>
      ))}
      <Button
        type="link"
        onClick={handleAdd}
        size="small"
        className="self-start spark-flow-text-btn"
        icon={<IconFont type="spark-plus-line" />}
      >
        {$i18n.get({
          id: 'spark-flow.demos.spark-flow-1.nodes.VariableAssign.panel.addVariable',
          dm: '添加变量',
        })}
      </Button>
    </div>
  );
});
