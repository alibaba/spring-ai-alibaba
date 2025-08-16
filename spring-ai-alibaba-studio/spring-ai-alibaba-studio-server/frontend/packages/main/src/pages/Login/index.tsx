import Header from '@/layouts/Header';
import LangSelect from '@/layouts/LangSelect';
import PureLayout from '@/layouts/Pure';
import ThemeSelect from '@/layouts/ThemeSelect';
import { authLogin } from '@/services/login';
import { useRequest } from 'ahooks';
import React from 'react';
import { history } from 'umi';
import Login from './components/Login';
import styles from './index.module.less';

const LoginPage: React.FC = () => {
  const { loading, runAsync } = useRequest((data) => authLogin(data), {
    manual: true,
  });

  const onLogin = (data: any) => {
    runAsync(data).then(() => {
      history.replace('/');
    });
  };

  return (
    <PureLayout>
      <Header
        right={
          <>
            <ThemeSelect />
            <LangSelect />
          </>
        }
      />
      <div className={styles['container']}>
        <Login onSubmit={onLogin} loading={loading} />
      </div>
    </PureLayout>
  );
};

export default LoginPage;
