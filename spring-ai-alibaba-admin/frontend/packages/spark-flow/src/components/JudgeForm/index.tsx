import $i18n from '@/i18n';
import { IBranchItem, IConditionItem } from '@/types/work-flow';
import { generateUniqueName } from '@/utils';
import uniqueId from '@/utils/uniqueId';
import { Button, IconFont } from '@spark-ai/design';
import { Flex } from 'antd';
import classNames from 'classnames';
import React, { memo, useCallback } from 'react';
import BranchTitleHeader from '../BranchTitleHeader';
import ConditionItem from '../ConditionItem';
import { IVarTreeItem } from '../VariableTreeSelect';
import './index.less';

export default memo(function JudgeForm(props: {
  value: IBranchItem[];
  onChange: (val: IBranchItem[]) => void;
  leftVariableList: IVarTreeItem[];
  rightVariableList: IVarTreeItem[];
  disabled?: boolean;
  areaStyle?: React.CSSProperties;
}) {
  const { value = [], onChange, disabled, areaStyle } = props;

  const changeBranchItem = useCallback(
    (id: string, payload: Partial<IBranchItem>) => {
      onChange(
        value.map((item) => (item.id === id ? { ...item, ...payload } : item)),
      );
    },
    [value, onChange],
  );

  const changeConditionItem = useCallback(
    (branchId: string, condIndex: number, payload: Partial<IConditionItem>) => {
      changeBranchItem(branchId, {
        conditions:
          value
            .find((item) => item.id === branchId)
            ?.conditions?.map((item, index) =>
              index === condIndex ? { ...item, ...payload } : item,
            ) || [],
      });
    },
    [value, changeBranchItem],
  );

  const addBranchItem = useCallback(() => {
    onChange([
      ...value,
      {
        id: uniqueId(4),
        label: generateUniqueName(
          $i18n.get({
            id: 'spark-flow.JudgeForm.index.newConditionGroup',
            dm: '新条件组',
          }),

          value.map((item) => item.label),
        ),
        logic: 'and',
        conditions: [
          {
            left: {
              value_from: 'refer',
              type: 'String',
              value: void 0,
            },
            right: {
              value_from: 'input',
              type: 'String',
              value: void 0,
            },
            operator: void 0,
          },
        ],
      },
    ]);
  }, [value]);

  const addConditionItem = useCallback(
    (branchId: string) => {
      const targetBranch = value.find((item) => item.id === branchId);
      if (!targetBranch) return;
      changeBranchItem(branchId, {
        conditions: [
          ...(targetBranch.conditions || []),
          {
            left: {
              value_from: 'refer',
              type: 'String',
              value: void 0,
            },
            right: {
              value_from: 'input',
              type: 'String',
              value: void 0,
            },
            operator: void 0,
          },
        ],
      });
    },
    [value, changeBranchItem],
  );

  const deleteConditionItem = useCallback(
    (branchId: string, condIndex: number) => {
      changeBranchItem(branchId, {
        conditions:
          value
            .find((item) => item.id === branchId)
            ?.conditions?.filter((_, index) => index !== condIndex) || [],
      });
    },
    [value, changeBranchItem],
  );

  const deleteBranchItem = useCallback(
    (branchId: string) => {
      onChange(value.filter((item) => item.id !== branchId));
    },
    [value, onChange],
  );

  return (
    <>
      {value.map((item) => {
        if (item.id === 'default') return null;
        return (
          <div key={item.id}>
            <div
              key={item.id}
              style={areaStyle}
              className="spark-flow-panel-form-section"
            >
              <Flex vertical gap={16}>
                <BranchTitleHeader
                  data={item}
                  onChange={(payload) => changeBranchItem(item.id, payload)}
                  deleteBranchItem={() => deleteBranchItem(item.id)}
                  branches={value}
                  disabled={disabled}
                />

                {(item.conditions || []).map((condition, index) => (
                  <Flex key={index} gap={8}>
                    <div className="flex-1 flex-start">
                      <ConditionItem
                        disabled={disabled}
                        onChange={(val) =>
                          changeConditionItem(item.id, index, val)
                        }
                        value={condition}
                        leftVariableList={props.leftVariableList}
                        rightVariableList={props.rightVariableList}
                      />
                    </div>
                    <div
                      className={classNames(
                        'py-[8px] flex-shrink-0',
                        'spark-flow-judge-form-delete-btn-wrap',
                      )}
                    >
                      <IconFont
                        onClick={() => {
                          deleteConditionItem(item.id, index);
                        }}
                        className={disabled ? 'disabled-icon-btn' : ''}
                        size="small"
                        isCursorPointer={!disabled}
                        type="spark-delete-line"
                      />
                    </div>
                  </Flex>
                ))}
                <Button
                  onClick={() => addConditionItem(item.id)}
                  icon={<IconFont type="spark-plus-line" />}
                  type="dashed"
                  disabled={disabled}
                  className="spark-flow-judge-form-add-branch-btn"
                >
                  {$i18n.get({
                    id: 'spark-flow.JudgeForm.index.addCondition',
                    dm: '添加条件',
                  })}
                </Button>
              </Flex>
            </div>
          </div>
        );
      })}
      <div style={areaStyle} className="spark-flow-panel-form-section">
        <Button
          onClick={() => addBranchItem()}
          icon={<IconFont type="spark-plus-line" />}
          type="dashed"
          className="spark-flow-judge-form-add-branch-btn"
          disabled={disabled}
        >
          {$i18n.get({
            id: 'spark-flow.JudgeForm.index.addConditionGroup',
            dm: '添加条件组',
          })}
        </Button>
      </div>
    </>
  );
});
