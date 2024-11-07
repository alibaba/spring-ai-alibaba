import { Tabs } from 'antd';
import Config from './Config';
import Tool from './Tool';
import type { TabsProps } from 'antd';
import styles from './index.module.css';

export default function Setup() {
  const items: TabsProps['items'] = [
    {
      key: 'config',
      label: '配置',
      children: <Config />,
    },
    {
      key: '2',
      label: '工具',
      children: <Tool />,
    },
  ];
  return (
    <div className={styles.container}>
      <Tabs defaultActiveKey="1" items={items} />
    </div>
  );
}
