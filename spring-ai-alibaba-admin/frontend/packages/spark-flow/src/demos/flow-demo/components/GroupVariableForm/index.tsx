import $i18n from '@/i18n';
import { IconFont, Input, Select } from '@spark-ai/design';
import {
  DraggableWithHandle,
  useStore,
  VALUE_FROM_OPTIONS,
  VariableTreeSelect,
} from '@spark-ai/flow';
import { useSetState } from 'ahooks';
import { message } from 'antd';
import React, { memo, useCallback, useRef } from 'react';
import { IVariable, IVariableHandleGroupItem } from '../../types/flow';
import './index.less';

export default memo(function GroupVariableForm({
  data,
  onDelete,
  onChange,
  handleCheckGroupName,
}: {
  data: IVariableHandleGroupItem;
  onDelete: () => void;
  onChange: (group: IVariableHandleGroupItem) => void;
  handleCheckGroupName: (val: string) => Promise<boolean>;
}) {
  const variableTree = useStore((store) => store.variableTree);
  const [state, setState] = useSetState({
    isEdit: false,
    tempName: data.group_name,
  });

  const isComposingRef = useRef(false);

  const changeVariable = useCallback(
    (variables: IVariable[]) => {
      onChange({
        ...data,
        variables,
      });
    },
    [data],
  );

  const changeVariableRowItem = useCallback(
    (id: string, payload: Partial<IVariable>) => {
      const newVariables = data.variables.map((item) => {
        if (item.id === id) {
          return {
            ...item,
            ...payload,
          };
        }
        return item;
      });
      changeVariable(newVariables);
    },
    [data],
  );

  const removeVariable = useCallback(
    (id: string) => {
      const newVariables = data.variables.filter((item) => item.id !== id);
      changeVariable(newVariables);
    },
    [data],
  );

  const renderVariableItem = useCallback(
    (variable: IVariable, dragHandle: React.ReactNode) => (
      <div className="spark-flow-group-variable-form-item flex gap-[8px]">
        {dragHandle}
        <Select
          style={{ width: 50 }}
          disabled
          options={VALUE_FROM_OPTIONS}
          value={variable.value_from}
          onChange={(val) =>
            changeVariableRowItem(variable.id, { value_from: val })
          }
        />

        <VariableTreeSelect options={variableTree}>
          <Select open={false} />
        </VariableTreeSelect>
        <IconFont
          onClick={() => removeVariable(variable.id)}
          isCursorPointer
          type="spark-delete-line"
        />
      </div>
    ),

    [changeVariableRowItem, variableTree],
  );

  const handleSure = useCallback(() => {
    handleCheckGroupName(state.tempName)
      .then(() => {
        onChange({
          ...data,
          group_name: state.tempName,
        });
        setState({
          isEdit: false,
        });
      })
      .catch((msg) => message.warning(msg));
  }, [state.tempName, data]);

  const handleCompositionStart = useCallback(() => {
    isComposingRef.current = true;
  }, []);

  const handleCompositionEnd = useCallback(() => {
    isComposingRef.current = false;
  }, []);

  const handlePressEnter = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (isComposingRef.current) {
        return;
      }

      handleSure();

      e.preventDefault();
    },
    [handleSure],
  );

  return (
    <>
      <div className="spark-flow-group-variable-form flex flex-col gap-[12px]">
        <div className="flex items-center justify-between">
          {state.isEdit ? (
            <div className="flex gap-[8px] items-center w-full">
              <Input
                value={state.tempName}
                onChange={(e) =>
                  setState({
                    tempName: e.target.value,
                  })
                }
                onCompositionStart={handleCompositionStart}
                onCompositionEnd={handleCompositionEnd}
                onPressEnter={handlePressEnter}
              />

              <IconFont
                onClick={handleSure}
                isCursorPointer
                type="spark-true-line"
                className="spark-flow-name-input-ok-btn"
              />

              <IconFont
                onClick={() => {
                  setState({
                    isEdit: false,
                    tempName: data.group_name,
                  });
                }}
                type="spark-false-line"
                isCursorPointer
                className="spark-flow-name-input-cancel-btn"
              />
            </div>
          ) : (
            <div className="flex gap-[4px] items-center spark-flow-panel-form-title">
              {data.group_name}
              <IconFont
                className="spark-flow-hover-icon"
                type="spark-edit-line"
                isCursorPointer
                onClick={() => {
                  setState({
                    isEdit: true,
                    tempName: data.group_name,
                  });
                }}
              />
            </div>
          )}
          {!state.isEdit && (
            <IconFont
              onClick={onDelete}
              isCursorPointer
              className="spark-flow-hover-icon"
              type="spark-delete-line"
            />
          )}
        </div>
        <div className="spark-flow-group-variable-form-label flex gap-[8px] pl-[24px]">
          <span style={{ width: 50 }}>
            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.components.GroupVariableForm.index.referenceMethod',
              dm: '引用方式',
            })}
          </span>
          <span>
            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.components.GroupVariableForm.index.variable',
              dm: '变量',
            })}
          </span>
        </div>

        <DraggableWithHandle
          items={data.variables}
          onChange={changeVariable}
          getItemId={(item) => item.id}
          renderItem={renderVariableItem}
          className="flex flex-col gap-[8px]"
        />
      </div>
    </>
  );
});
