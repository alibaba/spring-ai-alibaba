import { iconFontUrl } from '@/components/Icon';
import $i18n from '@/i18n';
import { getGlobalConfig } from '@/services/globalConfig';
import {
  ConfigProvider,
  Empty,
  purpleDarkTheme,
  purpleTheme,
} from '@spark-ai/design';
import { useRequest } from 'ahooks';
import { Flex, theme } from 'antd';
import enUS from 'antd/locale/en_US';
import jaJP from 'antd/locale/ja_JP';
import zhCN from 'antd/locale/zh_CN';
import 'dayjs/locale/zh-cn';
import { ErrorBoundary } from 'react-error-boundary';
import styles from './index.module.less';
import { prefersColor } from './ThemeSelect';

// Get current language preset
const langPreset = $i18n.getCurrentLanguage();

/**
 * Pure layout component
 * Provides theme configuration and basic layout structure
 */
export default function PureLayout(props: {
  children: React.ReactNode | React.ReactNode[];
}) {
  // Check for dark mode preference
  const darkMode = prefersColor.get() === 'dark';
  // Set locale based on current language
  const locale = {
    zh: zhCN,
    en: enUS,
    ja: jaJP,
  }[langPreset];

  // Select theme based on dark mode
  const inputTheme = (darkMode ? purpleDarkTheme : purpleTheme).theme;
  const { loading } = useRequest(getGlobalConfig);

  if (loading) return null;

  return (
    <ErrorBoundary
      FallbackComponent={(...args) => {
        return <h1> something error </h1>;
      }}
    >
      <ConfigProvider
        {...purpleTheme}
        button={{
          autoInsertSpace: false,
        }}
        theme={{
          ...inputTheme,
          algorithm: darkMode ? theme.darkAlgorithm : theme.defaultAlgorithm,
          cssVar: { prefix: 'ag-ant' },
          hashed: false,
        }}
        getPopupContainer={() =>
          document.querySelector('#root .ag-ant-app') as HTMLElement
        }
        prefix="ag"
        prefixCls="ag-ant"
        iconfont={iconFontUrl}
        locale={locale}
        renderEmpty={() => {
          return (
            <Flex justify="center">
              <Empty description={locale?.Empty?.description} />
            </Flex>
          );
        }}
      >
        <div className={styles['main']}>{props.children}</div>
      </ConfigProvider>
    </ErrorBoundary>
  );
}
