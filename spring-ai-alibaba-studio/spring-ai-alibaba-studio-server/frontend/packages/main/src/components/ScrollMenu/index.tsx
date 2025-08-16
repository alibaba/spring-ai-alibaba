import classNames from 'classnames';
import styles from './index.module.less';

interface IProps {
  menus: Array<string>;
  handleMenuClick: (val: number) => void;
  activeMenuCode: number;
}

export default function ScrollMenu({
  menus,
  handleMenuClick,
  activeMenuCode,
}: IProps) {
  return (
    <div className={styles.content}>
      {menus.map((menuItem, index) => (
        <div
          onClick={() => handleMenuClick(index)}
          key={index}
          className={classNames(styles['step-item'], {
            [styles['active']]: index === activeMenuCode,
          })}
        >
          <span />
          {menuItem}
        </div>
      ))}
    </div>
  );
}
