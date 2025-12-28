import $i18n from '@/i18n';
import { Button, IconFont, Modal } from '@spark-ai/design';
import { useMount, useUnmount } from 'ahooks';
import { Divider, Flex } from 'antd';
import classNames from 'classnames';
import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './index.module.less';

interface IProps {
  onClose: () => void;
  onClickAction: (val: string) => void;
  disableShowChannel?: boolean;
}

export default function PublishAppSuccessModal(props: IProps) {
  const [count, setCount] = useState(5);
  const countRef = useRef(5);
  const timerRef = useRef(null as any);
  const navigate = useNavigate();
  const clearTimer = () => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  };

  useMount(() => {
    clearTimer();
    timerRef.current = setInterval(() => {
      if (countRef.current <= 0) {
        clearTimer();
        props.onClose();
      }
      countRef.current--;
      setCount(countRef.current);
    }, 1000);
  });

  useUnmount(() => {
    clearTimer();
  });
  return (
    <Modal
      onCancel={props.onClose}
      className={styles.successModal}
      open
      footer={
        <Flex className="w-full" justify="flex-end">
          <Button
            onClick={() => {
              props.onClose();
              navigate('/app');
            }}
          >
            {$i18n.get(
              {
                id: 'main.components.PublishAppSuccessModal.index.notConfigureReturn',
                dm: '暂不配置，返回应用列表（{var1}S）',
              },
              { var1: count },
            )}
          </Button>
        </Flex>
      }
      title={$i18n.get({
        id: 'main.components.PublishAppSuccessModal.index.appPublished',
        dm: '应用发布成功！',
      })}
      width={600}
    >
      <Flex align="center" vertical>
        <Flex className={styles.successCon} gap={12} vertical>
          <Flex align="center" gap={8} className="mt-[18px]">
            <img
              className="w-[60px] h-[60px]"
              src={'/images/congratulations.png'}
            />
          </Flex>
        </Flex>
        <div className={styles.shortTip}>
          <Divider>
            {$i18n.get({
              id: 'main.components.PublishAppSuccessModal.index.quickActions',
              dm: '你还可以进行以下操作',
            })}
          </Divider>
        </div>
        <Flex
          align="center"
          vertical
          gap={16}
          className={classNames(styles.actions, 'w-full')}
        >
          <div className={classNames(styles.actionCard)}>
            <div className={styles.imgCon}>
              <IconFont className={styles.icon} type="spark-plugin-line" />
            </div>
            <div className="flex-1">
              <div className={styles.title}>
                {$i18n.get({
                  id: 'main.components.PublishAppSuccessModal.index.publishAppComponent',
                  dm: '发布应用组件',
                })}
              </div>
              <div className={styles.desc}>
                {$i18n.get({
                  id: 'main.components.PublishAppSuccessModal.index.publishAsAppComponent',
                  dm: '发布为应用组件，方便在智能体或工作流中直接调用！',
                })}
              </div>
            </div>
            <Button
              className="font-medium"
              type="text"
              onClick={() => props.onClickAction('comp')}
            >
              <span>
                {$i18n.get({
                  id: 'main.components.PublishAppSuccessModal.index.immediatelyPublish',
                  dm: '立即发布',
                })}
              </span>
              <IconFont type="spark-rightArrow-line" />
            </Button>
          </div>
        </Flex>
      </Flex>
    </Modal>
  );
}
