import $i18n from '@/i18n';
import {
  getModelParameterRules,
  getModelSelector,
} from '@/services/modelService';
import {
  IModel,
  IModelConfigParamItem,
  IModelParameterRule,
  IModelSelectorItem,
  ISelectedModelParams,
} from '@/types/modelService';
import { Button, IconFont, Slider } from '@spark-ai/design';
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
import { memo, useEffect, useMemo, useState } from 'react';
import styles from './index.module.less';

// Get slider step value based on precision
const getStepFromPrecision = (precision?: number): number => {
  if (precision === undefined) return 1;

  // Fix floating point precision issues
  const step = Math.pow(0.1, precision);

  // Convert to string then parse back to number to avoid floating point calculation errors
  return Number(step.toFixed(precision));
};

type ISelectModel = IModelSelectorItem['models'][0];

interface IModelSelectorProps {
  value: ISelectedModelParams;
  onSelect: (model: ISelectModel, originalModel: IModel) => void;
  onChange: (payload: ISelectedModelParams['params']) => void;
  modelOptions?: IModelOption[];
  disabled?: boolean;
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
  value: Array<IModelConfigParamItem>; //
  onChange: (params: Array<IModelConfigParamItem>) => void;
  schemaList: IModelParameterRule[];
  disabled?: boolean;
}

const ModelConfigBtn = memo(
  ({ value, onChange, schemaList, disabled }: IModelConfigBtnProps) => {
    const changeParamItem = (
      key: string,
      payload: Partial<IModelConfigParamItem>,
    ) => {
      const newParams = schemaList.map((item) => {
        const matchedValue = value.find((vItem) => vItem.key === item.code);
        if (item.code === key) {
          return {
            key: item?.code,
            type: item.type,
            value: item.default_value,
            enable: matchedValue?.enable,
            ...payload,
          };
        }
        return {
          key: item?.code,
          type: item.type,
          value: matchedValue?.value || item.default_value,
          enable: matchedValue?.enable || false,
        };
      });
      // @ts-ignore

      onChange(newParams);
    };
    const memoForm = useMemo(() => {
      if (!schemaList?.length)
        return $i18n.get({
          id: 'main.components.ModelConfigForm.index.selectModelFirst',
          dm: '请先选择模型',
        });
      return (
        <div className="flex flex-col gap-3">
          {schemaList.map((item) => {
            const targetValue = value.find((vItem) => vItem.key === item.code);
            return (
              <div key={item.code} className="flex-justify-between gap-5">
                <div className="flex items-center gap-2">
                  <Checkbox
                    checked={!!targetValue?.enable}
                    disabled={disabled}
                    onChange={(e) =>
                      changeParamItem(item.code, { enable: e.target.checked })
                    }
                  />

                  <Typography.Text
                    className={styles['model-selector-form-label']}
                    ellipsis={{ tooltip: item.name }}
                    style={{ width: 78 }}
                  >
                    {item.name}
                  </Typography.Text>
                </div>
                {item.type === 'Number' && (
                  <div className="flex items-center gap-3 flex-1">
                    <Slider
                      disabled={!targetValue?.enable || disabled}
                      style={{ width: 236 }}
                      onChange={(val) =>
                        changeParamItem(item.code, { value: val })
                      }
                      min={item.min}
                      max={item.max}
                      step={getStepFromPrecision(item.precision)}
                      value={
                        (targetValue?.value as number) || item.default_value
                      }
                    />

                    <InputNumber
                      disabled={!targetValue?.enable || disabled}
                      style={{ width: 70 }}
                      precision={item.precision}
                      value={
                        (targetValue?.value as number) || item.default_value
                      }
                      onChange={(val) =>
                        changeParamItem(item.code, { value: val as number })
                      }
                    />
                  </div>
                )}
                {item.type === 'String' &&
                  (item.options ? (
                    <Select
                      value={targetValue?.value || item.options?.[0]}
                      disabled={!targetValue?.enable || disabled}
                      className="w-full"
                      options={item.options.map((option) => ({
                        label: option,
                        value: option,
                      }))}
                      onSelect={(value) =>
                        changeParamItem(item.code, { value: value })
                      }
                    ></Select>
                  ) : (
                    <Input
                      disabled={!targetValue?.enable || disabled}
                      className="flex-1"
                      value={
                        (targetValue?.value as string) || item.default_value
                      }
                      onChange={(e) =>
                        changeParamItem(item.code, { value: e.target.value })
                      }
                    />
                  ))}
                {item.type === 'Boolean' && (
                  <Switch
                    disabled={!targetValue?.enable || disabled}
                    // @ts-ignore
                    checked={
                      (targetValue?.value as boolean) || item.default_value
                    }
                    onChange={(val) =>
                      changeParamItem(item.code, { value: val })
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
  ({
    onChange,
    onSelect,
    value,
    modelOptions = [],
    disabled,
  }: IModelSelectorProps) => {
    const [state, setState] = useSetState({
      schemaList: [] as IModelParameterRule[],
    });

    const initModelParams = (modelId: string, provider?: string) => {
      if ((!value.provider?.length && !provider?.length) || !modelId?.length)
        return;
      getModelParameterRules(provider || value.provider, modelId).then(
        (res) => {
          setState({
            schemaList: res.data,
          });
        },
      );
    };

    useMount(() => {
      if (value.model_id) initModelParams(value.model_id);
    });

    return (
      <div className="flex items-center gap-2">
        <Select
          disabled={disabled}
          placeholder={$i18n.get({
            id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.selectModel',
            dm: '请选择模型',
          })}
          className="flex-1 w-[240px]"
          value={!value.model_id ? void 0 : value.model_id}
          options={modelOptions}
          onChange={(value, option: any) => {
            const [provider, modelId] = value.split('@@@');
            initModelParams(modelId, provider);
            if (Array.isArray(option)) {
              onSelect(
                option[0]?.extra as ISelectModel,
                option[0]?.extra as IModel,
              );
            } else {
              onSelect(option?.extra as ISelectModel, option?.extra as IModel);
            }
          }}
        />

        <ModelConfigBtn
          disabled={disabled}
          schemaList={state.schemaList}
          value={value.params}
          onChange={(params) => {
            onChange(params);
          }}
        />
      </div>
    );
  },
);

const ModelConfigForm = ({
  value,
  onChange,
  onSelectedModelChange,
  disabled,
}: {
  value: ISelectedModelParams;
  onChange: (
    modelConfig: Partial<ISelectedModelParams>,
    originalModel: IModel,
  ) => void; // When model selection or parameters change, return model config and complete model config to parent component
  onSelectedModelChange?: (model: IModel) => void; // When component initializes or model selection changes, return complete model config to parent component
  disabled?: boolean;
}) => {
  const [selectedModel, setSelectedModel] = useState<IModel | null>(null);
  const [state, setState] = useSetState({ modelOptions: [] as IModelOption[] });

  useEffect(() => {
    if (selectedModel) {
      onSelectedModelChange?.(selectedModel);
    }
  }, [selectedModel]);
  useMount(() => {
    getModelSelector('llm').then((res) => {
      const modelOptions = res.data.map((item) => ({
        label: item.provider.name,
        options: item.models.map((model) => ({
          label: model.name || '',
          value: `${model.provider}@@@${model.model_id}`,
          extra: model,
        })),
      }));
      setState({
        modelOptions,
      });
      modelOptions.forEach((item) => {
        item.options.forEach((option) => {
          if (option.value.split('@@@')[1] === value.model_id) {
            setSelectedModel(option.extra);
          }
        });
      });
    });
  });
  return (
    <>
      <ModelSelector
        disabled={disabled}
        onSelect={(model, originalModel) => {
          setSelectedModel(originalModel);
          onChange(
            {
              model_id: model.model_id,
              provider: model.provider,
              params: [],
            },
            originalModel,
          );
        }}
        onChange={(config) => {
          onChange(
            {
              model_id: selectedModel?.model_id,
              provider: selectedModel?.provider,
              params: config,
            },
            selectedModel!,
          );
        }}
        value={value}
        modelOptions={state.modelOptions}
      />
    </>
  );
};

export default memo(ModelConfigForm);
