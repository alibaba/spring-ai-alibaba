import { getModelSelector } from '@/services/modelService';
import { Select } from '@spark-ai/design';
import React, { useEffect, useState } from 'react';
import styles from './index.module.less';

interface SelectOption {
  options?: SelectOption[];
  label: string | JSX.Element;
  value?: string;
  extra?: any;
}

interface ModelSelectorProps {
  value?: string;
  modelType: string;
  onChange: (val: string, option?: SelectOption | SelectOption[]) => void;
}

const ModelSelector: React.FC<ModelSelectorProps> = ({
  value,
  modelType,
  onChange,
}) => {
  const [options, setOptions] = useState<SelectOption[]>([]);

  useEffect(() => {
    getModelSelector(modelType).then((res) => {
      const modelList: SelectOption[] = res.data.map((item) => ({
        label: item.provider.name,
        options: item.models.map((model) => ({
          label: (
            <div className={styles['opt-item']}>
              <img src="/images/asset.svg" alt="" width={20} height={20} />
              <span className={styles['opt-title']}>{model.name}</span>
            </div>
          ),
          value: `${model.provider}@@@${model.model_id}`,
          extra: model,
        })),
      }));
      setOptions(modelList);
    });
  }, [modelType]);

  return <Select value={value} onChange={onChange} options={options} />;
};

export default ModelSelector;
