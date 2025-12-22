import { IconFont } from '@spark-ai/design';
import classNames from 'classnames';
import styles from './index.module.less';

interface IProps {
  expand: boolean;
  setExpand: (val: boolean) => void;
  className?: string;
}

export function RightExpandBtn(props: IProps) {
  return (
    <IconFont
      isCursorPointer
      className={classNames(styles.expandBtn, {
        [styles.expand]: props.expand,
      })}
      onClick={() => props.setExpand(!props.expand)}
      type="spark-right-line"
    />
  );
}

export function TopExpandBtn(props: IProps) {
  return (
    <IconFont
      isCursorPointer
      onClick={() => props.setExpand(!props.expand)}
      className={classNames(styles.expandBtn, styles.top, props.className, {
        [styles.expand]: props.expand,
      })}
      type="spark-up-line"
    />
  );
}
