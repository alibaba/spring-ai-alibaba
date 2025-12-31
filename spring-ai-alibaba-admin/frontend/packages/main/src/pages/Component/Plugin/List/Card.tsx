import ProCard from '@/components/Card/ProCard';
import $i18n from '@/i18n';
import { removePlugin } from '@/services/plugin';
import { Plugin } from '@/types/plugin';
import { AlertDialog, IconButton, message } from '@spark-ai/design';
import { Button, Dropdown } from 'antd';
import dayjs from 'dayjs';
import { history } from 'umi';
import styles from './index.module.less';
export default function PluginCard(props: Plugin & { reload: () => void }) {
  return (
    <ProCard
      title={props.name}
      className={styles['card']}
      info={[
        {
          label: $i18n.get({
            id: 'main.pages.Component.Plugin.List.Card.pluginDescription',
            dm: '描述',
          }),
          content: props.description,
        },
        {
          label: $i18n.get({
            id: 'main.pages.Component.Plugin.List.Card.pluginId',
            dm: 'ID',
          }),
          content: props.plugin_id || '',
        },
      ]}
      logo={<img className={styles['logo']} src={'/images/plugin.svg'} />}
      onClick={() => {}}
      footerDescNode={
        <div className={styles['update-time']}>
          {$i18n.get({
            id: 'main.pages.Component.Plugin.List.Card.updatedAt',
            dm: '更新于',
          })}
          {dayjs(props.gmt_modified).format('YYYY-MM-DD HH:mm:ss')}
        </div>
      }
      footerOperateNode={
        <>
          <Button
            type="primary"
            className="flex-1"
            onClick={() => {
              history.push(`/component/plugin/${props.plugin_id}/tools`);
            }}
          >
            {$i18n.get({
              id: 'main.pages.Component.Plugin.List.Card.viewTools',
              dm: '查看工具',
            })}
          </Button>
          <Button
            className="flex-1"
            onClick={() => {
              history.push(`/component/plugin/${props.plugin_id}`);
            }}
          >
            {$i18n.get({
              id: 'main.pages.Component.Plugin.List.Card.edit',
              dm: '编辑',
            })}
          </Button>

          <Dropdown
            getPopupContainer={(ele) => ele}
            menu={{
              onClick: () => {
                AlertDialog.warning({
                  title: $i18n.get({
                    id: 'main.pages.Component.Plugin.List.Card.deletePlugin',
                    dm: '删除插件',
                  }),
                  children: $i18n.get({
                    id: 'main.pages.Component.Plugin.List.Card.confirmDeletePlugin',
                    dm: '确定删除该插件吗？',
                  }),
                  onOk: () => {
                    removePlugin(props.plugin_id as string).then(() => {
                      message.success(
                        $i18n.get({
                          id: 'main.pages.Component.Plugin.List.Card.successDelete',
                          dm: '删除成功',
                        }),
                      );
                      props.reload();
                    });
                  },
                });
              },
              items: [
                {
                  label: $i18n.get({
                    id: 'main.pages.Component.Plugin.List.Card.delete',
                    dm: '删除',
                  }),
                  key: 'delete',
                  danger: true,
                },
              ],
            }}
          >
            <IconButton shape="default" icon="spark-more-line" />
          </Dropdown>
        </>
      }
    ></ProCard>
  );
}
