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
import Tool from './Tool';
import type { TabsProps } from 'antd';
import styles from './index.module.css';
import { InitialTool } from '../types';
import { ChatOptions, ImageOptions } from '@/types/options';
import { RightPanelValues } from '../types';

type Props = {
  initialValues: RightPanelValues
};

export default function Setup(props: Props) {
  const { initialChatConfig, initialImgConfig, initialTool } = props.initialValues;

  const defaultChatCfgs: ChatOptions = {
    model: 'qwen-plus',
    temperature: 0.85,
    top_p: 0.8,
    seed: 1,
    enable_search: false,
    top_k: 0,
    stop: [],
    incremental_output: false,
    repetition_penalty: 1.1,
    tools: [],
  }

  const defaultImgCfgs: ImageOptions = {
    model: 'wanx-v1',
    responseFormat: '',
    n: 0,
    size_width: 0,
    size_height: 0,
    size: '',
    style: '',
    seed: 0,
    ref_img: '',
    ref_strength: 0,
    ref_mode: '',
    negative_prompt: ''
  }

  const items: TabsProps['items'] = [
    {
      key: 'config',
      label: '配置',
      children: <Config initialConfig={initialChatConfig || defaultChatCfgs} />,
    },
    {
      key: '2',
      label: '工具',
      children: <Tool initialTool={initialTool} />,
    },
  ];
  return (
    <div className={styles.container}>
      <Tabs defaultActiveKey="1" items={items} />
    </div>
  );
}
