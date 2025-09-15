import $i18n from '@/i18n';
import { IValueType } from '@spark-ai/flow';
import { Flex, Input } from 'antd';
import styles from './index.module.less';

export interface IOutputParamItem {
  field: string;
  type: IValueType;
  description: string;
}

interface IProps {
  output: IOutputParamItem[];
}

export default function OutputParamsComp(props: IProps) {
  return (
    <Flex vertical gap={8}>
      <Flex className={styles['key-label']} gap={8}>
        <span className="flex-1">
          {$i18n.get({
            id: 'main.pages.Component.AppComponent.components.OutputParamsComp.index.parameterName',
            dm: '参数名称',
          })}
        </span>
        <span className="flex-1">
          {$i18n.get({
            id: 'main.pages.Component.AppComponent.components.OutputParamsComp.index.parameterType',
            dm: '参数类型',
          })}
        </span>
      </Flex>
      {props.output.map((item, index) => (
        <Flex gap={8} key={index}>
          <Input className="flex-1" value={item.field} disabled />
          <Input className="flex-1" value={item.type} disabled />
        </Flex>
      ))}
    </Flex>
  );
}
