import ProCard from '@/components/Card/ProCard';
import $i18n from '@/i18n';
import { IProvider } from '@/types/modelService';
import { Button, Dropdown, IconButton, Tag } from '@spark-ai/design';
import dayjs from 'dayjs';
import { ProviderAvatar } from '../ProviderAvatar';
import styles from './index.module.less';
interface ModelServiceCardProps {
  service: IProvider;
  onClick?: (action?: string, data?: IProvider) => void;
}

const ModelServiceCard = ({ service, onClick }: ModelServiceCardProps) => {
  const color = service.enable ? 'success' : 'error';
  const text = service.enable
    ? $i18n.get({
        id: 'main.pages.Setting.ModelService.components.ModelServiceCard.index.started',
        dm: '已启动',
      })
    : $i18n.get({
        id: 'main.pages.Setting.ModelService.components.ModelServiceCard.index.stopped',
        dm: '已停用',
      });
  const updatedAt = service.gmt_modified
    ? dayjs(service.gmt_modified).format('YYYY-MM-DD HH:mm:ss')
    : '';

  const handleButtonClick = (action: string, e: React.MouseEvent) => {
    e.stopPropagation();
    onClick?.(action, service);
  };

  const handleDropdownClick = (info: { key: string }) => {
    onClick?.(info.key, service);
  };

  const renderActions = () => {
    const menuItems: {
      key: string;
      label: React.ReactNode;
      danger?: boolean;
    }[] = [];

    if (service.source !== 'preset') {
      menuItems.push({
        key: 'delete',
        label: $i18n.get({
          id: 'main.pages.Setting.ModelService.components.ModelServiceCard.index.delete',
          dm: '删除',
        }),
        danger: true,
      });
    }

    return (
      <>
        {service.enable ? (
          <Button
            className="flex-1"
            type="primary"
            onClick={(e) => handleButtonClick('stop', e)}
          >
            {$i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelServiceCard.index.stopService',
              dm: '停止服务',
            })}
          </Button>
        ) : (
          <Button
            className="flex-1"
            type="primary"
            onClick={(e) => handleButtonClick('start', e)}
          >
            {$i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelServiceCard.index.startService',
              dm: '启动服务',
            })}
          </Button>
        )}
        <Button
          className="flex-1"
          type="default"
          onClick={(e) => handleButtonClick('edit', e)}
        >
          {$i18n.get({
            id: 'main.pages.Setting.ModelService.components.ModelServiceCard.index.editService',
            dm: '编辑服务',
          })}
        </Button>
        {menuItems.length > 0 && (
          <Dropdown
            trigger={['click']}
            menu={{ items: menuItems, onClick: handleDropdownClick }}
          >
            <div onClick={(e) => e.stopPropagation()}>
              <IconButton shape="default" icon="spark-more-line" />
            </div>
          </Dropdown>
        )}
      </>
    );
  };

  const model_count = service.model_count?.toString() || '0';

  return (
    <ProCard
      title={service.name}
      logo={
        <ProviderAvatar
          provider={service}
          className={styles['provider-avatar']}
        />
      }
      statusNode={
        <div className={styles['status-tag']} data-color={color}>
          <span className={styles.dot}></span>
          <span>{text}</span>
        </div>
      }
      info={[
        {
          label: $i18n.get({
            id: 'main.pages.Setting.ModelService.components.ModelServiceCard.index.modelCount',
            dm: '模型数量',
          }),
          content: $i18n.get(
            {
              id: 'main.pages.Setting.ModelService.components.ModelServiceCard.index.numberOfModels',
              dm: '{var1}个',
            },
            { var1: model_count },
          ),
        },
        {
          label: $i18n.get({
            id: 'main.pages.Setting.ModelService.components.ModelServiceCard.index.modelType',
            dm: '模型类型',
          }),
          content: (
            <div className={styles['model-type']}>
              {service.supported_model_types?.map(
                (capability: string, index: number) => (
                  <Tag className={styles['type-tag']} key={index} color="mauve">
                    {capability}
                  </Tag>
                ),
              )}
            </div>
          ),
        },
      ]}
      footerDescNode={
        <div className={styles['footer-desc-node']}>
          {$i18n.get({
            id: 'main.pages.Setting.ModelService.components.ModelServiceCard.index.updatedAt',
            dm: '更新于',
          })}
          {updatedAt}
        </div>
      }
      footerOperateNode={renderActions()}
      className={styles['service-card']}
      onClick={() => onClick?.('detail', service)}
    ></ProCard>
  );
};

export default ModelServiceCard;
