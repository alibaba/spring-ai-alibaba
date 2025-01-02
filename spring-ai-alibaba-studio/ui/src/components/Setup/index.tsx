/**
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Tabs } from 'antd';
import Config from './Config';
import Prompt from './Prompt';
import Tool from './Tool';
import type { TabsProps } from 'antd';
import styles from './index.module.css';
import { RightPanelValues } from '../../pages/run/models/types';
import { ChatOptions, ImageOptions } from '@/types/options';
import { ModelType } from '@/types/chat_model';
import { useEffect, useState } from 'react';

type Props = {
  modelType: ModelType;
  initialValues: RightPanelValues;
  onChangeConfig: (cfg: ChatOptions | ImageOptions) => void;
  onChangePrompt: (prompt: string) => void;
};

export default function Setup(props: Props) {
  const { modelType } = props;
  const { initialChatConfig, initialImgConfig, initialTool } =
    props.initialValues;

  const [items, setItems] = useState<TabsProps['items']>();
  useEffect(() => {
    setItems([
      {
        key: 'config',
        label: '配置',
        children: (
          <Config
            modelType={modelType}
            onChangeConfig={props.onChangeConfig}
            configFromAPI={
              modelType == ModelType.CHAT ? initialChatConfig : initialImgConfig
            }
          />
        ),
      },
      {
        key: 'prompt',
        label: '提示词',
        children: <Prompt onchangePrompt={props.onChangePrompt} />,
      },
      {
        key: 'tool',
        label: '工具',
        children: <Tool initialTool={initialTool} />,
      },
    ]);
  }, [initialChatConfig, initialImgConfig]);
  return (
    <div className={styles.container}>
      <Tabs defaultActiveKey="config" items={items} />
    </div>
  );
}
