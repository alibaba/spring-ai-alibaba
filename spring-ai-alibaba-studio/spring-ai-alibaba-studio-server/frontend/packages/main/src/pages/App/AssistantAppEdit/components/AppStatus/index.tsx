import $i18n from '@/i18n';
import { AppStatus, IAppStatus } from '@/types/appManage';
import { EllipsisTip, Tag } from '@spark-ai/design';
import { Flex } from 'antd';
import styles from './index.module.less';

enum AppStatusColor {
  draft = '',
  published = 'blue',
  published_editing = 'blue',
}

interface IProps {
  appStatus: IAppStatus;
  autoSaveTime: string;
}

export default function AppStatusBar(props: IProps) {
  const appStatus = props.appStatus;
  const autoSaveTime = props.autoSaveTime;
  return (
    <Flex className={styles.appStatus} align="center">
      <Tag color={AppStatusColor[appStatus]}>{AppStatus[appStatus]}</Tag>

      {!!autoSaveTime && (
        <div
          className={'text-[12px] leading-[32px]'}
          style={{ color: 'var(--ag-ant-color-text-quaternary)' }}
        >
          <EllipsisTip
            tooltip={$i18n.get(
              {
                id: 'main.pages.App.AssistantAppEdit.components.AppStatus.index.autosavedOn',
                dm: '已于{var1}自动保存',
              },
              { var1: autoSaveTime },
            )}
          >
            {$i18n.get(
              {
                id: 'main.pages.App.AssistantAppEdit.components.AppStatus.index.autosavedOn',
                dm: '已于{var1}自动保存',
              },
              { var1: autoSaveTime },
            )}
          </EllipsisTip>
        </div>
      )}
    </Flex>
  );
}
