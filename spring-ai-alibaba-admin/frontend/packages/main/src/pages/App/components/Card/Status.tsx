import $i18n from '@/i18n';
import { IAppStatus } from '@/types/appManage';
import React from 'react';
import styles from './Status.module.less';

interface StatusProps {
  status: IAppStatus;
}

const Status: React.FC<StatusProps> = ({ status }) => {
  const getStatusConfig = () => {
    switch (status) {
      case 'published':
        return {
          text: $i18n.get({
            id: 'main.pages.App.components.Card.Status.published',
            dm: '已发布',
          }),
          className: styles.published,
        };
      case 'published_editing':
        return {
          text: $i18n.get({
            id: 'main.pages.App.components.Card.Status.publishedEditing',
            dm: '已发布编辑中',
          }),
          className: styles.editing,
        };
      case 'draft':
        return {
          text: $i18n.get({
            id: 'main.pages.App.components.Card.Status.draft',
            dm: '草稿',
          }),
          className: styles.draft,
        };
      default:
        return {
          text: '',
          className: '',
        };
    }
  };

  const config = getStatusConfig();

  return (
    <div className={`${styles.status} ${config.className}`}>
      <div className={styles.dot} />
      <span className={styles.text}>{config.text}</span>
    </div>
  );
};

export default Status;
