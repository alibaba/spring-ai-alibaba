import CardList from '@/components/Card/List';
import $i18n from '@/i18n';
import { listPlugin } from '@/services/plugin';
import { IconFont } from '@spark-ai/design';
import { useRequest, useSetState } from 'ahooks';
import { Button } from 'antd';
import { history } from 'umi';
import PluginCard from './Card';

export default function () {
  const [params, setParams] = useSetState({ current: 1, size: 50 });

  const {
    data,
    loading,
    run: reload,
  } = useRequest(() => listPlugin(params), {
    refreshDeps: [params],
  });

  const list = data?.data?.records || [];

  return (
    <CardList
      className="pt-[20px]"
      loading={loading}
      emptyAction={
        <Button
          type="primary"
          onClick={() => history.push('/component/plugin/create')}
          icon={<IconFont type="spark-plus-line" />}
        >
          {$i18n.get({
            id: 'main.pages.Component.Plugin.List.List.addCustomPlugin',
            dm: '新增自定义插件',
          })}
        </Button>
      }
      pagination={{
        current: params.current,
        total: data?.data.total,
        pageSize: params.size,
        onChange: (current, size) => {
          setParams({
            current,
            size,
          });
        },
      }}
    >
      {list.map((item) => (
        <PluginCard reload={reload} key={item.plugin_id} {...item} />
      ))}
    </CardList>
  );
}
