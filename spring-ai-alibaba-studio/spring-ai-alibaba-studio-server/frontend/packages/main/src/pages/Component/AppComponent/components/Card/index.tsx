import ProCard from '@/components/Card/ProCard';
import $i18n from '@/i18n';
import { IAppComponentListItem } from '@/types/appComponent';
import { Button, Dropdown, IconButton } from '@spark-ai/design';
import dayjs from 'dayjs';
import React, { useMemo } from 'react';
import { APP_ICON_IMAGE } from '../AppSelector';
import styles from './index.module.less';
export interface AppComponentCardProps extends IAppComponentListItem {
  onClickAction: (key: string) => void;
}

const AppComponentCard: React.FC<AppComponentCardProps> = ({
  code,
  name,
  gmt_modified,
  type,
  onClickAction,
  description,
}) => {
  const updateTime = useMemo(() => {
    return dayjs(gmt_modified).format('YYYY-MM-DD HH:mm:ss');
  }, [gmt_modified]);

  return (
    <ProCard
      title={name!}
      info={[
        {
          label: $i18n.get({
            id: 'main.pages.Component.AppComponent.components.Card.index.componentDescription',
            dm: '组件描述',
          }),
          content: description,
        },
        {
          label: $i18n.get({
            id: 'main.pages.Component.AppComponent.components.Card.index.componentId',
            dm: '组件ID',
          }),
          content: code,
        },
      ]}
      logo={<img className={styles['logo']} src={APP_ICON_IMAGE[type!]} />}
      onClick={() => onClickAction('detail')}
      footerDescNode={
        <div className={styles.bottom}>
          {$i18n.get({
            id: 'main.pages.Component.AppComponent.components.Card.index.updatedAt',
            dm: '更新于',
          })}
          {updateTime}
        </div>
      }
      footerOperateNode={
        <>
          <Button
            type="primary"
            className="flex-1"
            onClick={() => onClickAction('edit')}
          >
            {$i18n.get({
              id: 'main.pages.Component.AppComponent.components.Card.index.edit',
              dm: '编辑',
            })}
          </Button>
          <Button className="flex-1" onClick={() => onClickAction('gotoApp')}>
            {$i18n.get({
              id: 'main.pages.Component.AppComponent.components.Card.index.viewOriginalApplication',
              dm: '查看原应用',
            })}
          </Button>
          <Dropdown
            getPopupContainer={(ele) => ele}
            menu={{
              items: [
                {
                  label: $i18n.get({
                    id: 'main.pages.Component.AppComponent.components.Card.index.componentReferenceDetails',
                    dm: '组件引用详情',
                  }),
                  key: 'detail',
                  onClick: () => onClickAction('referDetail'),
                },
                {
                  label: $i18n.get({
                    id: 'main.pages.Component.AppComponent.components.Card.index.delete',
                    dm: '删除',
                  }),
                  key: 'delete',
                  danger: true,
                  onClick: () => onClickAction('delete'),
                },
              ],
            }}
          >
            <IconButton shape="default" icon="spark-more-line" />
          </Dropdown>
        </>
      }
    />
  );
};

export default AppComponentCard;
