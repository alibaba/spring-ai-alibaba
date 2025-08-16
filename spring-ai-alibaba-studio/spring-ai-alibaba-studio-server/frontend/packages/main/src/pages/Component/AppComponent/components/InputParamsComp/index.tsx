import { TopExpandBtn } from '@/components/ExpandBtn';
import $i18n from '@/i18n';
import { HelpIcon } from '@spark-ai/design';
import { IValueType } from '@spark-ai/flow';
import { Flex, Input, Select } from 'antd';
import classNames from 'classnames';
import { useState } from 'react';
import styles from './index.module.less';

export interface IParamItem {
  field: string;
  type: IValueType;
  description: string;
  default_value: any;
  alias: string;
  required: boolean;
  display: boolean;
  source: 'biz' | 'model';
}

export interface IUserParamItem {
  code: string;
  name: string;
  params: IParamItem[];
}

export interface IOutputParamItem {
  field: string;
  type: IValueType;
  description: string;
}

export interface IConfigInput {
  user_params: IUserParamItem[];
  system_params: IParamItem[];
}

interface IProps {
  input: IConfigInput;
  onChange: (val: Partial<IConfigInput>) => void;
  disabled?: boolean;
}

interface IInputCompItemProps {
  params: IParamItem[];
  name: string;
  onChange: (val: IParamItem[]) => void;
  disabled?: boolean;
}

export function InputCompItem(props: IInputCompItemProps) {
  const [expand, setExpand] = useState(true);
  const changeRowItem = (payload: Partial<IParamItem>, ind: number) => {
    props.onChange(
      props.params.map((item, index) => ({
        ...item,
        ...(ind === index ? payload : {}),
      })),
    );
  };
  return (
    <div className="flex flex-col gap-3">
      <Flex className={styles['title-wrap']} gap={8}>
        <span className={styles.title}>{props.name}</span>
        <TopExpandBtn setExpand={setExpand} expand={expand} />
      </Flex>
      {expand && (
        <Flex className={styles['inputs-form']} vertical gap={8}>
          <Flex className={styles['key-label']} gap={8}>
            <span className={styles.name}>
              {$i18n.get({
                id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.parameterName',
                dm: '参数名称',
              })}
            </span>
            <span className={classNames(styles.alias, 'flex items-center')}>
              <span>
                {$i18n.get({
                  id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.alias',
                  dm: '别名',
                })}
              </span>
              <HelpIcon
                content={$i18n.get({
                  id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.youNeedToDefineAliasForComponentInputParameters',
                  dm: '您需要给组件的入参定义别名，用户在引用组件的时候，只会看到别名，不会看到实际的参数名称',
                })}
              />
              {!props.disabled && <img src="/images/require.svg" alt="" />}
            </span>
            <span className={styles.desc}>
              {$i18n.get({
                id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.parameterDescription',
                dm: '参数描述',
              })}
            </span>
            <span className={styles.type}>
              {$i18n.get({
                id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.type',
                dm: '类型',
              })}
            </span>
            <span className={styles.required}>
              {$i18n.get({
                id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.isRequired',
                dm: '是否必填',
              })}
            </span>
            <span className={styles.enable}>
              {$i18n.get({
                id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.isVisible',
                dm: '是否可见',
              })}
            </span>
            <span className={styles.source}>
              {$i18n.get({
                id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.passingMethod',
                dm: '传参方式',
              })}
            </span>
            <span className={styles['default-value']}>
              {$i18n.get({
                id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.defaultValue',
                dm: '默认值',
              })}
            </span>
          </Flex>
          {props.params.map((item, index) => (
            <Flex
              key={`${item.field}_${index}`}
              className={styles['form-row-item']}
              gap={8}
              align="center"
            >
              <Input value={item.field} className={styles.name} disabled />
              <Input
                onChange={(e) =>
                  changeRowItem({ alias: e.target.value }, index)
                }
                value={item.alias}
                className={styles.alias}
                placeholder={$i18n.get({
                  id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.enterParameterAlias',
                  dm: '请输入参数别名',
                })}
                disabled={props.disabled}
              />

              <Input
                onChange={(e) =>
                  changeRowItem({ description: e.target.value }, index)
                }
                value={item.description}
                className={styles.desc}
                disabled={props.disabled}
                placeholder={
                  props.disabled
                    ? ''
                    : $i18n.get({
                        id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.enterParameterDescription',
                        dm: '请输入参数描述',
                      })
                }
              />

              <Input value={item.type} className={styles.type} disabled />
              <Select
                value={item.required}
                className={styles.required}
                disabled={props.disabled}
                options={[
                  {
                    label: $i18n.get({
                      id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.yes',
                      dm: '是',
                    }),
                    value: true,
                  },
                  {
                    label: $i18n.get({
                      id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.no',
                      dm: '否',
                    }),
                    value: false,
                  },
                ]}
                onChange={(val) => changeRowItem({ required: val }, index)}
              />

              <Select
                value={item.display}
                className={styles.enable}
                options={[
                  {
                    label: $i18n.get({
                      id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.yes',
                      dm: '是',
                    }),
                    value: true,
                  },
                  {
                    label: $i18n.get({
                      id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.no',
                      dm: '否',
                    }),
                    value: false,
                  },
                ]}
                disabled={props.disabled}
                onChange={(val) => changeRowItem({ display: val }, index)}
              />

              <Select
                value={item.source}
                className={styles.source}
                disabled={props.disabled}
                onChange={(val) => changeRowItem({ source: val }, index)}
                options={[
                  {
                    label: $i18n.get({
                      id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.businessPassThrough',
                      dm: '业务透传',
                    }),
                    value: 'biz',
                  },
                  {
                    label: $i18n.get({
                      id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.modelRecognition',
                      dm: '模型识别',
                    }),
                    value: 'model',
                  },
                ]}
              />

              <Input
                onChange={(e) =>
                  changeRowItem({ default_value: e.target.value }, index)
                }
                value={item.default_value}
                className={styles['default-value']}
                disabled={props.disabled}
                placeholder={
                  props.disabled
                    ? ''
                    : $i18n.get({
                        id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.inputDefaultValue',
                        dm: '输入默认值',
                      })
                }
              />
            </Flex>
          ))}
        </Flex>
      )}
    </div>
  );
}

export default function InputParamsComp(props: IProps) {
  const handleChangeUserParams = (params: IParamItem[], code: string) => {
    const newUserParams = props.input.user_params.map((item) => {
      if (item.code === code)
        return {
          ...item,
          params,
        };
      return item;
    });
    props.onChange({
      user_params: newUserParams,
    });
  };

  return (
    <Flex vertical gap={20}>
      {props.input.user_params.map((item) => (
        <InputCompItem
          onChange={(val) => handleChangeUserParams(val, item.code)}
          name={item.name}
          key={item.code}
          params={item.params}
          disabled={props.disabled}
        />
      ))}
      <InputCompItem
        name={$i18n.get({
          id: 'main.pages.Component.AppComponent.components.InputParamsComp.index.systemParameter',
          dm: '系统参数',
        })}
        onChange={(val) => props.onChange({ system_params: val })}
        params={props.input.system_params}
        disabled={props.disabled}
      />
    </Flex>
  );
}
