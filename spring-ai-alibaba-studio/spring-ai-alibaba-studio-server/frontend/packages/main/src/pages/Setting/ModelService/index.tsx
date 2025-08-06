import CardList from '@/components/Card/List';
import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import {
  deleteProvider,
  listProviders,
  updateProvider,
} from '@/services/modelService';
import { IProvider } from '@/types/modelService';
import { AlertDialog, Button, IconFont, message } from '@spark-ai/design';
import { useEffect, useState } from 'react';
import { useNavigate } from 'umi';
import ModelServiceCard from './components/ModelServiceCard';
import ModelServiceProviderModal from './components/ModelServiceProviderModal';
import styles from './index.module.less';

const ModelService = () => {
  const navigate = useNavigate();
  const [providers, setProviders] = useState<IProvider[]>([]);
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    fetchProviders();
  }, []);

  const fetchProviders = async () => {
    try {
      setLoading(true);
      const res = await listProviders();
      setProviders(res?.data || []);
    } finally {
      setLoading(false);
    }
  };

  const handleServiceClick = (action?: string, provider?: IProvider) => {
    if (!action || !provider) return;
    switch (action) {
      case 'detail':
      case 'edit':
        navigate(`/setting/modelService/${provider.provider}`);
        break;
      case 'delete':
        AlertDialog.warning({
          title: $i18n.get({
            id: 'main.pages.Setting.ModelService.index.delete',
            dm: '删除',
          }),
          content: $i18n.get({
            id: 'main.pages.Setting.ModelService.index.confirmDeleteModelServiceProvider',
            dm: '确定删除该模型服务商吗？',
          }),
          onOk: () => {
            handleDeleteService(provider);
          },
        });
        break;
      case 'start':
        handleEnableService(provider, true);
        break;
      case 'stop':
        handleEnableService(provider, false);
        break;
    }
  };

  const handleDeleteService = (provider: IProvider) => {
    deleteProvider(provider.provider).then((res) => {
      if (res.data) {
        message.success(
          $i18n.get({
            id: 'main.pages.Setting.ModelService.index.deleteSuccess',
            dm: '删除成功',
          }),
        );
        fetchProviders();
      }
    });
  };

  const handleEnableService = (provider: IProvider, enable: boolean) => {
    updateProvider(provider.provider, { ...provider, enable }).then((res) => {
      if (res.data) {
        message.success(
          enable
            ? $i18n.get({
                id: 'main.pages.Setting.ModelService.index.startSuccess',
                dm: '启动成功',
              })
            : $i18n.get({
                id: 'main.pages.Setting.ModelService.index.stopSuccess',
                dm: '停止成功',
              }),
        );
        fetchProviders();
      }
    });
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
            id: 'main.pages.Setting.ModelService.index.modelServiceManagement',
            dm: '模型服务管理',
          }),
        },
      ]}
      right={
        <>
          <Button
            type="primary"
            icon={<IconFont type="spark-plus-line" />}
            onClick={() => setIsModalOpen(true)}
          >
            {$i18n.get({
              id: 'main.pages.Setting.ModelService.index.addModelServiceProvider',
              dm: '新增模型服务商',
            })}
          </Button>
        </>
      }
      styles={{
        breadcrumb: {
          maxWidth: 300,
        },
      }}
    >
      <div className={styles.container}>
        <CardList loading={loading}>
          {providers.map((provider) => (
            <ModelServiceCard
              key={provider.provider}
              service={provider}
              onClick={handleServiceClick}
            />
          ))}
        </CardList>
      </div>
      <ModelServiceProviderModal
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onSuccess={() => {
          setIsModalOpen(false);
          fetchProviders();
        }}
      />
    </InnerLayout>
  );
};

export default ModelService;
