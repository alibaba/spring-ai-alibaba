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

import { Card, Flex, Button, Checkbox, Input, Image } from 'antd';
import Setup from '../Setup';

export default function ImageModel() {
  const initialValues = {
    initialConfig: {
      model: 'ollama/llama3.2',
      temperature: 50,
      topP: 50,
      topK: 50,
      maxTokens: 10,
      sequences: '',
      version: 1,
    },
    initialTool: {},
  };

  const { TextArea } = Input;

  return (
    <Flex justify="space-between">
      <Flex vertical justify="space-between" style={{ width: 500 }}>
        <div>
          <Card title="模型 Bean 名称">
            <TextArea autoSize={{ minRows: 3 }} />
          </Card>
          <Card title="图片生成结果" style={{ marginTop: 20 }}>
            <Flex align="flex-end">
              <Image
                width={200}
                src="https://zos.alipayobjects.com/rmsportal/jkjgkEfvpUPVyRjUImniVslZfWPnJuuZ.png"
              />
              <Button style={{ marginLeft: 20 }}>下载</Button>
            </Flex>
          </Card>
        </div>
        <Flex align="center" justify="space-around">
          <Button>清空</Button>
          <Checkbox>聊天模式</Checkbox>
          <Button>运行</Button>
        </Flex>
      </Flex>
      <Setup initialValues={initialValues} />
    </Flex>
  );
}
