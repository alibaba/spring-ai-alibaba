import { IconFont } from '@spark-ai/design';
import { Typography } from 'antd';
import classNames from 'classnames';
import React from 'react';
import styles from './index.module.less';

/**
 * Card component props interface
 */
interface CardProps {
  /**
   * Custom className to override default styles
   */
  className?: string;
  /**
   * Click event handler
   */
  onClick?: () => void;
  logo?: string | React.ReactNode;
  title: string;
  statusNode?: React.ReactNode;
  info: Array<{ label: string; content: string | React.ReactNode }>;
  footerDescNode: React.ReactNode;
  footerOperateNode: React.ReactNode;
  labelWidth?: number;
}

/**
 * Card component
 *
 * Pure card component with only common styling, no content included
 */
const ProCard: React.FC<CardProps> = (props) => {
  const { className, onClick } = props;

  return (
    <div className={classNames(styles['card'], className)} onClick={onClick}>
      <div className={'flex justify-between items-center gap-2'}>
        <div className="flex items-center gap-2 flex-1">
          {props.logo && (
            <div className={styles['avatar']}>
              {typeof props.logo === 'string' ? (
                <IconFont
                  type={props.logo}
                  className={styles['workflow-icon']}
                />
              ) : (
                props.logo
              )}
            </div>
          )}
          <Typography.Text
            ellipsis={{ tooltip: props.title }}
            className={classNames(styles['title'], 'flex-1 w-1')}
          >
            {props.title}
          </Typography.Text>
        </div>
        <div className="flex-shrink-0">{props.statusNode}</div>
      </div>
      <div className={'mt-3'}>
        {props.info.map((item, index) => (
          <div key={index} className={'flex gap-2 items-center'}>
            {item.label && (
              <div
                style={{ width: props.labelWidth }}
                className={classNames(styles['info-label'])}
              >
                {item.label}
              </div>
            )}
            <div className={styles['info-content']}>{item.content}</div>
          </div>
        ))}
      </div>
      <div className={styles['card-footer']}>{props.footerDescNode}</div>
      <div
        onClick={(e) => e.stopPropagation()}
        className={styles['operate-container']}
      >
        {props.footerOperateNode}
      </div>
    </div>
  );
};

export default ProCard;
