import { Card } from '@spark-ai/design';
import React from 'react';
import styles from './index.module.less';

export interface ProCardInfo {
  content: string;
}

export interface ProCardProps {
  title: string;
  logo?: React.ReactNode;
  info?: ProCardInfo[];
  onClick?: () => void;
  className?: string;
  statusNode?: React.ReactNode;
  footerDescNode?: React.ReactNode;
  footerOperateNode?: React.ReactNode;
}

const ProCard: React.FC<ProCardProps> = ({
  title,
  logo,
  info = [],
  onClick,
  className,
  statusNode,
  footerDescNode,
  footerOperateNode,
}) => {
  return (
    <Card
      className={`${styles.proCard} ${className || ''}`}
      onClick={onClick}
      hoverable={!!onClick}
    >
      <div className={styles.cardHeader}>
        <div className={styles.headerLeft}>
          {logo && <div className={styles.logo}>{logo}</div>}
          <div className={styles.titleWrapper}>
            <h3 className={styles.title}>{title}</h3>
            {statusNode && <div className={styles.status}>{statusNode}</div>}
          </div>
        </div>
      </div>

      {info.length > 0 && (
        <div className={styles.cardBody}>
          {info.map((item, index) => (
            <div key={index} className={styles.infoItem}>
              <span className={styles.content}>{item.content}</span>
            </div>
          ))}
        </div>
      )}

      {(footerDescNode || footerOperateNode) && (
        <div className={styles.cardFooter}>
          {footerDescNode && (
            <div className={styles.footerDesc}>{footerDescNode}</div>
          )}
          {footerOperateNode && (
            <div className={styles.footerOperate}>{footerOperateNode}</div>
          )}
        </div>
      )}
    </Card>
  );
};

export default ProCard;
