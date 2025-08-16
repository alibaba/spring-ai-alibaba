import $i18n from '@/i18n';
import { updateChunksContent } from '@/services/knowledge';
import { Button, Drawer, Input } from '@spark-ai/design';
import type { InputRef } from 'antd';
import { Flex } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import styles from './index.module.less';

interface ChunkEditDrawerProps {
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
  /**
   * Refresh list callback
   *  */
  refreshList?: () => void;
  /**
   * Drawer chunk_id
   * */
  chunk_id?: string;
}

export default function ChunkEditDrawer(props: ChunkEditDrawerProps) {
  const {
    visible,
    title = '',
    onClose,
    content = '',
    refreshList,
    chunk_id = '',
  } = props;
  const { doc_id } = useParams<{
    doc_id: string;
  }>();
  const [text, setText] = useState<string>(content);
  const [loading, setLoading] = useState(false);
  const inputRef = useRef<InputRef>(null);
  const handleUpdate = (): Promise<void> => {
    const params = {
      doc_id: doc_id as string,
      chunk_id: chunk_id,
      text: text,
    };
    setLoading(true);
    return new Promise((resolve, reject) => {
      updateChunksContent(params)
        .then(() => {
          setTimeout(() => {
            refreshList?.();
            resolve();
            onClose();
          }, 1000);
        })
        .catch((err) => {
          reject(err);
        })
        .finally(() => {
          setLoading(false);
        });
    });
  };

  useEffect(() => {
    if (visible && inputRef.current) {
      inputRef.current!.focus({
        preventScroll: true,
      });
    }
  }, [visible]);

  return (
    <Drawer
      open={visible}
      title={$i18n.get(
        {
          id: 'main.pages.Knowledge.Detail.components.ChunkEditDrawer.index.editSliceVar1',
          dm: '编辑切片{var1}',
        },
        { var1: title },
      )}
      onClose={onClose}
      width={480}
      footer={
        <div className={styles.footer}>
          <div>
            {text.length}
            {$i18n.get({
              id: 'main.pages.Knowledge.Detail.components.ChunkEditDrawer.index.character',
              dm: '字符',
            })}
          </div>
          <Flex align="center" gap={12}>
            <Button onClick={onClose}>
              {$i18n.get({
                id: 'main.pages.Knowledge.Detail.components.ChunkEditDrawer.index.cancel',
                dm: '取消',
              })}
            </Button>
            <Button type="primary" onClick={handleUpdate} loading={loading}>
              {$i18n.get({
                id: 'main.pages.Knowledge.Detail.components.ChunkEditDrawer.index.save',
                dm: '保存',
              })}
            </Button>
          </Flex>
        </div>
      }
    >
      <Input.TextArea
        defaultValue={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={$i18n.get({
          id: 'main.pages.Knowledge.Detail.components.ChunkEditDrawer.index.enterContent',
          dm: '请输入内容',
        })}
        style={{ height: '100%', resize: 'none' }}
        ref={inputRef}
      />
    </Drawer>
  );
}
