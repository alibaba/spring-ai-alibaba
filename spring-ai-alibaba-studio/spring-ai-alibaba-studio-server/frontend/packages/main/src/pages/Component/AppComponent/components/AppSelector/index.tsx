import Search from '@/components/Search';
import $i18n from '@/i18n';
import { getEnableAppList, IAppType } from '@/services/appComponent';
import { IEnableAppListItem } from '@/types/appComponent';
import {
  Button,
  Checkbox,
  Empty,
  IconFont,
  message,
  Modal,
  Pagination,
  Radio,
} from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { Flex, Spin, Typography } from 'antd';
import classNames from 'classnames';
import { useMemo, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './index.module.less';

interface IProps {
  list: IEnableAppListItem[];
  value?: string | string[];
  onSelect: (
    val: IEnableAppListItem | string[],
    target: IEnableAppListItem,
  ) => void;
  onRemove?: (val: string) => void;
  className?: string;
  loading?: boolean;
  isSearch?: boolean;
  isMulti?: boolean;
  pagination?: {
    pageSize: number;
    total: number;
    current: number;
    onChange: (page: number, pageSize: number) => void;
  };
}

export const APP_ICON_IMAGE = {
  [IAppType.AGENT]: '/images/agentLogo.svg',
  [IAppType.WORKFLOW]: '/images/workflowLogo.svg',
};

export default function AppSelector(props: IProps) {
  const navigate = useNavigate();
  const memoList = useMemo(() => {
    if (!props.value || (Array.isArray(props.value) && !props.value.length))
      return props.list.map((item) => ({
        ...item,
        isSelected: false,
      }));

    return props.list.map((item) => {
      const isSelected = Array.isArray(props.value)
        ? props.value.includes(item.app_id)
        : props.value === item.app_id;
      return {
        ...item,
        isSelected,
      };
    });
  }, [props.value, props.list]);

  if (props.loading) return <Spin className="loading-center" spinning />;
  if (!props.list.length)
    return (
      <div className="loading-center">
        <Empty
          title={
            props.isSearch
              ? $i18n.get({
                  id: 'main.pages.Component.AppComponent.components.AppSelector.index.noSearchResult',
                  dm: '未搜索出来符合条件的应用',
                })
              : $i18n.get({
                  id: 'main.pages.Component.AppComponent.components.AppSelector.index.noApplication',
                  dm: '暂无应用',
                })
          }
          description={
            props.isSearch
              ? $i18n.get({
                  id: 'main.pages.Component.AppComponent.components.AppSelector.index.tryAnotherSearchCondition',
                  dm: '换个搜索条件试试',
                })
              : $i18n.get({
                  id: 'main.pages.Component.AppComponent.components.AppSelector.index.goToCreateApplicationFirst',
                  dm: '请先前往应用管理创建应用',
                })
          }
        >
          {!props.isSearch && (
            <Button
              className="mt-[12px]"
              onClick={() => navigate('/app')}
              type="primary"
            >
              {$i18n.get({
                id: 'main.pages.Component.AppComponent.components.AppSelector.index.goToCreate',
                dm: '前往创建',
              })}
            </Button>
          )}
        </Empty>
      </div>
    );

  return (
    <Flex className="h-full" vertical gap={12}>
      <Flex className={classNames(styles.list, props.className)} gap={12} wrap>
        {memoList.map((item) => (
          <div
            onClick={() => {
              if (props.isMulti) {
                const newVal = (props.value as string[]) || [];
                if (item.isSelected) {
                  props.onRemove?.(item.app_id);
                } else {
                  props.onSelect([...newVal, item.app_id], item);
                }
              } else {
                if (item.isSelected) return;
                props.onSelect(item, item);
              }
            }}
            className={classNames(styles.card, {
              [styles.selected]: item.isSelected,
            })}
            key={item.app_id}
          >
            <Flex gap={8} align="center">
              <img src={APP_ICON_IMAGE[item.type]} alt="" />
              <Flex align="center" gap={4} className="flex-1 w-1">
                <Typography.Text
                  ellipsis={{ tooltip: item.name }}
                  className={styles.title}
                >
                  {item.name}
                </Typography.Text>
                <IconFont
                  onClick={() => {
                    window.open(
                      `/app/${
                        item.type === IAppType.AGENT ? 'assistant' : 'workflow'
                      }/${item.app_id}`,
                    );
                  }}
                  className="cursor-pointer"
                  type="spark-setting-line"
                />
              </Flex>
              {props.isMulti ? (
                <Checkbox checked={item.isSelected} />
              ) : (
                <Radio checked={item.isSelected} />
              )}
            </Flex>
            <div className={styles.desc}>
              {$i18n.get(
                {
                  id: 'main.pages.Component.AppComponent.components.AppSelector.index.idVar1',
                  dm: 'ID：{var1}',
                },
                { var1: item.app_id },
              )}
            </div>
          </div>
        ))}
      </Flex>
      {props.pagination && (
        <Pagination
          hideOnSinglePage
          className={styles.page}
          {...props.pagination}
        />
      )}
    </Flex>
  );
}

interface IAppModalProps {
  onOk: (val: IEnableAppListItem) => void;
  onClose: () => void;
  type: IAppType;
}

export function AppSelectorModalByAppComponent(props: IAppModalProps) {
  const [state, setState] = useSetState({
    loading: true,
    list: [] as IEnableAppListItem[],
    app_name: '',
    current: 1,
    size: 10,
    total: 0,
    selectedApp: null as IEnableAppListItem | null,
  });
  const isSearchRef = useRef(false);

  const handleOk = () => {
    if (!state.selectedApp) {
      message.warning(
        $i18n.get({
          id: 'main.pages.Component.AppComponent.components.AppSelector.index.selectApplication',
          dm: '请选择应用',
        }),
      );
      return;
    }
    props.onOk(state.selectedApp);
  };

  const fetchList = (params: any = {}) => {
    setState({ loading: true });
    const extraParams = isSearchRef.current ? { app_name: state.app_name } : {};
    getEnableAppList({
      type: props.type,
      current: state.current,
      size: state.size,
      ...params,
      ...extraParams,
    })
      .then((res) => {
        setState({
          list: res.records,
          total: res.total,
          loading: false,
        });
      })
      .catch(() => {
        setState({
          loading: false,
        });
      });
  };

  useMount(() => {
    fetchList();
  });

  const handleSearch = () => {
    isSearchRef.current = !!state.app_name;
    setState({ current: 1 });
    fetchList({ current: 1 });
  };

  return (
    <Modal
      width={807}
      onOk={handleOk}
      onCancel={props.onClose}
      open
      title={$i18n.get({
        id: 'main.pages.Component.AppComponent.components.AppSelector.index.selectApplication',
        dm: '选择应用',
      })}
    >
      <Flex vertical gap={12}>
        <Flex justify="space-between" align="center">
          <Search
            className={styles.search}
            value={state.app_name}
            onChange={(val) => setState({ app_name: val })}
            onSearch={handleSearch}
            placeholder={$i18n.get({
              id: 'main.pages.Component.AppComponent.components.AppSelector.index.searchApplicationName',
              dm: '搜索应用名称',
            })}
          />

          <Button
            icon={<IconFont type="spark-plus-line" />}
            onClick={() => window.open('/app')}
          >
            {$i18n.get({
              id: 'main.pages.Component.AppComponent.components.AppSelector.index.create',
              dm: '创建',
            })}

            {props.type === IAppType.AGENT
              ? $i18n.get({
                  id: 'main.pages.Component.AppComponent.components.AppSelector.index.smartAgent',
                  dm: '智能体',
                })
              : $i18n.get({
                  id: 'main.pages.Component.AppComponent.components.AppSelector.index.workflow',
                  dm: '工作流',
                })}
          </Button>
        </Flex>
        <div className={styles['list-wrap']}>
          <AppSelector
            onSelect={(val) =>
              setState({ selectedApp: val as IEnableAppListItem })
            }
            list={state.list}
            value={state.selectedApp?.app_id}
            loading={state.loading}
            isSearch={isSearchRef.current}
            pagination={{
              total: state.total,
              pageSize: state.size,
              current: state.current,
              onChange: (current, size) => {
                setState({ current, size });
                fetchList({ current, size });
              },
            }}
          />
        </div>
      </Flex>
    </Modal>
  );
}
