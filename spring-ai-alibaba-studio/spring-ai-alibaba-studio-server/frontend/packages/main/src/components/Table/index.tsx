import { Table as AntdTable, TableProps } from 'antd';
import classNames from 'classnames';
import styles from './index.module.less';
const Table = <T extends object>(props: TableProps<T>) => {
  const { className, ...restProps } = props;
  return (
    <AntdTable<T>
      className={classNames(styles['table'], className)}
      {...restProps}
    />
  );
};

export default Table;
