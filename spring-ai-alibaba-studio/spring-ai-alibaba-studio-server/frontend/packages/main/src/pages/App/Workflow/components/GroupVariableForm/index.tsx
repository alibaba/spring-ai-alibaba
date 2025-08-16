import $i18n from '@/i18n';
import { Button, IconFont, Input, message } from '@spark-ai/design';
import type { IValueType, IVarTreeItem } from '@spark-ai/flow';
import {
  DraggableWithHandle,
  filterVarItemsByType,
  uniqueId,
  VariableSelector,
} from '@spark-ai/flow';
import { useSetState } from 'ahooks';
import { Typography } from 'antd';
import classNames from 'classnames';
import React, { memo, useCallback, useMemo, useRef } from 'react';
import { IVariable, IVariableHandleGroupItem } from '../../types';
import styles from './index.module.less';

export default memo(function GroupVariableForm({
  data,
  onDelete,
  onChange,
  handleCheckGroupName,
  variableList,
  disabled,
}: {
  data: IVariableHandleGroupItem;
  onDelete: () => void;
  onChange: (group: IVariableHandleGroupItem) => void;
  handleCheckGroupName: (val: string) => Promise<boolean>;
  variableList: IVarTreeItem[];
  disabled?: boolean;
}) {
  const [state, setState] = useSetState({
    isEdit: false,
    tempName: data.group_name,
  });

  // use ref to track the Chinese input status
  const isComposingRef = useRef(false);

  const changeVariable = useCallback(
    (variables: IVariable[]) => {
      const hasTypeVariables = variables.filter((item) => !!item.type);
      onChange({
        ...data,
        variables,
        output_type: (hasTypeVariables[0]?.type || 'String') as IValueType,
      });
    },
    [data, onChange],
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
    [data, changeVariable],
  );

  const removeVariable = useCallback(
    (id: string) => {
      const newVariables = data.variables.filter((item) => item.id !== id);
      changeVariable(newVariables);
    },
    [data, changeVariable],
  );

  const addVariable = useCallback(() => {
    changeVariable([
      ...data.variables,
      {
        id: uniqueId(4),
        value: '',
        value_from: 'refer',
      },
    ]);
  }, [data, changeVariable]);

  const filteredVariableList = useMemo(() => {
    if (data.variables.length <= 1) {
      return variableList;
    }
    const list: IVarTreeItem[] = [];
    variableList.forEach((item) => {
      const subList = filterVarItemsByType(item.children, [data.output_type]);
      if (subList.length) {
        list.push({
          ...item,
          children: subList,
        });
      }
    });
    return list;
  }, [variableList, data.variables, data.output_type]);

  const renderVariableItem = useCallback(
    (variable: IVariable, dragHandle: React.ReactNode) => {
      return (
        <div
          className={classNames(
            styles['spark-flow-group-variable-form-item'],
            'flex gap-2',
          )}
        >
          {dragHandle}
          <div className="flex-1">
            <VariableSelector
              disabled={disabled}
              variableList={filteredVariableList}
              value={variable}
              onChange={(val) => {
                changeVariableRowItem(variable.id, val);
              }}
            />
          </div>
          <IconFont
            onClick={() => {
              if (disabled) return;
              removeVariable(variable.id);
            }}
            className={disabled ? 'disabled-icon-btn' : ''}
            type="spark-delete-line"
            isCursorPointer={!disabled}
          />
        </div>
      );
    },

    [changeVariableRowItem, filteredVariableList, data.variables],
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

  // handle Chinese input start event
  const handleCompositionStart = useCallback(() => {
    isComposingRef.current = true;
  }, []);

  // handle Chinese input end event
  const handleCompositionEnd = useCallback(() => {
    isComposingRef.current = false;
  }, []);

  // handle enter event
  const handlePressEnter = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      // if Chinese input is ongoing, do not handle enter event
      if (isComposingRef.current) {
        return;
      }

      // trigger confirm operation
      handleSure();

      // prevent default behavior (such as form submission)
      e.preventDefault();
    },
    [handleSure],
  );

  return (
    <>
      <div
        className={classNames(
          styles['spark-flow-group-variable-form'],
          'flex flex-col gap-[12px]',
        )}
      >
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
                type="spark-true-line"
                isCursorPointer
                className={styles['spark-flow-name-input-ok-btn']}
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
                className={styles['spark-flow-name-input-cancel-btn']}
              />
            </div>
          ) : (
            <div
              className={classNames(
                'flex gap-[4px] items-center',
                styles['spark-flow-panel-form-title'],
              )}
            >
              {!!data.output_type && (
                <div className="spark-flow-var-type">[{data.output_type}]</div>
              )}
              <Typography.Text
                style={{ maxWidth: 200 }}
                ellipsis={{ tooltip: data.group_name }}
              >
                {data.group_name}
              </Typography.Text>
              <IconFont
                className={classNames(
                  styles['spark-flow-hover-icon'],
                  disabled ? 'disabled-icon-btn' : '',
                )}
                isCursorPointer={!disabled}
                type="spark-edit-line"
                onClick={() => {
                  if (disabled) return;
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
              onClick={disabled ? void 0 : onDelete}
              className={classNames(
                disabled ? 'disabled-icon-btn' : '',
                styles['spark-flow-hover-icon'],
              )}
              isCursorPointer={!disabled}
              type="spark-delete-line"
            />
          )}
        </div>
        <DraggableWithHandle
          disabled={disabled}
          items={data.variables}
          onChange={changeVariable}
          getItemId={(item) => item.id}
          renderItem={renderVariableItem}
          className="flex flex-col gap-[8px]"
        />

        <Button
          className="spark-flow-text-btn self-start"
          icon={<IconFont type="spark-plus-line" />}
          type="link"
          size="small"
          onClick={addVariable}
          disabled={disabled}
        >
          {$i18n.get({
            id: 'main.pages.App.Workflow.components.GroupVariableForm.index.addVariable',
            dm: '添加变量',
          })}
        </Button>
      </div>
    </>
  );
});
