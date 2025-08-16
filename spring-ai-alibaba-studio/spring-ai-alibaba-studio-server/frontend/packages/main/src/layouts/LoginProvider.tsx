import { getAccountInfo } from '@/services/account';
import { useRequest } from 'ahooks';
import { Spin } from 'antd';
import { history } from 'umi';

export default function (props: {
  children: React.ReactNode | React.ReactNode[];
}) {
  const { loading } = useRequest(getAccountInfo, {
    onSuccess(res) {
      window.g_config.user = res.data;
    },
    onError() {
      if (new URL(window.location.href).searchParams.get('ignore-login'))
        return;
      history.replace('/login');
    },
  });

  if (loading)
    return (
      <div className="loading-center">
        <Spin />
      </div>
    );

  return props.children;
}
