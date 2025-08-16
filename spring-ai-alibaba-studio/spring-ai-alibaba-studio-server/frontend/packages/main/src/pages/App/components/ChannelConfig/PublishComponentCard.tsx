import EditDrawer from '@/pages/Component/AppComponent/components/EditDrawer';
import ReferDetailDrawer from '@/pages/Component/AppComponent/components/ReferDetailDrawer';
import {
  getAppComponentDetailByAppCode,
  IAppType,
} from '@/services/appComponent';
import { IAppComponentListItem } from '@/types/appComponent';
import { IconFont } from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { Button, Divider } from 'antd';
import classNames from 'classnames';
import { memo, useEffect } from 'react';
import styles from './index.module.less';

import $i18n from '@/i18n';
import { createEventBus } from '@/pages/App/utils/event-bus';

export const channelConfigEventBus = createEventBus('channel-config');
export default memo(function PublishComponentCard({
  app_id,
  disabled = false,
  type,
}: {
  app_id: string;
  disabled?: boolean;
  type: IAppType;
  showOpenCompCfg?: boolean;
}) {
  const [state, setState] = useSetState({
    referVisible: false,
    editVisible: false,
    createVisible: false,
    detail: null as IAppComponentListItem | null,
  });

  const init = () => {
    getAppComponentDetailByAppCode(app_id).then((res) => {
      if (!res) {
        setState({
          detail: null,
        });
      } else {
        setState({
          detail: res,
        });
      }
    });
  };

  useMount(() => {
    init();
  });

  useEffect(() => {
    channelConfigEventBus.on('openCompCfg', () => {
      setState({ createVisible: true });
    });
    return () => {
      channelConfigEventBus.removeAllListeners();
    };
  }, []);

  return (
    <div
      className={classNames(styles.card, {
        [styles.disabled]: disabled,
      })}
    >
      <div className={styles['img-con']}>
        <img alt="appComponent" src="/images/appComponent.svg" />
      </div>
      <div className="flex flex-col gap-[4px] flex-1">
        <div className={styles.name}>
          {$i18n.get({
            id: 'main.pages.App.components.ChannelConfig.PublishComponentCard.component',
            dm: '组件',
          })}
        </div>
        <div className={styles.desc}>
          {$i18n.get({
            id: 'main.pages.App.components.ChannelConfig.PublishComponentCard.configureInput',
            dm: '以组件的形式发布智能体/工作流应用，需要配置入参，发布后用户可以通过组件的方式在其他应用中黑盒的引用这个智能体/工作流的内容',
          })}
        </div>
      </div>
      {disabled ? null : !state.detail ? (
        <>
          <Button
            onClick={() => {
              setState({ createVisible: true });
            }}
            size="small"
            type="text"
            className="self-center"
            icon={<IconFont type="spark-plus-line" />}
          >
            {$i18n.get({
              id: 'main.pages.App.components.ChannelConfig.PublishComponentCard.create',
              dm: '创建',
            })}
          </Button>
        </>
      ) : (
        <>
          <Button
            onClick={() => setState({ referVisible: true })}
            size="small"
            type="text"
            icon={<IconFont type="spark-projectNo-line" />}
            className="self-center"
          >
            {$i18n.get({
              id: 'main.pages.App.components.ChannelConfig.PublishComponentCard.referenceDetails',
              dm: '组件引用详情',
            })}
          </Button>
          <Divider
            className="self-center"
            type="vertical"
            style={{ margin: 0 }}
          />
          <Button
            onClick={() => setState({ editVisible: true })}
            size="small"
            type="text"
            icon={<IconFont type="spark-edit-line" />}
            className="self-center"
          >
            {$i18n.get({
              id: 'main.pages.App.components.ChannelConfig.PublishComponentCard.edit',
              dm: '编辑',
            })}
          </Button>
        </>
      )}
      {state.referVisible && state.detail && (
        <ReferDetailDrawer
          data={state.detail}
          onClose={() => setState({ referVisible: false })}
        />
      )}
      {state.editVisible && state.detail && (
        <EditDrawer
          onOk={() => {
            setState({ editVisible: false });
          }}
          data={state.detail}
          onClose={() => setState({ editVisible: false })}
        />
      )}
      {state.createVisible && (
        <EditDrawer
          onOk={() => {
            init();
            setState({ createVisible: false });
          }}
          onClose={() => {
            setState({ createVisible: false });
          }}
          data={{
            app_id: app_id,
            type,
          }}
        />
      )}
    </div>
  );
});
