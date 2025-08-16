import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import {
  createModel,
  deleteModel,
  getProviderDetail,
  listModels,
  updateModel,
} from '@/services/modelService';
import {
  ICreateModelParams,
  IModel,
  IProviderConfigInfo,
  MODEL_TAGS,
} from '@/types/modelService';
import {
  AlertDialog,
  Button,
  Empty,
  Switch,
  Tag,
  message,
} from '@spark-ai/design';
import { Spin, Table } from 'antd';
import React, { useEffect, useState } from 'react';
import { useParams } from 'umi';
import styles from './Detail.module.less';
import ModelConfigModal from './components/ModelConfigModal';
import ProviderInfoForm from './components/ProviderInfoForm';

const ModelServiceDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [isModelConfigModalVisible, setIsModelConfigModalVisible] =
    useState<boolean>(false);
  const [currentModel, setCurrentModel] = useState<IModel | undefined>(
    undefined,
  );
  const [provider, setProvider] = useState<IProviderConfigInfo | null>(null);
  const [models, setModels] = useState<IModel[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [modelLoading, setModelLoading] = useState<boolean>(true);

  const fetchProviderDetail = () => {
    if (!id) return;

    setLoading(true);
    getProviderDetail(id)
      .then((response) => {
        if (response?.data) {
          setProvider(response.data);
        }
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchProviderDetail();
  }, [id]);

  const fetchModels = () => {
    if (!id) return;

    setModelLoading(true);
    listModels(id)
      .then((response) => {
        if (response?.data) {
          setModels(response.data);
        }
      })
      .finally(() => {
        setModelLoading(false);
      });
  };

  useEffect(() => {
    fetchModels();
  }, [id]);

  const columns = [
    {
      title: $i18n.get({
        id: 'main.pages.Setting.ModelService.Detail.modelName',
        dm: '模型名称',
      }),
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => (
        <div className={styles['model-name']}>
          <span>{text}</span>
        </div>
      ),
    },
    {
      title: $i18n.get({
        id: 'main.pages.Setting.ModelService.Detail.modelType',
        dm: '模型类型',
      }),
      dataIndex: 'type',
      key: 'type',
    },
    {
      title: $i18n.get({
        id: 'main.pages.Setting.ModelService.Detail.modelAbility',
        dm: '模型能力',
      }),
      dataIndex: 'tags',
      key: 'tags',
      render: (tags: string[]) => {
        if (!tags) return null;
        return (
          <div className={styles['capabilities']}>
            {tags.map((tag, index) => (
              <Tag key={index} color="mauve">
                {MODEL_TAGS[tag as keyof typeof MODEL_TAGS]}
              </Tag>
            ))}
          </div>
        );
      },
    },
    {
      title: $i18n.get({
        id: 'main.pages.Setting.ModelService.Detail.enable',
        dm: '启用',
      }),
      dataIndex: 'enable',
      key: 'enable',
      render: (enable: boolean, record: IModel) => (
        <Switch
          checked={enable}
          onChange={(checked) =>
            updateModelInfo({ ...record, enable: checked })
          }
        />
      ),
    },
    {
      title: $i18n.get({
        id: 'main.pages.Setting.ModelService.Detail.operation',
        dm: '操作',
      }),
      key: 'action',
      render: (_: any, record: IModel) => {
        const isPreset = record.source === 'preset';
        return (
          <div className={styles['action-buttons']}>
            <a onClick={() => handleConfigModel(record)}>
              {$i18n.get({
                id: 'main.pages.Setting.ModelService.Detail.setting',
                dm: '设置',
              })}
            </a>
            <a
              className={isPreset ? styles['disabled'] : ''}
              onClick={() => handleDeleteModel(record)}
            >
              {$i18n.get({
                id: 'main.pages.Setting.ModelService.Detail.delete',
                dm: '删除',
              })}
            </a>
          </div>
        );
      },
    },
  ];

  const handleConfigModel = (record: IModel) => {
    setCurrentModel(record);
    setIsModelConfigModalVisible(true);
  };

  const handleDeleteModel = (record: IModel) => {
    AlertDialog.warning({
      title: $i18n.get({
        id: 'main.pages.Setting.ModelService.Detail.deleteModel',
        dm: '删除模型',
      }),
      children: $i18n.get(
        {
          id: 'main.pages.Setting.ModelService.Detail.confirmDeleteModel',
          dm: '确定要删除模型{var1}吗？此操作不可恢复。',
        },
        { var1: record.name },
      ),
      onOk: () => {
        if (!record.model_id) {
          message.error(
            $i18n.get({
              id: 'main.pages.Setting.ModelService.Detail.modelIdNotExist',
              dm: '模型ID不存在',
            }),
          );
          return;
        }
        if (!id) {
          message.error(
            $i18n.get({
              id: 'main.pages.Setting.ModelService.Detail.providerIdNotExist',
              dm: '提供商ID不存在',
            }),
          );
          return;
        }
        deleteModel(id, record.model_id).then((response) => {
          if (response) {
            message.success(
              $i18n.get({
                id: 'main.pages.Setting.ModelService.Detail.deleteModelSuccess',
                dm: '删除模型成功',
              }),
            );
            fetchModels();
          }
        });
      },
    });
  };

  const handleAddModel = () => {
    setCurrentModel(undefined);
    setIsModelConfigModalVisible(true);
  };

  const updateModelInfo = async (modelInfo: IModel) => {
    if (!id) {
      message.error(
        $i18n.get({
          id: 'main.pages.Setting.ModelService.Detail.providerIdNotExist',
          dm: '提供商ID不存在',
        }),
      );
      return;
    }

    const updateParams: ICreateModelParams = {
      ...modelInfo,
      name: modelInfo.name,
    };

    const response = await updateModel(id, modelInfo.model_id, updateParams);
    if (response) {
      message.success(
        $i18n.get({
          id: 'main.pages.Setting.ModelService.Detail.updateModelSuccess',
          dm: '更新模型成功',
        }),
      );
      setIsModelConfigModalVisible(false);
      fetchModels();
    }
  };

  const createModelInfo = async (modelInfo: ICreateModelParams) => {
    if (!id) {
      message.error(
        $i18n.get({
          id: 'main.pages.Setting.ModelService.Detail.providerIdNotExist',
          dm: '提供商ID不存在',
        }),
      );
      return;
    }

    const createParams: ICreateModelParams = {
      ...modelInfo,
      name: modelInfo.name,
    };

    const response = await createModel(id, createParams);
    if (response) {
      message.success(
        $i18n.get({
          id: 'main.pages.Setting.ModelService.Detail.addModelSuccess',
          dm: '添加模型成功',
        }),
      );
      setIsModelConfigModalVisible(false);
      fetchModels();
    }
  };

  const handleModelConfigSubmit = async (modelInfo: ICreateModelParams) => {
    if (!id) {
      message.error(
        $i18n.get({
          id: 'main.pages.Setting.ModelService.Detail.providerIdNotExist',
          dm: '提供商ID不存在',
        }),
      );
      return;
    }

    if (currentModel) {
      await updateModelInfo({ ...currentModel, ...modelInfo });
    } else {
      await createModelInfo(modelInfo);
    }
  };

  if (loading) {
    return (
      <InnerLayout
        breadcrumbLinks={[
          {
            title: $i18n.get({
              id: 'main.pages.Setting.ModelService.Detail.modelServiceManagement',
              dm: '模型服务管理',
            }),
            path: `/setting/modelService`,
          },
          {
            title: $i18n.get({
              id: 'main.pages.Setting.ModelService.Detail.modelServiceDetail',
              dm: '模型服务详情',
            }),
          },
        ]}
      >
        <div className={styles.container}>
          <Spin
            spinning={loading}
            size="large"
            tip={$i18n.get({
              id: 'main.pages.Setting.ModelService.Detail.loading',
              dm: '加载中...',
            })}
          >
            <div style={{ minHeight: '400px' }}></div>
          </Spin>
        </div>
      </InnerLayout>
    );
  }

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.Setting.ModelService.Detail.modelServiceManagement',
            dm: '模型服务管理',
          }),
          path: `/setting/modelService`,
        },
        {
          title:
            provider?.name ||
            $i18n.get({
              id: 'main.pages.Setting.ModelService.Detail.modelServiceDetail',
              dm: '模型服务详情',
            }),
        },
      ]}
      loading={modelLoading}
    >
      <div className={styles.container}>
        <div className={styles.info}>
          <div className={styles.title}>
            {$i18n.get({
              id: 'main.pages.Setting.ModelService.Detail.serviceConfiguration',
              dm: '服务配置',
            })}
          </div>
          <ProviderInfoForm
            provider={provider}
            providerId={id || ''}
            onRefresh={fetchProviderDetail}
          />
        </div>
        <div className={styles['table-container']}>
          <div className={styles['table-header']}>
            <div className={styles['table-title']}>
              <span>
                {$i18n.get({
                  id: 'main.pages.Setting.ModelService.Detail.modelConfiguration',
                  dm: '模型配置',
                })}
              </span>
              <span className={styles.count}>
                （{models.length}
                {$i18n.get({
                  id: 'main.pages.Setting.ModelService.Detail.models',
                  dm: '个模型',
                })}
                ）
              </span>
            </div>
            <Button type="link" onClick={handleAddModel}>
              {$i18n.get({
                id: 'main.pages.Setting.ModelService.Detail.addModel',
                dm: '新增模型',
              })}
            </Button>
          </div>
          {models.length > 0 ? (
            <Table
              columns={columns}
              dataSource={models}
              rowKey="model_id"
              pagination={false}
              className={styles['table']}
            />
          ) : (
            <Empty
              description={$i18n.get({
                id: 'main.pages.Setting.ModelService.Detail.noModel',
                dm: '暂无模型',
              })}
            />
          )}
        </div>
      </div>

      <ModelConfigModal
        open={isModelConfigModalVisible}
        onCancel={() => setIsModelConfigModalVisible(false)}
        onOk={handleModelConfigSubmit}
        model={currentModel}
      />
    </InnerLayout>
  );
};

export default ModelServiceDetail;
