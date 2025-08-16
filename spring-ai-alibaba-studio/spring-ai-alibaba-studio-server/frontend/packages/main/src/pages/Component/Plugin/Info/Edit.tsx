import { getPlugin } from '@/services/plugin';
import { useRequest } from 'ahooks';
import { useParams } from 'umi';
import Index from './index';

export default function () {
  const { id = '' } = useParams<{ id: string }>();
  const { data } = useRequest(() => getPlugin(id));
  const pluginData = data?.data || undefined;
  if (!pluginData) return null;

  return <Index isCreate={false} key={id} pluginData={pluginData} />;
}
