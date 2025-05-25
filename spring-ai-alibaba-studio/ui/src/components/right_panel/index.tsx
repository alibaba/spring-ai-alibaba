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
import Config from './config';
import Prompt from './prompt';
import Tool from './tool';
import type { TabsProps } from 'antd';
import styles from './index.module.css';
import { RightPanelValues } from './types';
import { ChatOptions, ImageOptions } from '@/types/options';
import { ModelType } from '@/types/chat_model';
import { useEffect, useState } from 'react';

type Props = {
  tabs: string[];
  modelType: ModelType;
  initialValues: RightPanelValues;
  onChangeConfig: (cfg: ChatOptions | ImageOptions) => void;
  onChangePrompt: (prompt: string) => void;
};

// 右侧面板组件
export default function Setup(props: Props) {
  const { modelType, tabs } = props;
  const { initialChatConfig, initialImgConfig, initialTool } =
    props.initialValues;

  const [items, setItems] = useState<TabsProps['items']>();

  const allTabs = [
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
  ];

  useEffect(() => {
    setItems(
      allTabs.filter((tab) => {
        return tabs.includes(tab.key);
      }),
    );
  }, [props.initialValues]);
  return (
    <div className={styles.container}>
      <Tabs defaultActiveKey="config" items={items} />
    </div>
  );
}
