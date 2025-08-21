import CardList from '@/components/Card/List';
import InnerLayout from '@/components/InnerLayout';
import Search from '@/components/Search';
import $i18n from '@/i18n';
import { IAppType } from '@/services/appComponent';
import {
  copyApp,
  deleteApp,
  getAppList,
  IGetAppListParams,
} from '@/services/appManage';
import { IAppCard } from '@/types/appManage';
import { AlertDialog, Button, IconFont, message } from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import AppCard from './components/Card';
import CreateModal from './components/CreateModal';
import { EditNameModal } from './components/EditNameModal';

const tabs = [
  {
    label: $i18n.get({
      id: 'main.pages.App.index.allApplications',
      dm: '全部应用',
    }),
    key: 'all',
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.index.smartAgentApplication',
      dm: '智能体应用',
    }),
    key: 'agent',
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.index.workflowApplication',
      dm: '工作流应用',
    }),
    key: 'workflow',
  },
];

const typeMap: Record<string, IAppType | undefined> = {
  all: void 0,
  agent: IAppType.AGENT,
  workflow: IAppType.WORKFLOW,
};

export default function () {
  const { tab } = useParams();
  const [state, setState] = useSetState({
    activeTab: tab || tabs[0].key,
    size: 50,
    current: 1,
    total: 0,
    name: '',
    loading: true,
    status: '',
    list: [] as IAppCard[],
    showCreateModal: false,
    activeRecord: null as IAppCard | null,
    showEditNameModal: false,
  });
  const isSearchRef = useRef(false);
  const navigate = useNavigate();

  const fetchList = (extraParams: Partial<IGetAppListParams> = {}) => {
    const searchParams = isSearchRef.current
      ? {
          name: state.name,
        }
      : {};
    setState({
      loading: true,
    });
    getAppList({
      size: state.size,
      current: state.current,
      status: state.status.length ? state.status : undefined,
      type: typeMap[state.activeTab],
      ...searchParams,
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

  const onTabChange = (key: string) => {
    navigate(`/app/${key}`);
    isSearchRef.current = false;
    setState({
      current: 1,
      name: '',
      activeTab: key,
    });
    fetchList({
      current: 1,
      type: typeMap[key],
    });
  };

  const handleSearch = (val: string) => {
    isSearchRef.current = !!val;
    setState({
      current: 1,
    });
    fetchList({
      current: 1,
    });
  };

  const handleDelete = (app_id: string) => {
    AlertDialog.warning({
      title: $i18n.get({
        id: 'main.pages.App.index.deleteApplication',
        dm: '删除应用',
      }),
      content: $i18n.get({
        id: 'main.pages.App.index.confirmDeleteApplication',
        dm: '确定删除该应用吗？',
      }),
      onOk: () => {
        deleteApp(app_id).then(() => {
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

  const onClose = (needFresh = false) => {
    if (needFresh) fetchList();
    setState({
      activeRecord: null,
      showEditNameModal: false,
    });
  };

  const initList = () => {
    isSearchRef.current = false;
    setState({
      current: 1,
      name: '',
      status: '',
    });
    fetchList({
      current: 1,
      status: '',
    });
  };

  const gotoAppDetail = (val: { type: IAppType; app_id: string }) => {
    navigate(
      `/app/${val.type === IAppType.AGENT ? 'assistant' : 'workflow'}/${
        val.app_id
      }`,
    );
  };

  const handleClickAction = (key: string, item: IAppCard) => {
    switch (key) {
      case 'click':
      case 'edit':
        gotoAppDetail(item);
        break;
      case 'editName':
        setState({
          activeRecord: item,
          showEditNameModal: true,
        });
        break;
      case 'copy':
        copyApp(item.app_id).then(() => {
          message.success(
            $i18n.get({
              id: 'main.pages.App.index.copySuccess',
              dm: '复制成功',
            }),
          );
          initList();
        });
        break;
      case 'delete':
        handleDelete(item.app_id);
        break;
      default:
        break;
    }
  };

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.App.index.home',
            dm: '首页',
          }),
          path: '/',
        },
        {
          title: $i18n.get({
            id: 'main.pages.App.index.applicationManagement',
            dm: '应用管理',
          }),
        },
      ]}
      activeTab={state.activeTab}
      tabs={tabs}
      right={
        <Button
          onClick={() => setState({ showCreateModal: true })}
          icon={<IconFont type="spark-plus-line" />}
          type="primary"
        >
          {$i18n.get({
            id: 'main.pages.App.index.createApplication',
            dm: '创建应用',
          })}
        </Button>
      }
      onTabChange={onTabChange}
    >
      {!state.list.length && !isSearchRef.current ? null : (
        <Search
          placeholder={$i18n.get({
            id: 'main.pages.App.index.enterApplicationName',
            dm: '请输入应用名称',
          })}
          value={state.name}
          onChange={(val) => setState({ name: val })}
          className={'mx-[20px] my-[16px]'}
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
            icon={<IconFont type="spark-plus-line" />}
            onClick={() => setState({ showCreateModal: true })}
            type="primary"
          >
            {$i18n.get({
              id: 'main.pages.App.index.createApplication',
              dm: '创建应用',
            })}
          </Button>
        }
        loading={state.loading}
      >
        {state.list.map((item) => (
          <AppCard
            key={item.app_id}
            {...item}
            onClickAction={(key) => handleClickAction(key, item)}
          />
        ))}
      </CardList>
      {state.showCreateModal && (
        <CreateModal
          onCancel={() => setState({ showCreateModal: false })}
          onOk={(val) => {
            setState({ showCreateModal: false });
            gotoAppDetail(val);
          }}
        />
      )}
      {state.showEditNameModal && !!state.activeRecord && (
        <EditNameModal
          onClose={() => onClose()}
          onOk={() => onClose(true)}
          app_id={state.activeRecord.app_id}
          name={state.activeRecord.name}
          description={state.activeRecord.description}
        />
      )}
    </InnerLayout>
  );
}
