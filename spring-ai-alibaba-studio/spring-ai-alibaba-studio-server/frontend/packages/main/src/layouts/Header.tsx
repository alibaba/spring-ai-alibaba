import { getCommonConfig } from '@spark-ai/design';
import { history } from 'umi';
import styles from './index.module.less';

type TChildren = React.ReactNode | React.ReactNode[];

export default function (props: {
  logo?: string;
  children?: TChildren;
  right?: TChildren;
}) {
  const darkMode = getCommonConfig().isDarkMode;

  return (
    <div className={styles['header']}>
      <img
        className={styles['header-logo']}
        onClick={() => history.push('/')}
        src={darkMode ? '/images/logoBlack.png' : '/images/logoWhite.png'}
      />
      {props.children}
      <div className={styles['header-right']}>{props.right}</div>
    </div>
  );
}
