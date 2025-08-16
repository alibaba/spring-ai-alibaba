import $i18n from '@/i18n';
import { Button, IconFont, Input } from '@spark-ai/design';
import {
  INodeDataInputParamItem,
  IVarTreeItem,
  VariableSelector,
} from '@spark-ai/flow';
import { Flex } from 'antd';
import { memo, useCallback } from 'react';

export interface IIteratorVariableFormProps {
  value: INodeDataInputParamItem[];
  onChange: (value: INodeDataInputParamItem[]) => void;
  variableList: IVarTreeItem[];
  disabled?: boolean;
}

export default memo(function IteratorVariableForm({
  value,
  onChange,
  variableList,
  disabled,
}: IIteratorVariableFormProps) {
  const changeRowItem = useCallback(
    (ind: number, payload: Partial<INodeDataInputParamItem>) => {
      const newVal = value.map((item, index) => {
        if (index === ind) {
          return {
            ...item,
            ...payload,
          };
        }
        return item;
      });
      onChange(newVal);
    },
    [onChange, value],
  );

  const deleteRowItem = useCallback(
    (ind: number) => {
      const newVal = value.filter((_, index) => index !== ind);
      onChange(newVal);
    },
    [onChange, value],
  );

  const addRowItem = useCallback(() => {
    const newVal = [
      ...value,
      {
        key: '',
        value: '',
        value_from: 'refer',
        type: 'String',
      } as INodeDataInputParamItem,
    ];

    onChange(newVal);
  }, [onChange, value]);

  return (
    <Flex vertical gap={12}>
      <Flex gap={8} className={'spark-flow-inputs-form-label'}>
        <div style={{ width: 140 }}>
          {$i18n.get({
            id: 'main.pages.App.Workflow.components.IteratorVariableForm.index.variableName',
            dm: '变量名',
          })}
        </div>
        <div>
          {$i18n.get({
            id: 'main.pages.App.Workflow.components.IteratorVariableForm.index.value',
            dm: '值',
          })}
        </div>
      </Flex>
      {value.map((item, index) => (
        <Flex gap={8} key={index}>
          <div style={{ width: 140 }}>
            <Input
              disabled={disabled}
              className="flex-1"
              placeholder={$i18n.get({
                id: 'main.pages.App.Workflow.components.IteratorVariableForm.index.enterVariableName',
                dm: '请输入变量名',
              })}
              value={item.key}
              onChange={(e) => changeRowItem(index, { key: e.target.value })}
            />
          </div>
          <div className="flex-1">
            <VariableSelector
              disabled={disabled}
              value={item}
              onChange={(val) => changeRowItem(index, val)}
              variableList={variableList}
            />
          </div>
          <IconFont
            onClick={disabled ? undefined : () => deleteRowItem(index)}
            isCursorPointer={!disabled}
            className={disabled ? 'spark-flow-disabled-icon-btn' : ''}
            type="spark-delete-line"
          />
        </Flex>
      ))}
      <Button
        className="spark-flow-text-btn self-start"
        icon={<IconFont type="spark-plus-line" />}
        type="link"
        size="small"
        disabled={disabled}
        onClick={addRowItem}
      >
        {$i18n.get({
          id: 'main.pages.App.Workflow.components.IteratorVariableForm.index.addVariable',
          dm: '添加变量',
        })}
      </Button>
    </Flex>
  );
});
