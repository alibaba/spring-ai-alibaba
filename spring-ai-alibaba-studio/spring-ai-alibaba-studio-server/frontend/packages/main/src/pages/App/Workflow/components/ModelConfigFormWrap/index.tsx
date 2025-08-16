import ModelConfigForm from '@/components/ModelConfigForm';
import $i18n from '@/i18n';
import { IModel } from '@/types/modelService';
import { IVarTreeItem, VariableSelector } from '@spark-ai/flow';
import { Flex, Switch } from 'antd';
import { memo, useMemo, useState } from 'react';
import { SELECTED_MODEL_PARAMS_DEFAULT } from '../../constant';
import { ISelectedModelParams } from '../../types';
import InfoIcon from '../InfoIcon';

export interface IModelConfigFormWrapProps {
  value: ISelectedModelParams;
  onChange: (
    payload: ISelectedModelParams,
    options?: { isSupportReasoning: boolean; isSupportVision: boolean },
  ) => void;
  variableList: IVarTreeItem[];
  disabled?: boolean;
}

export default memo(function ModelConfigFormWrap(
  props: IModelConfigFormWrapProps,
) {
  const [model, setModel] = useState<IModel>();

  const enableVision = useMemo(() => {
    return model?.tags?.includes('vision');
  }, [model]);

  return (
    <>
      <ModelConfigForm
        onChange={(payload) => {
          props.onChange({
            ...props.value,
            ...payload,
          });
        }}
        disabled={props.disabled}
        value={{
          model_id: props.value.model_id,
          provider: props.value.provider,
          params: props.value.params,
        }}
        onSelectedModelChange={(val) => {
          const isSupportVision = val.tags?.includes('vision') || false;
          const visionConfig =
            props.value.vision_config ||
            SELECTED_MODEL_PARAMS_DEFAULT.vision_config;

          props.onChange(
            {
              ...props.value,
              vision_config: {
                ...visionConfig,
                enable: !isSupportVision ? false : visionConfig.enable,
              },
            },
            {
              isSupportReasoning: val.tags?.includes('reasoning') || false,
              isSupportVision: isSupportVision,
            },
          );
          setModel(val);
        }}
      />

      {enableVision && (
        <>
          <Flex justify="space-between" align="center">
            <div className="spark-flow-panel-form-title">
              <span>
                {$i18n.get({
                  id: 'main.pages.App.Workflow.components.ModelConfigFormWrap.index.visual',
                  dm: '视觉',
                })}
              </span>
              <InfoIcon
                tip={$i18n.get({
                  id: 'main.pages.App.Workflow.components.ModelConfigFormWrap.index.inputContentForIntent',
                  dm: '输入需要用做意图判断的内容。',
                })}
              />
            </div>
            <Switch
              disabled={props.disabled}
              onChange={(val) => {
                props.onChange({
                  ...props.value,
                  vision_config: {
                    ...props.value.vision_config,
                    enable: val,
                  },
                });
              }}
              checked={props.value.vision_config?.enable}
            />
          </Flex>
          {!!props.value.vision_config?.enable && (
            <VariableSelector
              disabled={props.disabled}
              prefix="File"
              value={props.value.vision_config?.params[0] || {}}
              onChange={(val) => {
                props.onChange({
                  ...props.value,
                  vision_config: {
                    ...props.value.vision_config,
                    params: [
                      {
                        ...(props.value.vision_config?.params[0] || {}),
                        ...val,
                      },
                    ],
                  },
                });
              }}
              variableList={props.variableList}
            />
          )}
        </>
      )}
    </>
  );
});
