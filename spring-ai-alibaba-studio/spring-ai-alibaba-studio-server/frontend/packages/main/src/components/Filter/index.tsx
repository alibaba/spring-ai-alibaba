import { Flex, Tag } from 'antd';
import styles from './index.module.less';

export interface IFilterProps {
  options: {
    value: string;
    label: string;
  }[];
  value: string;
  onSelect: (value: string, item: any) => void;
}

export default function Filter({ options, value, onSelect }: IFilterProps) {
  return (
    <Flex wrap className={styles['filter-selector']} gap={8}>
      {options.map((item) => {
        const isActive = item.value === value;
        return (
          <Tag.CheckableTag
            checked={isActive}
            onClick={() => {
              if (isActive) return;
              onSelect(item.value, item);
            }}
            key={item.value}
          >
            {item.label}
          </Tag.CheckableTag>
        );
      })}
    </Flex>
  );
}
