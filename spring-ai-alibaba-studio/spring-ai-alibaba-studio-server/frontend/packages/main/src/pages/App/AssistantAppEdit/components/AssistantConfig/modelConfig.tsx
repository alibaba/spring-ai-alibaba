import ModelConfigForm from '@/components/ModelConfigForm';
import { SELECTED_MODEL_PARAMS_DEFAULT } from '@/pages/App/Workflow/constant';
import { IAssistantAppDetailWithInfos } from '@/types/appManage';
import { IModel, ISelectedModelParams } from '@/types/modelService';
import { useContext } from 'react';
import { AssistantAppContext } from '../../AssistantAppContext';

const convertAppConfigToModelConfig = (
  appConfig?: IAssistantAppDetailWithInfos['config'],
): ISelectedModelParams => {
  if (!appConfig) return SELECTED_MODEL_PARAMS_DEFAULT;
  return {
    model_id: appConfig.model?.model_id || '',
    provider: appConfig.model_provider || '',
    params: appConfig.parameter
      ? Object.keys(appConfig.parameter).map((key) => {
          return {
            key,
            type: 'Number',
            value: appConfig.parameter?.[
              key as keyof typeof appConfig.parameter
            ] as number,
            enable: true,
          };
        })
      : [],
  };
};

const convertModelConfigToAppConfig = (
  modelConfig: Partial<ISelectedModelParams>,
  originalModel: IModel,
): Partial<IAssistantAppDetailWithInfos['config']> => {
  if (!modelConfig.model_id || !modelConfig.provider) return {};
  return {
    model: originalModel,
    model_provider: modelConfig.provider,
    parameter: modelConfig.params
      ? modelConfig.params
          .filter((item) => item.enable)
          .reduce((acc: Record<string, any>, curr) => {
            acc[curr.key] = curr.value;
            return acc;
          }, {})
      : new Object(),
  };
};
export default () => {
  const { appState, onAppConfigChange } = useContext(AssistantAppContext);
  const { appBasicConfig } = appState;

  return (
    <ModelConfigForm
      value={convertAppConfigToModelConfig(appBasicConfig?.config)}
      onChange={(
        modelConfig: Partial<ISelectedModelParams>,
        originalModel: IModel,
      ) => {
        onAppConfigChange(
          convertModelConfigToAppConfig(modelConfig, originalModel),
        );
      }}
    />
  );
};
