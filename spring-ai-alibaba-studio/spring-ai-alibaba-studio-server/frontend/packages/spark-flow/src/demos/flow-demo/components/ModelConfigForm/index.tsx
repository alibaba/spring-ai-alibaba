import $i18n from '@/i18n';
import { Button, IconFont, Slider } from '@spark-ai/design';
import { useStore, VariableTreeSelect } from '@spark-ai/flow';
import { useMount, useSetState } from 'ahooks';
import {
  Checkbox,
  Input,
  InputNumber,
  Popover,
  Select,
  Switch,
  Typography,
} from 'antd';
import React, { memo, useMemo, useState } from 'react';
import { mockModelList, mockModelParamsSchema } from '../../constant/mock';
import { IModelConfigParamItem, ISelectedModelParams } from '../../types/flow';
import { IModelParamsSchema, IModelSelectorItem } from '../../types/model';
import './index.less';

const getStepFromPrecision = (precision?: number): number => {
  if (precision === undefined) return 1;

  const step = Math.pow(0.1, precision);

  return Number(step.toFixed(precision));
};

type ISelectModel = IModelSelectorItem['models'][0];

interface IModelSelectorProps {
  value: ISelectedModelParams;
  onSelect: (model: ISelectModel) => void;
  onChange: (payload: Partial<ISelectedModelParams>) => void;
}

interface IModelOption {
  label: string;
  options: Array<{
    label: string;
    value: string;
    extra: ISelectModel;
  }>;
}

interface IModelConfigBtnProps {
  value: Array<IModelConfigParamItem>;
  onChange: (params: Array<IModelConfigParamItem>) => void;
  schemaList: IModelParamsSchema[];
}

const ModelConfigBtn = memo(
  ({ value, onChange, schemaList }: IModelConfigBtnProps) => {
    const changeParamItem = (
      key: string,
      payload: Partial<IModelConfigParamItem>,
    ) => {
      const newParams = value.map((item) => {
        if (item.key === key) {
          return {
            ...item,
            ...payload,
          };
        }
        return item;
      });
      onChange(newParams);
    };
    const memoForm = useMemo(() => {
      if (!schemaList?.length)
        return $i18n.get({
          id: 'spark-flow.demos.spark-flow-1.components.ModelConfigForm.index.selectModelFirst',
          dm: '请先选择模型',
        });
      return (
        <div className="flex flex-col gap-[12px]">
          {schemaList.map((item) => {
            const targetValue = value.find((vItem) => vItem.key === item.key);
            if (!targetValue) return null;
            return (
              <div key={item.key} className="flex-justify-between gap-[20px]">
                <div className="flex items-center gap-[8px]">
                  <Checkbox
                    checked={targetValue.enable}
                    onChange={(e) =>
                      changeParamItem(item.key, { enable: e.target.checked })
                    }
                  />

                  <Typography.Text
                    className="spark-model-selector-form-label"
                    ellipsis={{ tooltip: item.name }}
                    style={{ width: 78 }}
                  >
                    {item.name}
                  </Typography.Text>
                </div>
                {item.type === 'Number' && (
                  <div className="flex items-center gap-[12px] flex-1">
                    <Slider
                      disabled={!targetValue.enable}
                      style={{ width: 236 }}
                      onChange={(val) =>
                        changeParamItem(item.key, { value: val })
                      }
                      min={item.min}
                      max={item.max}
                      step={getStepFromPrecision(item.precision)}
                      value={targetValue.value as number}
                    />

                    <InputNumber
                      disabled={!targetValue.enable}
                      style={{ width: 70 }}
                      precision={item.precision}
                      value={targetValue.value as number}
                      onChange={(val) =>
                        changeParamItem(item.key, { value: val as number })
                      }
                    />
                  </div>
                )}
                {item.type === 'String' && (
                  <Input
                    disabled={!targetValue.enable}
                    className="flex-1"
                    value={targetValue.value as string}
                    onChange={(e) =>
                      changeParamItem(item.key, { value: e.target.value })
                    }
                  />
                )}
                {item.type === 'Boolean' && (
                  <Switch
                    disabled={!targetValue.enable}
                    checked={targetValue.value as boolean}
                    onChange={(val) =>
                      changeParamItem(item.key, { value: val })
                    }
                  />
                )}
              </div>
            );
          })}
        </div>
      );
    }, [value, onChange, schemaList]);

    return (
      <Popover placement="bottomLeft" content={memoForm} trigger={['click']}>
        <Button icon={<IconFont type="spark-modify-line" />} />
      </Popover>
    );
  },
);

const ModelSelector = memo(
  ({ onChange, onSelect, value }: IModelSelectorProps) => {
    const [state, setState] = useSetState({
      modelOptions: [] as IModelOption[],
      schemaList: [] as IModelParamsSchema[],
    });

    const initModelParams = (model_id: string) => {
      setState({
        schemaList: mockModelParamsSchema,
      });
      const newParams: Array<IModelConfigParamItem> = mockModelParamsSchema.map(
        (item) => {
          const targetParam = value.params.find(
            (param) => param.key === item.key,
          ) || { value: item.default_value, enable: false };
          return {
            key: item.key,
            type: item.type,
            value: targetParam.value,
            enable: targetParam.enable,
          };
        },
      );
      onChange({
        params: newParams,
        model_id,
      });
    };

    useMount(() => {
      const modelOptions = mockModelList.map((item) => ({
        label: item.provider.name,
        options: item.models.map((model) => ({
          label: model.name,
          value: model.model_id,
          extra: model,
        })),
      }));
      setState({
        modelOptions,
      });
      if (value.model_id) initModelParams(value.model_id);
    });

    return (
      <div className="flex items-center gap-[8px]">
        <Select
          placeholder={$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.components.ModelConfigForm.index.selectModel',
            dm: '请选择模型',
          })}
          className="flex-1"
          value={!value.model_id ? void 0 : value.model_id}
          options={state.modelOptions}
          onChange={(value, option: any) => {
            initModelParams(value);
            if (Array.isArray(option)) {
              onSelect(option[0]?.extra as ISelectModel);
            } else {
              onSelect(option?.extra as ISelectModel);
            }
          }}
        />

        <ModelConfigBtn
          schemaList={state.schemaList}
          value={value.params}
          onChange={(params) => {
            onChange({
              params,
            });
          }}
        />
      </div>
    );
  },
);

const ModelConfigForm = ({
  value,
  onChange,
}: {
  value: ISelectedModelParams;
  onChange: (model: Partial<ISelectedModelParams>) => void;
}) => {
  const variableTree = useStore((store) => store.variableTree);
  const [selectedModel, setSelectedModel] = useState(
    null as IModelSelectorItem['models'][0] | null,
  );
  return (
    <>
      <ModelSelector
        onSelect={(model) => {
          setSelectedModel(model);
          onChange({
            model_id: model.model_id,
            model_name: model.name,
            provider: model.provider,
            vision_config: {
              ...value.vision_config,
              enable: !model.tags.includes('vision')
                ? false
                : value.vision_config.enable || false,
            },
          });
        }}
        onChange={onChange}
        value={value}
      />

      <div className="flex-justify-between">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.components.ModelConfigForm.index.visual',
            dm: '视觉',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <Switch
          checked={value.vision_config.enable}
          onChange={(val) =>
            onChange({
              vision_config: {
                ...value.vision_config,
                enable: val,
              },
            })
          }
          disabled={!selectedModel?.tags.includes('vision')}
        />
      </div>
      <div className="spark-flow-panel-form-title">
        {$i18n.get({
          id: 'spark-flow.demos.spark-flow-1.components.ModelConfigForm.index.variable',
          dm: '变量',
        })}
      </div>
      {value.vision_config.enable && (
        <VariableTreeSelect options={variableTree}>
          <Select open={false} />
        </VariableTreeSelect>
      )}
    </>
  );
};

export default memo(ModelConfigForm);
