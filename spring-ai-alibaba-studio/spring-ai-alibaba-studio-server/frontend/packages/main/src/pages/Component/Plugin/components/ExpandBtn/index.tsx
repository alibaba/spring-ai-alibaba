import { IconButton } from '@spark-ai/design';
import classNames from 'classnames';
import styles from './index.module.less';

interface IProps {
  expand: boolean;
  setExpand: (val: boolean) => void;
}

export function RightExpandBtn(props: IProps) {
  return (
    <IconButton
      className={classNames(styles.expandBtn, {
        [styles.expand]: props.expand,
      })}
      onClick={() => props.setExpand(!props.expand)}
      icon="bl-icon-right-line"
    />
  );
}

export function TopExpandBtn(props: IProps) {
  return (
    <IconButton
      onClick={() => props.setExpand(!props.expand)}
      className={classNames(styles.expandBtn, styles.top, {
        [styles.expand]: props.expand,
      })}
      icon="bl-icon-up-line"
      shape="circle"
    />
  );
}
