import $i18n from '@/i18n';
import { Button, Drawer } from '@spark-ai/design';
import styles from './index.module.less';

interface ChunkViewDrawerProps {
  /**
   * Drawer visibility
   * */
  visible: boolean;
  /**
   * Drawer title
   * */
  title: string;
  /**
   * Drawer content
   * */
  content: string;
  /**
   * Callback when the drawer is closed
   * */
  onClose: () => void;
}

export default function ChunkViewDrawer(props: ChunkViewDrawerProps) {
  const { visible, title = '', onClose, content = '' } = props;

  return (
    <Drawer
      open={visible}
      title={$i18n.get(
        {
          id: 'main.pages.Knowledge.Detail.components.ChunkViewDrawer.index.viewSliceVar1',
          dm: '查看切片{var1}',
        },
        { var1: title },
      )}
      onClose={onClose}
      width={480}
      footer={
        <div className={styles.footer}>
          <div>
            {content.length}
            {$i18n.get({
              id: 'main.pages.Knowledge.Detail.components.ChunkViewDrawer.index.character',
              dm: '字符',
            })}
          </div>
          <Button onClick={onClose}>
            {$i18n.get({
              id: 'main.pages.Knowledge.Detail.components.ChunkViewDrawer.index.close',
              dm: '关闭',
            })}
          </Button>
        </div>
      }
    >
      <div
        dangerouslySetInnerHTML={{ __html: content?.replace(/\n/g, '<br/>') }}
        className={styles['content']}
      />
    </Drawer>
  );
}
