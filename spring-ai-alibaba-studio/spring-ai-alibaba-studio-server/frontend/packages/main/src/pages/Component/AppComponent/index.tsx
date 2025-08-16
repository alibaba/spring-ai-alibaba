import CardList from '@/components/Card/List';
import { useInnerLayout } from '@/components/InnerLayout/utils';
import Search from '@/components/Search';
import $i18n from '@/i18n';
import {
  deleteAppComponentByCode,
  getAppComponentList,
  IAppComponentListQueryParams,
  IAppType,
} from '@/services/appComponent';
import {
  IAppComponentListItem,
  IEnableAppListItem,
} from '@/types/appComponent';
import { IconFont } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Button, message, Modal } from 'antd';
import classNames from 'classnames';
import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppSelectorModalByAppComponent } from './components/AppSelector';
import AppComponentCard from './components/Card';
import DetailDrawer from './components/DetailDrawer';
import EditDrawer from './components/EditDrawer';
import ReferDetailDrawer from './components/ReferDetailDrawer';

export default function AppComponent(props: { type: IAppType }) {
  const [state, setState] = useSetState({
    loading: false,
    list: [] as IAppComponentListItem[],
    name: '',
    current: 1,
    size: 50,
    total: 0,
    showCreateModal: false,
    detailRecord: null as IAppComponentListItem | null,
    activeRecord: null as Partial<IAppComponentListItem> | null,
    referRecord: null as IAppComponentListItem | null,
  });
  const isSearchRef = useRef(false);
  const navigate = useNavigate();
  const portal = useInnerLayout();

  const fetchList = (
    extraParams: Partial<IAppComponentListQueryParams> = {},
  ) => {
    const searchParams = isSearchRef.current
      ? {
          name: state.name,
        }
      : {};
    setState({
      loading: true,
    });
    getAppComponentList({
      size: state.size,
      current: state.current,
      type: props.type,
      ...searchParams,
      ...extraParams,
    })
      .then((res) => {
        setState({
          list: res.records,
          total: res.total,
        });
      })
      .finally(() => {
        setState({
          loading: false,
        });
      });
  };

  useEffect(() => {
    fetchList();
  }, [props.type]);

  const handleSearch = (val: string) => {
    isSearchRef.current = !!val;
    setState({
      current: 1,
    });
    fetchList({
      current: 1,
    });
  };

  const handleDelete = (code: string) => {
    Modal.confirm({
      title: $i18n.get({
        id: 'main.pages.Component.AppComponent.index.delete',
        dm: '是否删除',
      }),
      content: $i18n.get({
        id: 'main.pages.Component.AppComponent.index.noticeDeleteComponent',
        dm: '请注意，删除组件会导致所有已关联的应用中的组件失效，请确认关联应用清单',
      }),

      onOk: () => {
        deleteAppComponentByCode(code).then(() => {
          message.success(
            $i18n.get({
              id: 'main.pages.Component.AppComponent.index.successDelete',
              dm: '删除成功',
            }),
          );
          let current = state.current;
          if (state.list.length === 1 && current > 1) {
            current -= 1;
            setState({
              current,
            });
          }
          fetchList({
            current,
          });
        });
      },
    });
  };

  const handleClickAction = (key: string, item: IAppComponentListItem) => {
    switch (key) {
      case 'gotoApp':
        navigate(
          `/app/${item.type === IAppType.WORKFLOW ? 'workflow' : 'assistant'}/${
            item.app_id
          }`,
        );
        break;
      case 'edit':
        setState({ activeRecord: item });
        break;
      case 'detail':
        setState({ detailRecord: item });
        break;
      case 'referDetail':
        setState({ referRecord: item });
        break;
      case 'delete':
        handleDelete(item.code!);
        break;
    }
  };

  return (
    <>
      {portal.rightPortal(
        <Button
          type="primary"
          icon={<IconFont type="spark-plus-line" />}
          onClick={() => setState({ showCreateModal: true })}
        >
          {$i18n.get({
            id: 'main.pages.Component.AppComponent.index.create',
            dm: '创建',
          })}

          {props.type === IAppType.WORKFLOW
            ? $i18n.get({
                id: 'main.pages.Component.AppComponent.index.workflow',
                dm: '工作流',
              })
            : $i18n.get({
                id: 'main.pages.Component.AppComponent.index.intelligentAgent',
                dm: '智能体',
              })}
          {$i18n.get({
            id: 'main.pages.Component.AppComponent.index.component',
            dm: '组件',
          })}
        </Button>,
      )}
      {!state.list.length && !isSearchRef.current ? null : (
        <Search
          placeholder={$i18n.get({
            id: 'main.pages.Component.AppComponent.index.enterComponentName',
            dm: '请输入组件名称',
          })}
          value={state.name}
          onChange={(val) => setState({ name: val })}
          className={classNames('mx-[20px] my-[16px]')}
          onSearch={handleSearch}
        />
      )}
      <CardList
        pagination={{
          current: state.current,
          total: state.total,
          pageSize: state.size,
          onChange: (current, size) => {
            setState({
              current,
              size,
            });
            fetchList({
              current,
              size,
            });
          },
        }}
        isSearch={isSearchRef.current}
        emptyAction={
          <Button
            onClick={() => setState({ showCreateModal: true })}
            type="primary"
            icon={<IconFont type="spark-plus-line" />}
          >
            {$i18n.get({
              id: 'main.pages.Component.AppComponent.index.create',
              dm: '创建',
            })}

            {props.type === IAppType.WORKFLOW
              ? $i18n.get({
                  id: 'main.pages.Component.AppComponent.index.workflow',
                  dm: '工作流',
                })
              : $i18n.get({
                  id: 'main.pages.Component.AppComponent.index.intelligentAgent',
                  dm: '智能体',
                })}
            {$i18n.get({
              id: 'main.pages.Component.AppComponent.index.component',
              dm: '组件',
            })}
          </Button>
        }
        loading={state.loading}
      >
        {state.list.map((item) => (
          <AppComponentCard
            key={item.code}
            {...item}
            onClickAction={(key) => handleClickAction(key, item)}
          />
        ))}
      </CardList>
      {state.activeRecord && (
        <EditDrawer
          data={state.activeRecord}
          onOk={() => {
            isSearchRef.current = false;
            setState({ current: 1, name: '' });
            fetchList({
              current: 1,
            });
            setState({ activeRecord: null });
          }}
          onClose={() => {
            setState({ activeRecord: null });
          }}
        />
      )}
      {state.detailRecord && (
        <DetailDrawer
          onClose={() => setState({ detailRecord: null })}
          data={state.detailRecord}
        />
      )}
      {state.referRecord && (
        <ReferDetailDrawer
          data={state.referRecord}
          onClose={() => {
            setState({ referRecord: null });
          }}
        />
      )}
      {state.showCreateModal && (
        <AppSelectorModalByAppComponent
          type={props.type}
          onOk={(val: IEnableAppListItem) => {
            setState({
              showCreateModal: false,
              activeRecord: {
                app_id: val.app_id,
                app_name: val.name,
                type: props.type,
              },
            });
          }}
          onClose={() => {
            setState({ showCreateModal: false });
          }}
        />
      )}
    </>
  );
}
