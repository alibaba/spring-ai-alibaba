import styles from './index.module.less';

export default function (props: { title: string; children: React.ReactNode }) {
  return (
    <div className={styles.field}>
      <div className={styles.title}>{props.title}</div>
      <div className={styles.children}>{props.children}</div>
    </div>
  );
}
