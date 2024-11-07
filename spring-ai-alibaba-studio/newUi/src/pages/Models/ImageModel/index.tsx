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
