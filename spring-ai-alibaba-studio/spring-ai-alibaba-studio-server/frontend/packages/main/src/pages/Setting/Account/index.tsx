import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { deleteAccount, getAccountList } from '@/services/account';
import type { IAccount } from '@/types/account';
import {
  AlertDialog,
  Button,
  IconFont,
  Pagination,
  Tag,
} from '@spark-ai/design';
import type { TableProps } from 'antd';
import { Table } from 'antd';
import classNames from 'classnames';
import { useEffect, useState } from 'react';
import UserEditModal, { UserEditData } from './components/UserEditModal';
import styles from './index.module.less';

export default function Account() {
  const [isUserModalOpen, setIsUserModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserEditData | null>(null);
  const [dataSource, setDataSource] = useState<IAccount[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [current, setCurrent] = useState<number>(1);
  const [pageSize, setPageSize] = useState<number>(10);
  const [total, setTotal] = useState<number>(0);

  const isAdmin = window.g_config.user?.type === 'admin';

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async (params = {}) => {
    setLoading(true);
    try {
      const response = await getAccountList({
        ...params,
      });
      if (response.data) {
        const formattedData = response.data.records.map((user) => ({
          ...user,
          key: user.account_id,
        }));
        setDataSource(formattedData);
        setTotal(response.data.total);
        setCurrent(response.data.current);
        setPageSize(response.data.size);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleOpenAddModal = () => {
    setEditingUser(null);
    setIsUserModalOpen(true);
  };

  const handleOpenEditModal = (user: IAccount) => {
    setEditingUser({ key: user.account_id, name: user.username });
    setIsUserModalOpen(true);
  };

  const handleModalCancel = () => {
    setIsUserModalOpen(false);
    setEditingUser(null);
  };

  const handleModalOk = async () => {
    setIsUserModalOpen(false);
    setEditingUser(null);
    await fetchData();
  };

  const handleDeleteUser = async (user: IAccount) => {
    AlertDialog.warning({
      title: $i18n.get({
        id: 'main.pages.Setting.Account.index.deleteUser',
        dm: '删除用户',
      }),
      content: $i18n.get({
        id: 'main.pages.Setting.Account.index.confirmDeleteUser',
        dm: '确定要删除该用户吗？',
      }),
      onOk: async () => {
        await deleteAccount(user.account_id);
        await fetchData();
      },
    });
  };

  const columns: TableProps<IAccount>['columns'] = [
    {
      title: $i18n.get({
        id: 'main.pages.Setting.Account.index.userName',
        dm: '用户名称',
      }),
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: $i18n.get({
        id: 'main.pages.Setting.Account.index.role',
        dm: '角色',
      }),
      dataIndex: 'type',
      key: 'type',
      render: (type) => (
        <Tag color={type === 'admin' ? 'purple' : 'default'}>
          {type === 'admin'
            ? $i18n.get({
                id: 'main.pages.Setting.Account.index.admin',
                dm: '管理员',
              })
            : $i18n.get({
                id: 'main.pages.Setting.Account.index.user',
                dm: '用户',
              })}
        </Tag>
      ),
    },
    {
      title: $i18n.get({
        id: 'main.pages.Setting.Account.index.edit',
        dm: '编辑',
      }),
      key: 'action',
      render: (_, record) => (
        <span className={styles.actions}>
          <a onClick={() => handleOpenEditModal(record)}>
            {$i18n.get({
              id: 'main.pages.Setting.Account.index.edit',
              dm: '编辑',
            })}
          </a>
          <a
            className={classNames({ [styles.disabled]: !isAdmin })}
            onClick={() => handleDeleteUser(record)}
          >
            {$i18n.get({
              id: 'main.pages.Setting.Account.index.deleteUser',
              dm: '删除用户',
            })}
          </a>
        </span>
      ),
    },
  ];

  const pagination = (
    <div className={styles.pagination}>
      <Pagination
        hideTips
        current={current}
        pageSize={pageSize}
        total={total}
        onChange={async (current, pageSize) => {
          await fetchData({
            current,
            size: pageSize,
          });
        }}
      />
    </div>
  );

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
            id: 'main.pages.Setting.Account.index.accountManagement',
            dm: '账户管理',
          }),
        },
      ]}
      right={
        <Button
          type="primary"
          icon={<IconFont type="spark-plus-line" />}
          onClick={handleOpenAddModal}
        >
          {$i18n.get({
            id: 'main.pages.Setting.Account.index.addUser',
            dm: '新增用户',
          })}
        </Button>
      }
      bottom={pagination}
    >
      <div className={styles.container}>
        <Table
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          rowKey="account_id"
          pagination={false}
        />
      </div>

      <UserEditModal
        open={isUserModalOpen}
        onCancel={handleModalCancel}
        onOk={handleModalOk}
        initialValues={editingUser}
      />
    </InnerLayout>
  );
}
