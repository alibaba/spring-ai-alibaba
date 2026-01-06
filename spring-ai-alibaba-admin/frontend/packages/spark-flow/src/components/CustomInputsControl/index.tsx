import $i18n from '@/i18n';
import { INodeDataInputParamItem, IValueType } from '@/types/work-flow';
import { extractVariables } from '@/utils';
import { Button, Input, Select, SelectProps } from '@spark-ai/design';
import { Typography } from 'antd';
import React, { memo, useCallback, useMemo } from 'react';
import CustomIcon from '../CustomIcon';
import FlowIcon from '../FlowIcon';
import VariableInput, { VariableBaseInput } from '../VariableInput';
import VariableTreeSelect, { IVarTreeItem } from '../VariableTreeSelect';
import VarTypePrefix, { typeAbbr } from '../VarTypePrefix';
import './index.less';

export interface ICustomInputsControlProps {
  value?: INodeDataInputParamItem[];
  onChange: (value: INodeDataInputParamItem[]) => void;
  variableList?: IVarTreeItem[];
  disabledValueFrom?: boolean;
  disabled?: boolean;
  disabledKey?: boolean;
  disableType?: boolean;
  disabledTypes?: IValueType[];
  defaultType?: IValueType;
}

export interface IVariableFormCompProps {
  data: Omit<INodeDataInputParamItem, 'key'>;
  onChange: (val: Partial<INodeDataInputParamItem>) => void;
  variableList?: IVarTreeItem[];
  disabled?: boolean;
  disabledType?: boolean;
  isCompact?: boolean;
  disabledTypes?: IValueType[];
}

export const variableLabelRender = ({
  value,
  nodeInfo,
  hiddenType,
}: {
  value: Omit<INodeDataInputParamItem, 'key'>;
  nodeInfo?: { nodeName: string; variableKey: string; nodeType: string };
  hiddenType?: boolean;
}) => {
  if (value.value_from !== 'refer' || !value.value || !nodeInfo) {
    return null;
  }

  return (
    <div className="spark-flow-var-label flex text-[12px] items-center gap-[2px]">
      <FlowIcon noWidth nodeType={nodeInfo.nodeType} showBg={false} />
      <Typography.Text
        ellipsis={{ tooltip: nodeInfo.nodeName }}
        style={{ maxWidth: '35%' }}
      >
        {nodeInfo.nodeName}
      </Typography.Text>
      <Typography.Text
        ellipsis={{ tooltip: nodeInfo.variableKey }}
        className="spark-flow-var-name"
      >
        {`/${nodeInfo.variableKey}`}
      </Typography.Text>
      {!hiddenType && (
        <span className="spark-flow-var-type">{`[${typeAbbr[value.type as keyof typeof typeAbbr]}]`}</span>
      )}
    </div>
  );
};

export const VariableSelector = memo(
  (props: {
    value: Omit<INodeDataInputParamItem, 'key'>;
    onChange: (val: Partial<INodeDataInputParamItem>) => void;
    variableList?: IVarTreeItem[];
    prefix?: SelectProps['prefix'];
    variant?: SelectProps['variant'];
    disabled?: boolean;
  }) => {
    const nodeInfo = useMemo(() => {
      if (!props.value.value) return;
      const finalValue = extractVariables(
        props.value.value.replace(/[\[]]/g, ''),
      )[0];
      const list = finalValue.split('.');
      if (!list.length) return;
      const [nodeId, ...variableKeyList] = list;
      const targetNode = props.variableList?.find(
        (node) => node.nodeId === nodeId,
      );
      if (!targetNode) return;
      return {
        nodeName: targetNode.label as string,
        variableKey: variableKeyList.join('.'),
        nodeType: targetNode.nodeType,
      };
    }, [props.value, props.variableList]);

    return (
      <VariableTreeSelect
        onChange={(val) => {
          props.onChange({
            value: val.value,
            type: val.type,
          });
        }}
        disabled={props.disabled}
        options={props.variableList}
      >
        <Select
          disabled={props.disabled}
          placeholder={$i18n.get({
            id: 'spark-flow.components.CustomInputsControl.index.selectVariable',
            dm: '请选择变量',
          })}
          labelRender={() =>
            variableLabelRender({
              value: props.value,
              nodeInfo,
              hiddenType: !!props.prefix,
            })
          }
          className="w-full"
          open={false}
          value={!props.value.value ? undefined : props.value.value}
          prefix={<VarTypePrefix prefix={props.prefix as string} />}
          variant={props.variant}
        />
      </VariableTreeSelect>
    );
  },
);

export const VariableFormComp = memo((props: IVariableFormCompProps) => {
  if (props.data.value_from === 'clear')
    return (
      <Input
        className="flex-1"
        disabled
        placeholder={$i18n.get({
          id: 'spark-flow.components.CustomInputsControl.index.noInputNeeded',
          dm: '不需要输入值',
        })}
      />
    );
  if (props.data.value_from === 'refer')
    return (
      <VariableSelector
        disabled={props.disabled}
        value={props.data}
        onChange={props.onChange}
        variableList={props.variableList}
      />
    );

  if (props.data.value_from === 'input') {
    if (props.disabledType) {
      return (
        <div className="flex-1">
          <VariableBaseInput
            isCompact={props.isCompact}
            disabled={props.disabled}
            value={props.data.value}
            type={props.data.type}
            onChange={props.onChange}
            prefix={props.data.type}
          />
        </div>
      );
    }
    return (
      <VariableInput
        disabledTypes={props.disabledTypes}
        disabled={props.disabled}
        value={props.data.value}
        type={props.data.type}
        onChange={props.onChange}
      />
    );
  }
});

export const variableFromLabelRender = (value: string) => {
  if (value === 'refer')
    return <CustomIcon size="small" type="spark-quotation-line" />;
  if (value === 'input')
    return <CustomIcon size="small" type="spark-edit-line" />;
  if (value === 'clear')
    return <CustomIcon size="small" type="spark-clear-line" />;
  return null;
};

export const VALUE_FROM_OPTIONS = [
  {
    label: (
      <div className="flex items-center gap-[8px]">
        <CustomIcon size="small" type="spark-quotation-line" />
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
        <CustomIcon size="small" type="spark-edit-line" />
        {$i18n.get({
          id: 'spark-flow.components.CustomInputsControl.index.input',
          dm: '输入',
        })}
      </div>
    ),

    value: 'input',
  },
];

export default memo(function CustomInputsControl(
  props: ICustomInputsControlProps,
) {
  const { value = [] as INodeDataInputParamItem[] } = props;

  const changeRowValue = useCallback(
    (ind: number, payload: Partial<INodeDataInputParamItem>) => {
      const newVal = value.map((item, index) => {
        if (index === ind) return { ...item, ...payload };
        return item;
      });
      props.onChange(newVal);
    },
    [props.onChange, value],
  );

  const handleAddVar = useCallback(() => {
    props.onChange([
      ...value,
      { key: '', value_from: 'refer', type: props.defaultType || 'String' },
    ]);
  }, [props.defaultType, props.onChange, value]);

  const removeVariable = useCallback(
    (index: number) => {
      props.onChange(value.filter((_, i) => i !== index));
    },
    [props.onChange, value],
  );

  return (
    <>
      <div className="spark-flow-inputs-form-label flex gap-[8px]">
        <div style={{ width: props.disabledValueFrom ? 146 : 84 }}>
          {$i18n.get({
            id: 'spark-flow.components.CustomInputsControl.index.variableName',
            dm: '变量名',
          })}
        </div>
        {!props.disabledValueFrom && (
          <div style={{ width: 60 }}>
            {$i18n.get({
              id: 'spark-flow.components.CustomInputsControl.index.referenceType',
              dm: '引用方式',
            })}
          </div>
        )}
        <div>
          {$i18n.get({
            id: 'spark-flow.components.CustomInputsControl.index.value',
            dm: '值',
          })}
        </div>
      </div>
      {value.map((item, index) => (
        <div
          key={index}
          className="spark-flow-inputs-form-item flex gap-[8px] items-stretch w-full"
        >
          <Input
            style={{ width: props.disabledValueFrom ? 146 : 84 }}
            className="flex-shrink-0"
            value={item.key}
            placeholder={$i18n.get({
              id: 'spark-flow.components.CustomInputsControl.index.enterVariableName',
              dm: '请输入变量名',
            })}
            onChange={(e) => changeRowValue(index, { key: e.target.value })}
            disabled={props.disabled || props.disabledKey}
          />

          {!props.disabledValueFrom && (
            <Select
              style={{ width: 60 }}
              className="flex-shrink-0 spark-flow-variable-from-select"
              value={item.value_from}
              onChange={(val) =>
                changeRowValue(index, { value_from: val, value: void 0 })
              }
              disabled={props.disabled}
              options={VALUE_FROM_OPTIONS}
              labelRender={(props) =>
                variableFromLabelRender(props.value as string)
              }
              popupMatchSelectWidth={false}
            />
          )}
          <VariableFormComp
            onChange={(payload) => changeRowValue(index, payload)}
            data={item}
            disabled={props.disabled}
            variableList={props.variableList}
            disabledType={props.disableType}
            disabledTypes={props.disabledTypes}
          />

          <CustomIcon
            onClick={() => {
              if (props.disabled) return;
              removeVariable(index);
            }}
            isCursorPointer={!props.disabled}
            className={props.disabled ? 'spark-flow-disabled-icon-btn' : ''}
            type="spark-delete-line"
          />
        </div>
      ))}
      <Button
        className="spark-flow-text-btn self-start"
        icon={<CustomIcon type="spark-plus-line" />}
        type="link"
        size="small"
        onClick={handleAddVar}
        disabled={props.disabled}
      >
        {$i18n.get({
          id: 'spark-flow.components.CustomInputsControl.index.addVariable',
          dm: '添加变量',
        })}
      </Button>
    </>
  );
});
