import $i18n from '@/i18n';
import { CodeInput, IValueType } from '@spark-ai/flow';
import { Input, InputNumber, Select } from 'antd';
import { memo, useMemo } from 'react';
import { FileInput } from './FileInput';

interface IVariableInputProps {
  value?: string;
  type?: IValueType;
  onChange: (value: { value?: string; type?: IValueType }) => void;
  placeholder?: string;
  isCompact?: boolean;
  variant?: 'borderless';
  prefix?: React.ReactNode;
  disabled?: boolean;
}

export const VariableBaseInput = memo(
  ({
    value,
    onChange,
    type,
    placeholder,
    disabled,
    variant,
    prefix,
  }: IVariableInputProps) => {
    const renderType = useMemo(() => {
      switch (type) {
        case 'Number':
          return (
            <InputNumber
              className="w-full"
              placeholder={
                placeholder ||
                $i18n.get({
                  id: 'main.components.VariableBaseInput.index.inputValue',
                  dm: '输入变量值',
                })
              }
              stringMode
              prefix={prefix}
              variant={variant}
              disabled={disabled}
              value={value}
              onChange={(val) =>
                onChange({ value: val ? val.toString() : undefined })
              }
            />
          );
        case 'String':
          return (
            <Input
              disabled={disabled}
              prefix={prefix}
              placeholder={
                placeholder ||
                $i18n.get({
                  id: 'main.components.VariableBaseInput.index.inputValue',
                  dm: '输入变量值',
                })
              }
              variant={variant}
              value={value}
              onChange={(e) => onChange({ value: e.target.value })}
            />
          );

        case 'Boolean':
          return (
            <Select
              disabled={disabled}
              variant={variant}
              className="w-full"
              value={value}
              placeholder={$i18n.get({
                id: 'main.components.VariableBaseInput.index.select',
                dm: '请选择',
              })}
              options={[
                {
                  label: $i18n.get({
                    id: 'main.components.VariableBaseInput.index.yes',
                    dm: '是',
                  }),
                  value: 'true',
                },
                {
                  label: $i18n.get({
                    id: 'main.components.VariableBaseInput.index.no',
                    dm: '否',
                  }),
                  value: 'false',
                },
              ]}
              onChange={(val) => onChange({ value: val })}
            />
          );

        case 'Object':
        case 'Array<Object>':
        case 'Array<String>':
        case 'Array<Number>':
        case 'Array<Boolean>':
          return (
            <CodeInput
              disabled={disabled}
              type={type}
              value={value}
              onChange={(val) => onChange({ value: val })}
            />
          );

        case 'File':
        case 'Array<File>':
          return (
            <FileInput
              value={value}
              isSingle={type === 'File'}
              onChange={(val) => onChange({ value: val })}
            />
          );

        default:
          return null;
      }
    }, [type, value, onChange]);
    return renderType;
  },
);
