import $i18n from '@/i18n';
import {
  getAppVersionDetail,
  getAppVersionList,
  updateApp,
} from '@/services/appManage';
import { IAppDetail, IAppVersion } from '@/types/appManage';
import {
  AlertDialog,
  Button,
  IconFont,
  message,
  parseJsonSafely,
  renderTooltip,
} from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { Flex, Timeline, Typography } from 'antd';
import classNames from 'classnames';
import dayjs from 'dayjs';
import { useCallback, useState } from 'react';
import useBus from 'use-bus';
import styles from './index.module.less';

interface ITimelineItem {
  selected: boolean;
  onSelect: (version: string, index?: number) => void;
  versionConfig?: IAppVersion;
  version: string;
  disabled?: boolean;
  onEditVersionDesc?: (version: string) => void;
  index?: number;
}
const TimelineItem = (props: ITimelineItem) => {
  const { selected, onSelect, version, versionConfig, disabled, index } = props;
  const [showMore, setShowMore] = useState(false);

  return (
    <div
      className={classNames(styles.timelineItemWrapper, {
        [styles.selected]: selected,
        [styles.disabled]: disabled,
        [styles.hover]: showMore,
      })}
      onClick={() => {
        if (version === 'draft') return onSelect('draft');
        onSelect(version, index);
      }}
      onMouseOver={() => {
        setShowMore(true);
      }}
      onMouseLeave={() => {
        setShowMore(false);
      }}
    >
      {version === 'draft' ? (
        <div style={{ position: 'relative' }}>
          {$i18n.get({
            id: 'main.components.HistoryPanel.index.currentDraft',
            dm: '当前草稿',
          })}
        </div>
      ) : (
        versionConfig && (
          <div style={{ position: 'relative' }}>
            <Flex justify="space-between" align="center">
              <Flex gap={4} justify="left">
                {index === 0 ? (
                  <div className={styles.timelineTitle}>
                    {$i18n.get({
                      id: 'main.components.HistoryPanel.index.liveVersion',
                      dm: '线上版本',
                    })}
                  </div>
                ) : (
                  <div className={styles.timelineTitle}>
                    {dayjs(versionConfig.gmt_modified).format('YYYY-MM-DD')}
                    {$i18n.get({
                      id: 'main.components.HistoryPanel.index.version',
                      dm: '版本',
                    })}
                  </div>
                )}
                {index === 0 && (
                  <div className={styles.timelineNew}>
                    {$i18n.get({
                      id: 'main.components.HistoryPanel.index.latest',
                      dm: '最新',
                    })}
                  </div>
                )}
              </Flex>
            </Flex>
            <Flex className={styles.desc}>
              {dayjs(versionConfig.gmt_modified).format('YYYY-MM-DD HH:mm:ss')}
            </Flex>
            <Flex className={styles.desc}>
              <span style={{ whiteSpace: 'nowrap' }}>
                {$i18n.get({
                  id: 'main.components.HistoryPanel.index.versionId',
                  dm: '版本ID：',
                })}
              </span>
              {versionConfig.version}
            </Flex>
            <Flex className={styles.desc}>
              <span style={{ whiteSpace: 'nowrap' }}>
                {$i18n.get({
                  id: 'main.components.HistoryPanel.index.publisher',
                  dm: '发布人：',
                })}
              </span>
              <Typography.Text
                ellipsis={{
                  tooltip: renderTooltip(versionConfig.modifier || ''),
                }}
              >
                {versionConfig.modifier}
              </Typography.Text>
            </Flex>
          </div>
        )
      )}
    </div>
  );
};

interface IProps {
  // switch to display the current version
  onSelectVersion: (version: string, index?: number) => void;
  onClose: () => void;
  selectedVersion?: string;
  appDetail: IAppDetail;
  hasInitData?: boolean;
}
export default function HistoryPanelComp(props: IProps) {
  const { selectedVersion = 'draft' } = props;
  const { appDetail, onSelectVersion } = props;
  const [state, setState] = useSetState<{
    versionConfigList: IAppVersion[];
    curConfig: IAppVersion | null;
    pageNo: number;
  }>({
    versionConfigList: [],
    curConfig: null,
    pageNo: 1,
  });

  const refresh = useCallback(() => {
    getAppVersionList({
      app_id: appDetail.app_id,
      current: state.pageNo,
      size: 10,
      status: 'published',
    }).then((res) => {
      setState({ versionConfigList: res.data.records });
    });
  }, [appDetail.app_id]);

  useMount(() => {
    refresh();
  });

  useBus('history-panel-fresh', refresh);

  const handleOverwriteDraft = () => {
    AlertDialog.warning({
      title: $i18n.get({
        id: 'main.components.HistoryPanel.index.confirmUseVersion',
        dm: '是否确认使用此版本内容',
      }),
      children: $i18n.get({
        id: 'main.components.HistoryPanel.index.overwriteDraft',
        dm: '确认后，会使用此版本覆盖当前草稿内容！',
      }),
      onOk: async () => {
        if (!appDetail) {
          return;
        }
        const newConfig =
          parseJsonSafely(
            (await getAppVersionDetail(appDetail.app_id, selectedVersion))?.data
              .config,
          ) || {};
        await updateApp({
          app_id: appDetail.app_id,
          name: appDetail.name,
          description: appDetail.description,
          config: newConfig,
        });

        onSelectVersion('draft');
        refresh();

        const item = state.versionConfigList.find(
          (e) => e.version === selectedVersion,
        );
        if (item) {
          const msg =
            $i18n.get({
              id: 'main.components.HistoryPanel.index.returnedTo',
              dm: '已回到',
            }) +
            dayjs(item.gmt_modified).format('YYYY-MM-DD') +
            $i18n.get({
              id: 'main.components.HistoryPanel.index.version',
              dm: '版本',
            });
          message.success(msg);
        }
      },
    });
  };

  return (
    <Flex className={styles.historyPanelWrapper} vertical>
      <div className={styles.header}>
        <div className={styles.title}>
          {$i18n.get({
            id: 'main.components.HistoryPanel.index.historyVersion',
            dm: '历史版本',
          })}
        </div>
        <IconFont
          type="spark-false-line"
          isCursorPointer
          onClick={() => {
            props.onClose();
          }}
        />
      </div>
      <div className={styles.list}>
        <Timeline
          items={[
            // current draft version
            {
              children: (
                <TimelineItem
                  selected={selectedVersion === 'draft'}
                  onSelect={() => props.hasInitData && onSelectVersion('draft')}
                  version="draft"
                />
              ),

              color:
                selectedVersion === 'draft'
                  ? 'var(--ag-ant-color-primary)'
                  : 'var(--ag-ant-color-text-description)',
            },
            // other published versions
            ...state.versionConfigList.map((config, index) => {
              return {
                children: (
                  <TimelineItem
                    selected={selectedVersion === config.version}
                    onSelect={(version) => {
                      props.hasInitData && onSelectVersion(version, index);
                    }}
                    versionConfig={config}
                    version={config.version}
                    index={index}
                  />
                ),

                color:
                  selectedVersion === config.version
                    ? 'var(--ag-ant-color-primary)'
                    : 'var(--ag-ant-color-text-description)',
              };
            }),
          ]}
        />
      </div>
      <Flex className={styles.footer} align="center">
        <Button
          disabled={selectedVersion === 'draft'}
          type="primary"
          onClick={handleOverwriteDraft}
        >
          {$i18n.get({
            id: 'main.components.HistoryPanel.index.overwriteDraft',
            dm: '覆盖当前草稿',
          })}
        </Button>
      </Flex>
    </Flex>
  );
}
