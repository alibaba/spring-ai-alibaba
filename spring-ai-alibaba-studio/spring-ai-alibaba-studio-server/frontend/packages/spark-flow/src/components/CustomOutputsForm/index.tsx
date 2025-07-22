import { FILE_PROPERTIES, VALUE_TYPE_OPTIONS } from '@/constant';
import $i18n from '@/i18n';
import {
  INodeDataOutputParamItem,
  IValueType,
  IValueTypeOption,
} from '@/types/work-flow';
import { Button, Input } from '@spark-ai/design';
import { Cascader } from 'antd';
import classNames from 'classnames';
import React, { memo, useMemo, useState } from 'react';
import CustomIcon from '../CustomIcon';
import { typeAbbr } from '../VarTypePrefix';
import './index.less';

const displayRender = (labels: string[], isMini?: boolean) => {
  const lastLabel = labels[labels.length - 1];
  if (isMini) {
    return (
      <span className="text-[12px]">
        {typeAbbr[lastLabel as keyof typeof typeAbbr]}
      </span>
    );
  }
  return lastLabel;
};
const parseType = (type?: IValueType) => {
  if (!type) return [];
  if (type.includes('Array')) return ['Array' as 'Array', type];
  return [type] as IValueType[];
};

export interface IVariableTypeSelectProps {
  handleChange: (type: IValueType) => void;
  type?: IValueType;
  disabled?: boolean;
  style?: React.CSSProperties;
  isMini?: boolean;
  className?: string;
  disabledTypes?: (IValueType | 'Array')[];
}

export const VariableTypeSelect = memo((props: IVariableTypeSelectProps) => {
  const { handleChange, type, style = {}, disabledTypes = [] } = props;

  const typeOptions = useMemo(() => {
    const list: IValueTypeOption[] = [];
    if (!disabledTypes.length) return VALUE_TYPE_OPTIONS;

    VALUE_TYPE_OPTIONS.forEach((item) => {
      if (disabledTypes.includes(item.value)) return;
      list.push({
        ...item,
        children: item.children?.filter(
          (child) => !disabledTypes.includes(child.value),
        ),
      });
    });

    return list;
  }, [disabledTypes]);

  return (
    <Cascader
      className={`spark-flow-variable-type-select ${props.className}`}
      allowClear={false}
      style={{ width: 140, flexShrink: 0, ...style }}
      options={typeOptions}
      placeholder={$i18n.get({
        id: 'spark-flow.components.CustomOutputsForm.index.selectVariableType',
        dm: '请选择变量类型',
      })}
      getPopupContainer={(ele) => ele}
      value={parseType(type)}
      onChange={(selectedList) => {
        const type = selectedList[selectedList.length - 1] as IValueType;
        handleChange(type);
      }}
      displayRender={(payload) => displayRender(payload, props.isMini)}
      disabled={props.disabled}
    />
  );
});

export interface ICustomOutputsFormProps {
  value?: INodeDataOutputParamItem[];
  onChange?: (params: INodeDataOutputParamItem[]) => void;
  readyOnly?: boolean;
  isRoot?: boolean;
}

export const CustomOutputsForm = memo(function ({
  value = [] as INodeDataOutputParamItem[],
  onChange,
  readyOnly,
  isRoot = false,
}: ICustomOutputsFormProps) {
  const [expand, setExpand] = useState(true);
  const handleChange = (
    ind: number,
    payload: Partial<INodeDataOutputParamItem>,
  ) => {
    const newValue = value.map((item, index) => {
      if (index === ind)
        return {
          ...item,
          ...payload,
        };
      return item;
    });
    onChange?.(newValue);
  };

  const handleAdd = () => {
    onChange?.(
      value.concat({
        key: '',
        type: 'String',
        desc: '',
      }),
    );
  };

  const handleDelete = (index: number) => {
    onChange?.(value.filter((_, i) => i !== index));
  };

  return (
    <>
      {value.map((item, index) => {
        const hasProperties =
          item.type === 'Array<Object>' || item.type === 'Object';
        return (
          <div
            key={index}
            className={classNames('flex flex-col gap-[12px]', {
              ['spark-flow-inputs-form-parent-node']: hasProperties,
              ['spark-flow-inputs-form-parent-node-hidden']: !expand,
            })}
          >
            <div className="spark-flow-inputs-form-item flex gap-[8px] items-center">
              <Input
                disabled={readyOnly}
                style={{ flex: 1 }}
                value={item.key}
                placeholder={$i18n.get({
                  id: 'spark-flow.components.CustomOutputsForm.index.enterVariableName',
                  dm: '请输入变量名',
                })}
                onChange={(e) => handleChange(index, { key: e.target.value })}
              />

              <VariableTypeSelect
                type={item.type}
                disabled={readyOnly}
                handleChange={(type) => {
                  handleChange(index, {
                    type,
                    properties: type.includes('File') ? FILE_PROPERTIES : [],
                  });
                }}
              />

              <Input
                style={{ width: 100, flexShrink: 0 }}
                disabled={readyOnly}
                value={item.desc}
                placeholder={$i18n.get({
                  id: 'spark-flow.components.CustomOutputsForm.index.enterVariableDescription',
                  dm: '请输入变量描述',
                })}
                onChange={(e) => handleChange(index, { desc: e.target.value })}
              />

              {!readyOnly && (
                <CustomIcon
                  className="flex-shrink-0 cursor-pointer"
                  type="spark-delete-line"
                  onClick={() => handleDelete(index)}
                />
              )}
            </div>
            {hasProperties && (
              <>
                <CustomIcon
                  onClick={() => setExpand(!expand)}
                  className="spark-flow-inputs-expand-btn cursor-pointer"
                  type="spark-up-line"
                  size="small"
                />

                {expand && (
                  <div className="pl-[16px] flex flex-col gap-[12px]">
                    <CustomOutputsForm
                      value={item.properties}
                      readyOnly={readyOnly}
                      onChange={(properties) =>
                        handleChange(index, { properties })
                      }
                    />
                  </div>
                )}
              </>
            )}
          </div>
        );
      })}
      {!readyOnly && (
        <Button
          type="link"
          onClick={handleAdd}
          size="small"
          className="self-start spark-flow-text-btn"
          icon={<CustomIcon type="spark-plus-line" />}
        >
          {isRoot
            ? $i18n.get({
                id: 'spark-flow.components.CustomOutputsForm.index.addVariable',
                dm: '添加变量',
              })
            : $i18n.get({
                id: 'spark-flow.components.CustomOutputsForm.index.addSubVariable',
                dm: '添加子变量',
              })}
        </Button>
      )}
    </>
  );
});

export const CustomOutputsFormWrap = memo(function (
  props: ICustomOutputsFormProps,
) {
  const { value, onChange, readyOnly } = props;

  return (
    <div className="flex flex-col gap-[12px]">
      <div className="spark-flow-inputs-form-label flex gap-[8px]">
        <div style={{ flex: 1 }}>
          {$i18n.get({
            id: 'spark-flow.components.CustomOutputsForm.index.variableName',
            dm: '变量名',
          })}
        </div>
        <div style={{ width: 140 }}>
          {$i18n.get({
            id: 'spark-flow.components.CustomOutputsForm.index.type',
            dm: '类型',
          })}
        </div>
        <div style={{ width: readyOnly ? 100 : 124 }}>
          {$i18n.get({
            id: 'spark-flow.components.CustomOutputsForm.index.description',
            dm: '描述',
          })}
        </div>
      </div>
      <CustomOutputsForm
        value={value}
        onChange={onChange}
        readyOnly={readyOnly}
        isRoot
      />
    </div>
  );
});
