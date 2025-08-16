import $i18n from '@/i18n';
import { Button, IconButton, Input, Select } from '@spark-ai/design';
import { Flex } from 'antd';
import { useMemo, useState } from 'react';
import { RightExpandBtn } from '../ExpandBtn';
import styles from './index.module.less';

export type IValueType =
  | 'Array<Object>'
  | 'Array<String>'
  | 'Array<Number>'
  | 'Array<Boolean>'
  | 'Object'
  | 'String'
  | 'Number'
  | 'Boolean';

export interface InputParamItem {
  key?: string;
  type?: IValueType;
  desc?: string;
  location?: string;
  default_value?: string;
  required?: boolean;
  properties?: Array<InputParamItem>;
  user_input?: boolean;
}

export const VALUE_TYPE_OPTS = [
  'Array<Object>',
  'Array<String>',
  'Array<Number>',
  'Array<Boolean>',
  'Object',
  'String',
  'Number',
  'Boolean',
].map((item) => ({ label: item, value: item }));

interface IProps {
  params: InputParamItem[];
  onChange: (val: InputParamItem[]) => void;
  requestMethod: string;
}

interface IItemProps {
  paramItem: InputParamItem;
  onChange: (val: InputParamItem) => void;
  showLine?: boolean;
  order?: number;
  handleRemove: () => void;
  list: InputParamItem[];
  index: number;
  hasProperties?: boolean;
  requestMethod: string;
  disableShowLocation?: boolean;
}

const DEFAULT_ROW_DATA: InputParamItem = {
  key: void 0,
  type: 'String',
  desc: void 0,
  location: 'Query',
  default_value: void 0,
  required: true,
};

function InputParamsRowComp({
  paramItem,
  onChange,
  order = 0,
  handleRemove,
  list,
  index: curIndex,
  hasProperties: bizHasProperties,
  requestMethod,
  disableShowLocation = false,
}: IItemProps) {
  const [expand, setExpand] = useState(true);
  const [errorTip, setErrorTip] = useState('');
  const [descErrorTip, setDescErrorTip] = useState('');
  const [fresh, setFresh] = useState(0);
  const hasProperties = useMemo(() => {
    return ['Object', 'Array<Object>'].includes(paramItem.type || '');
  }, [paramItem.type]);

  const hasChildren = useMemo(() => {
    return hasProperties && !!paramItem.properties?.length;
  }, [paramItem, hasProperties]);

  const changeRowItem = (payload: Partial<InputParamItem>) => {
    onChange({
      ...paramItem,
      ...payload,
    });
  };

  const handleChange = (newPropertyItem: InputParamItem, tIndex: number) => {
    changeRowItem({
      properties: (paramItem.properties || [])?.map((item, index) => {
        if (index === tIndex) return { ...item, ...newPropertyItem };
        return item;
      }),
    });
  };

  const handleAddRowItem = () => {
    changeRowItem({
      properties: [...(paramItem.properties || []), DEFAULT_ROW_DATA],
    });
  };

  const deleteItem = (ind: number) => {
    changeRowItem({
      properties: paramItem.properties?.filter((_, index) => index !== ind),
    });
    const timer = setTimeout(() => {
      setFresh(new Date().valueOf());
      clearTimeout(timer);
    }, 100);
  };

  const handleCheckKey = () => {
    if (!paramItem.key?.trim()) {
      setErrorTip(
        $i18n.get({
          id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.parameterNameCannotBeEmpty',
          dm: '参数名不能为空',
        }),
      );
      return;
    }
    if (!/^[a-zA-Z_][a-zA-Z0-9_-]*$/.test(paramItem.key)) {
      setErrorTip(
        $i18n.get({
          id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.parameterNameOnlyLettersNumbersOrUnderscoresAndStartWithLetterOrUnderscore',
          dm: '参数名称只能包含字母、数字或下划线，并且以字母或下划线开头',
        }),
      );
      return;
    }
    if (
      list.some((item, index) => {
        if (index === curIndex) return false;
        return item.key === paramItem.key;
      })
    ) {
      setErrorTip(
        $i18n.get({
          id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.parameterNameCannotBeDuplicate',
          dm: '参数名称不能重复',
        }),
      );
      return;
    }
    setErrorTip('');
  };

  const handleCheckDesc = () => {
    if (!paramItem.desc?.trim()) {
      setDescErrorTip(
        $i18n.get({
          id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.parameterExplanationCannotBeEmpty',
          dm: '参数解释不能为空',
        }),
      );
      return;
    }
    setDescErrorTip('');
  };

  return (
    <>
      <div>
        <Flex className={styles.item} gap={8} align="flex-start">
          {hasChildren ? (
            <RightExpandBtn expand={expand} setExpand={setExpand} />
          ) : (
            bizHasProperties && <div className={styles.placeholder}></div>
          )}
          <Flex style={{ flex: 1, width: 100 }} vertical>
            <Input
              status={errorTip ? 'error' : void 0}
              onBlur={handleCheckKey}
              onChange={(e) => {
                changeRowItem({ key: e.target.value });
              }}
              value={paramItem.key}
              placeholder={$i18n.get({
                id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.enterParameterName',
                dm: '请输入参数名称',
              })}
            />

            {errorTip && (
              <span className={styles['error-tip']}>{errorTip}</span>
            )}
          </Flex>

          <Flex style={{ width: 150 }} vertical>
            <Input
              onChange={(e) => changeRowItem({ desc: e.target.value })}
              value={paramItem.desc}
              status={descErrorTip ? 'error' : void 0}
              onBlur={handleCheckDesc}
              placeholder={$i18n.get({
                id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.enterParameterExplanation',
                dm: '请输入参数解释',
              })}
            />

            {descErrorTip && (
              <span className={styles['error-tip']}>{descErrorTip}</span>
            )}
          </Flex>
          <Select
            onChange={(val) => changeRowItem({ type: val })}
            options={VALUE_TYPE_OPTS}
            value={paramItem.type}
            style={{ width: !disableShowLocation ? 140 : 248 }}
            placeholder={$i18n.get({
              id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.selectType',
              dm: '选择类型',
            })}
          />

          {!disableShowLocation && (
            <Select
              options={[
                ...(requestMethod === 'GET'
                  ? []
                  : [{ label: 'Body', value: 'Body' }]),
                { label: 'Path', value: 'Path' },
                { label: 'Query', value: 'Query' },
                { label: 'Header', value: 'Header' },
              ]}
              value={paramItem.location}
              onChange={(val) => changeRowItem({ location: val })}
              style={{ width: 100 }}
              placeholder={$i18n.get({
                id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.selectInputMethod',
                dm: '选择传入方法',
              })}
            />
          )}
          <Select
            style={{ width: 100 }}
            value={paramItem.required ? 1 : 0}
            onChange={(val) => changeRowItem({ required: val === 1 })}
            options={[
              {
                label: $i18n.get({
                  id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.required',
                  dm: '必填',
                }),
                value: 1,
              },
              {
                label: $i18n.get({
                  id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.notRequired',
                  dm: '非必填',
                }),
                value: 0,
              },
            ]}
          />

          <Select
            style={{ width: 120 }}
            value={paramItem.user_input ? 1 : 0}
            onChange={(val) => changeRowItem({ user_input: val === 1 })}
            options={[
              {
                label: $i18n.get({
                  id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.largeModelRecognition',
                  dm: '大模型识别',
                }),
                value: 0,
              },
              {
                label: $i18n.get({
                  id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.businessPassThrough',
                  dm: '业务透传',
                }),
                value: 1,
              },
            ]}
          />

          <Flex gap={8}>
            {hasProperties && (
              <IconButton
                onClick={handleAddRowItem}
                icon="spark-plus-line"
                shape="circle"
              />
            )}
            <IconButton
              onClick={handleRemove}
              icon="spark-delete-line"
              bordered={false}
              shape="circle"
            />
          </Flex>
        </Flex>
      </div>
      {hasProperties &&
        expand &&
        paramItem.properties?.map((item, index) => (
          <InputParamsRowComp
            index={index}
            order={order + 1}
            list={paramItem.properties || []}
            handleRemove={() => deleteItem(index)}
            onChange={(payload) => handleChange(payload, index)}
            key={`${fresh}_${index}`}
            paramItem={item}
            showLine
            requestMethod={requestMethod}
            disableShowLocation
          />
        ))}
    </>
  );
}

export default function InputParamsConfig({
  params,
  onChange,
  requestMethod,
}: IProps) {
  const [fresh, setFresh] = useState(0);
  const handleChangeItem = (item: InputParamItem, index: number) => {
    onChange(
      params.map((vItem, vIndex) => {
        if (vIndex === index) return item;
        return vItem;
      }),
    );
  };

  const addRow = () => {
    onChange([...params, DEFAULT_ROW_DATA]);
  };

  const deleteItem = (ind: number) => {
    onChange(params.filter((_, index) => index !== ind));
    const timer = setTimeout(() => {
      setFresh(new Date().valueOf());
      clearTimeout(timer);
    }, 100);
  };

  const hasProperties = useMemo(() => {
    return params.some((item) => !!item.properties?.length);
  }, [params]);

  return (
    <Flex vertical gap={12}>
      <Flex gap={8} className={styles.label}>
        {hasProperties && <div className={styles.placeholder}></div>}
        <div className={styles.label} style={{ flex: 1 }}>
          {$i18n.get({
            id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.parameterName',
            dm: '参数名称',
          })}
        </div>
        <div className={styles.label} style={{ width: 150 }}>
          {$i18n.get({
            id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.parameterDescription',
            dm: '参数描述',
          })}
        </div>
        <div className={styles.label} style={{ width: 140 }}>
          {$i18n.get({
            id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.type',
            dm: '类型',
          })}
        </div>
        <div className={styles.label} style={{ width: 100 }}>
          {$i18n.get({
            id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.inputMethod',
            dm: '传入方法',
          })}
        </div>
        <div className={styles.label} style={{ width: 100 }}>
          {$i18n.get({
            id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.isRequired',
            dm: '是否必填',
          })}
        </div>
        <div className={styles.label} style={{ width: 120 }}>
          {$i18n.get({
            id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.passingMethod',
            dm: '传参方式',
          })}
        </div>
        <div style={{ width: 36 }}></div>
      </Flex>
      {params.map((item, index) => (
        <InputParamsRowComp
          requestMethod={requestMethod}
          hasProperties={hasProperties}
          handleRemove={() => deleteItem(index)}
          paramItem={item}
          list={params}
          key={`${fresh}_${index}`}
          index={index}
          onChange={(payload) => handleChangeItem(payload, index)}
        />
      ))}
      <Button
        className={'justify-start'}
        iconType="spark-plus-line"
        type="link"
        size="small"
        onClick={addRow}
      >
        {$i18n.get({
          id: 'main.pages.Component.Plugin.components.InputParamsConfig.index.addInputParameter',
          dm: '增加入参',
        })}
      </Button>
    </Flex>
  );
}
