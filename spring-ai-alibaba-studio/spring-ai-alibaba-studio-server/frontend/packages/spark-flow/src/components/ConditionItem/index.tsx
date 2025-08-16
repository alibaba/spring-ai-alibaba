import { filterVarItemsByType } from '@/hooks';
import { IConditionItem, IValueType } from '@/types/work-flow';
import { Divider, Flex, Select } from 'antd';
import React, { useMemo } from 'react';
import { OPERATOR_OPTS_MAP } from '../../constant';
import {
  VALUE_FROM_OPTIONS,
  variableFromLabelRender,
  VariableSelector,
} from '../CustomInputsControl';
import { VariableBaseInput } from '../VariableInput';
import { IVarTreeItem } from '../VariableTreeSelect';
import './index.less';

const getRightType: (type: IValueType) => IValueType = (type) => {
  return type?.includes('Array') || ['Object', 'File'].includes(type)
    ? 'String'
    : type;
};

export const operatorLabelRender = (val: string) => {
  switch (val) {
    case 'equals':
    case 'isNull':
    case 'lengthEquals':
    case 'isTrue':
    case 'isFalse':
      return '=';
    case 'notEquals':
    case 'isNotNull':
      return '≠';
    case 'greater':
    case 'lengthGreater':
      return '>';
    case 'greaterAndEqual':
    case 'lengthGreaterAndEqual':
      return '≥';
    case 'less':
    case 'lengthLess':
      return '<';
    case 'lessAndEqual':
    case 'lengthLessAndEqual':
      return '≤';
    case 'contains':
      return '∋';
    case 'notContains':
      return '∌';
  }
};

export interface IConditionItemProps {
  value: IConditionItem;
  onChange: (value: Partial<IConditionItem>) => void;
  leftVariableList: IVarTreeItem[];
  rightVariableList: IVarTreeItem[];
  disabled?: boolean;
}

export default function ConditionItem(props: IConditionItemProps) {
  const memoRightVariableListByType = useMemo(() => {
    const list: IVarTreeItem[] = [];
    props.rightVariableList.forEach((item) => {
      const subList = filterVarItemsByType(
        item.children,
        props.value.right.type
          ? ([props.value.right.type] as IValueType[])
          : [],
      );
      if (subList.length > 0) {
        list.push({
          ...item,
          children: subList,
        });
      }
    });
    return list;
  }, [props.value.right.type, props.rightVariableList]);

  const memoRightForm = useMemo(() => {
    switch (props.value.operator) {
      case 'isNull':
      case 'isNotNull':
        return (
          <div className="spark-flow-condition-item-disabled-label">Null</div>
        );

      case 'isTrue':
        return (
          <div className="spark-flow-condition-item-disabled-label">True</div>
        );

      case 'isFalse':
        return (
          <div className="spark-flow-condition-item-disabled-label">False</div>
        );

      default:
        return (
          <Flex align="center">
            <Select
              disabled={props.disabled}
              variant="borderless"
              style={{ width: 60 }}
              options={VALUE_FROM_OPTIONS}
              value={props.value.right.value_from}
              onChange={(val) =>
                props.onChange({
                  right: {
                    ...props.value.right,
                    value_from: val,
                    value: void 0,
                  },
                })
              }
              popupMatchSelectWidth={false}
              className="spark-flow-variable-from-select"
              labelRender={(val) =>
                variableFromLabelRender(val.value as string)
              }
            />

            <Divider
              className="spark-flow-condition-item-divider"
              type="vertical"
            />
            {props.value.right.value_from === 'refer' && (
              <div className="flex-1">
                <VariableSelector
                  disabled={props.disabled}
                  variant="borderless"
                  variableList={memoRightVariableListByType}
                  prefix={props.value.right.type}
                  value={props.value.right}
                  onChange={(val) =>
                    props.onChange({ right: { ...props.value.right, ...val } })
                  }
                />
              </div>
            )}
            {props.value.right.value_from === 'input' && (
              <div className="flex-1">
                <VariableBaseInput
                  disabled={props.disabled}
                  variant="borderless"
                  value={props.value.right.value}
                  prefix={props.value.right.type}
                  onChange={(val) =>
                    props.onChange({ right: { ...props.value.right, ...val } })
                  }
                  type={props.value.right.type}
                />
              </div>
            )}
          </Flex>
        );
    }
  }, [props.value, props.disabled, props.onChange]);

  return (
    <div className="spark-flow-condition-item">
      <Flex align="center" className="spark-flow-condition-item-header">
        <div className="flex-1">
          <VariableSelector
            variant="borderless"
            variableList={props.leftVariableList}
            value={props.value.left}
            disabled={props.disabled}
            onChange={(val) => {
              props.onChange({
                left: { ...props.value.left, ...val },
                right:
                  props.value.left.type === val.type
                    ? props.value.right
                    : {
                        value_from: props.value.right.value_from,
                        value: void 0,
                        type: getRightType(val.type as IValueType),
                      },
                operator: void 0,
              });
            }}
          />
        </div>
        <Divider
          className={'spark-flow-condition-item-divider'}
          type="vertical"
        />
        <Select
          disabled={props.disabled}
          labelRender={(val) => operatorLabelRender(val.value as string)}
          variant="borderless"
          style={{ width: 80 }}
          value={props.value.operator}
          onChange={(val) => {
            // set to default type (based on left condition)
            const right = {
              ...props.value.right,
              type: getRightType(props.value.left.type as IValueType),
              value: void 0,
            };

            // if the condition is related to length, set to Number type
            if (
              [
                'greater',
                'greaterAndEqual',
                'less',
                'lessAndEqual',
                'lengthGreater',
                'lengthGreaterAndEqual',
                'lengthLess',
                'lengthLessAndEqual',
                'lengthEquals',
              ].includes(val)
            ) {
              right.type = 'Number';
            }

            props.onChange({ operator: val, right });
          }}
          popupMatchSelectWidth={false}
          options={
            OPERATOR_OPTS_MAP[
              (props.value.left.type ||
                'String') as keyof typeof OPERATOR_OPTS_MAP
            ]
          }
        />
      </Flex>
      {memoRightForm}
    </div>
  );
}
