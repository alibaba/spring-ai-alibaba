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

import { Card, Flex, Button, Checkbox } from 'antd';
import Setup from '../Setup';

export default function ImageModel() {
  return (
    <Flex justify="space-between">
      <Flex vertical justify="space-between">
        <div>
          <Card title="{{ 模型 Bean 名称 }}" style={{ width: 300 }}>
            <p>Card content</p>
            <p>Card content</p>
            <p>Card content</p>
          </Card>
          <Card title="图片生成结果" style={{ width: 300 }}>
            <p>Card content</p>
            <p>Card content</p>
            <p>Card content</p>
          </Card>
        </div>
        <Flex align="center" justify="space-around">
          <Button>清空</Button>
          <Checkbox>聊天模式</Checkbox>
          <Button>运行</Button>
        </Flex>
      </Flex>
      <Setup />
    </Flex>
  );
}
