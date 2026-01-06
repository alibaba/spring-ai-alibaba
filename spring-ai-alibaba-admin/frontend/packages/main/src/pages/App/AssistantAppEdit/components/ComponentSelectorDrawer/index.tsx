import $i18n from '@/i18n';
import { APP_ICON_IMAGE } from '@/pages/Component/AppComponent/components/AppSelector';
import { getAppComponentList, IAppType } from '@/services/appComponent';
import { IAppComponentListItem } from '@/types/appComponent';
import { Button, Drawer, Empty, IconFont } from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import {
  Checkbox,
  Flex,
  Input,
  message,
  Pagination,
  Spin,
  Typography,
} from 'antd';
import classNames from 'classnames';
import { useMemo, useRef, useState } from 'react';
import styles from './index.module.less';

interface IProps {
  type: IAppType;
  onSelect: (val: IAppComponentListItem[]) => void;
  selected: IAppComponentListItem[];
  maxLength?: number;
  onClose: () => void;
  appCode: string;
}

interface IComponentItemProps {
  data: IAppComponentListItem;
  selected: boolean;
  onChange: (val: boolean) => void;
}

export const ComponentItem = (props: IComponentItemProps) => {
  return (
    <Flex
      className={classNames(styles.checkItem, {
        [styles.selected]: props.selected,
      })}
      onClick={() => {
        props.onChange(!props.selected);
      }}
      align="center"
      gap={8}
    >
      <Checkbox checked={props.selected} />
      <img src={APP_ICON_IMAGE[props.data.type!]} alt="" />
      <div className="flex-1 w-1">
        <Flex align="center" gap={8}>
          <div className={styles.title}>{props.data.name}</div>
        </Flex>
        <Typography.Text
          ellipsis={{ tooltip: props.data.description }}
          className={styles.desc}
        >
          {props.data.description}
        </Typography.Text>
      </div>
    </Flex>
  );
};

export default function ComponentSelectorDrawer(props: IProps) {
  const { maxLength = 10 } = props;
  const [selectedComps, setSelectedComps] = useState(props.selected);
  const [state, setState] = useSetState({
    list: [] as IAppComponentListItem[],
    loading: true,
    total: 0,
    size: 10,
    current: 1,
    name: '',
  });
  const isSearch = useRef(false);

  const renderTitle = useMemo(() => {
    return props.type === IAppType.AGENT
      ? $i18n.get({
          id: 'main.components.ComponentSelectorDrawer.index.selectAgent',
          dm: '选择智能体',
        })
      : $i18n.get({
          id: 'main.components.ComponentSelectorDrawer.index.selectWorkflow',
          dm: '选择工作流',
        });
  }, [props.type]);

  const renderFooter = useMemo(() => {
    return (
      <Flex className="w-full" justify="space-between" align="center">
        <span className={styles.footerDesc}>
          {props.type === IAppType.AGENT
            ? $i18n.get({
                id: 'main.components.ComponentSelectorDrawer.index.addedAgents',
                dm: '已添加智能体数：',
              })
            : $i18n.get({
                id: 'main.components.ComponentSelectorDrawer.index.addedWorkflows',
                dm: '已添加工作流数：',
              })}
          {selectedComps.length || 0} / {maxLength}
        </span>
        <Flex align="center" gap={12}>
          <Button onClick={props.onClose}>
            {$i18n.get({
              id: 'main.components.ComponentSelectorDrawer.index.cancel',
              dm: '取消',
            })}
          </Button>
          <Button type="primary" onClick={() => props.onSelect(selectedComps)}>
            {$i18n.get({
              id: 'main.components.ComponentSelectorDrawer.index.confirm',
              dm: '确认',
            })}
          </Button>
        </Flex>
      </Flex>
    );
  }, [selectedComps, maxLength]);

  const fetchList = (params: any = {}) => {
    const extraParams = isSearch.current ? { name: state.name } : {};
    getAppComponentList({
      current: state.current,
      size: state.size,
      type: props.type,
      app_code: props.appCode,
      ...params,
      ...extraParams,
    }).then((res) => {
      setState({
        list: res.records,
        total: res.total,
        loading: false,
      });
    });
  };

  const handleSearch = () => {
    isSearch.current = true;
    setState({ current: 1 });
    fetchList({ current: 1 });
  };

  useMount(() => {
    fetchList();
  });

  const memoList = useMemo(() => {
    return state.list.map((item) => ({
      ...item,
      isSelected: selectedComps.some((vItem) => vItem.code === item.code),
    }));
  }, [state.list, selectedComps]);

  const changeItemSelected = (val: boolean, item: IAppComponentListItem) => {
    if (val) {
      if (selectedComps.length >= maxLength) {
        message.warning(
          $i18n.get(
            {
              id: 'main.components.ComponentSelectorDrawer.index.agentLimit',
              dm: '{var1}组件数量已达上限',
            },
            { var1: typeName },
          ),
        );
        return;
      }
      setSelectedComps([...selectedComps, item]);
    } else {
      setSelectedComps(
        selectedComps.filter((vItem) => vItem.code !== item.code),
      );
    }
  };

  const typeName = useMemo(() => {
    return props.type === IAppType.AGENT
      ? $i18n.get({
          id: 'main.components.ComponentSelectorDrawer.index.agent',
          dm: '智能体',
        })
      : $i18n.get({
          id: 'main.components.ComponentSelectorDrawer.index.workflow',
          dm: '工作流',
        });
  }, [props.type]);

  return (
    <Drawer
      width={640}
      onClose={props.onClose}
      footer={renderFooter}
      open
      title={renderTitle}
    >
      <Flex className="h-full" vertical gap={16}>
        <Flex justify="space-between" align="center">
          <Input.Search
            className={styles.search}
            placeholder={$i18n.get({
              id: 'main.components.ComponentSelectorDrawer.index.searchComponent',
              dm: '搜索组件名称',
            })}
            value={state.name}
            onChange={(e) => setState({ name: e.target.value })}
            onSearch={handleSearch}
          />

          <Button
            icon={<IconFont type="spark-plus-line" />}
            onClick={() => {
              window.open(
                `/component/${
                  props.type === IAppType.AGENT ? 'agent' : 'flow'
                }`,
              );
            }}
          >
            {$i18n.get({
              id: 'main.components.ComponentSelectorDrawer.index.create',
              dm: '创建',
            })}

            {typeName}
            {$i18n.get({
              id: 'main.components.ComponentSelectorDrawer.index.component',
              dm: '组件',
            })}
          </Button>
        </Flex>
        <div className="flex-1 overflow-y-auto">
          {state.loading ? (
            <Spin className="loading-center" spinning />
          ) : !state.list.length ? (
            <div className="loading-center">
              <Empty
                title={
                  isSearch.current
                    ? $i18n.get(
                        {
                          id: 'main.components.ComponentSelectorDrawer.index.noSearchResult',
                          dm: '未搜索到{var1}组件',
                        },
                        { var1: typeName },
                      )
                    : $i18n.get(
                        {
                          id: 'main.components.ComponentSelectorDrawer.index.noComponents',
                          dm: '暂无{var1}组件',
                        },
                        { var1: typeName },
                      )
                }
                description={
                  isSearch.current
                    ? $i18n.get({
                        id: 'main.components.ComponentSelectorDrawer.index.tryAnotherSearch',
                        dm: '换个搜索条件试试',
                      })
                    : $i18n.get({
                        id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.index.goToComponentManagementPageCreate',
                        dm: '请前往组件管理页面进行组件创建',
                      })
                }
              >
                {!isSearch.current && (
                  <Button
                    type="primary"
                    className="mt-4"
                    onClick={() => {
                      window.open(
                        `/component/${
                          props.type === IAppType.AGENT ? 'agent' : 'flow'
                        }`,
                      );
                    }}
                  >
                    {$i18n.get({
                      id: 'main.components.ComponentSelectorDrawer.index.goToCreate',
                      dm: '前往创建',
                    })}
                  </Button>
                )}
              </Empty>
            </div>
          ) : (
            <Flex vertical gap={16}>
              {memoList.map(({ isSelected, ...item }) => (
                <ComponentItem
                  onChange={(val) => changeItemSelected(val, item)}
                  selected={isSelected}
                  data={item}
                  key={item.code}
                />
              ))}
            </Flex>
          )}
        </div>
        <Pagination
          hideOnSinglePage
          pageSize={state.size}
          current={state.current}
          total={state.total}
          onChange={(current, size) => {
            setState({
              current: current,
              size: size,
            });
            fetchList({ current: current, size: size });
          }}
        />
      </Flex>
    </Drawer>
  );
}
