/**
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useEffect, useState } from 'react';
import { Layout, Menu, Flex, Button, Radio } from 'antd';
import type { RadioChangeEvent } from 'antd';
import { Outlet, useNavigate, useLocation } from 'ice';
import styles from './layout.module.css';
import { useTranslation } from 'react-i18next';
import {
  CloudTwoTone,
  CodeTwoTone,
  ExperimentTwoTone,
} from '@ant-design/icons';

export default function PageLayout() {
  const { t, i18n } = useTranslation();
  const { Header } = Layout;
  const navigate = useNavigate();
  const location = useLocation();

  const [language, setLanguage] = useState('');

  const languageOptions = [
    {
      value: 'zh',
      label: t('chinese'),
    },
    {
      value: 'en',
      label: t('english'),
    },
  ];

  const headerMenu = [
    {
      key: '/run',
      icon: <CodeTwoTone />,
      label: t('run'),
    },
    {
      key: '/traces',
      icon: <CloudTwoTone />,
      label: t('traces'),
    },
    {
      key: '/evaluate',
      icon: <ExperimentTwoTone />,
      label: t('evaluate'),
    },
  ];

  const [selectedKey, setSelectedKey] = useState(
    `/${location.pathname.split('/')[1]}` || headerMenu[0].key,
  );

  const onMenuClick = (e) => {
    setSelectedKey(e.key);
    navigate(e.key);
  };

  const onLanguageChange = ({ target: { value } }: RadioChangeEvent) => {
    setLanguage(value);
    i18n.changeLanguage(value);
  };

  useEffect(() => {
    if (location.pathname === '/') {
      navigate(headerMenu[0].key);
      setSelectedKey(headerMenu[0].key);
    }
  }, [location, navigate]);

  useEffect(() => {
    setLanguage(i18n.language);
  }, [i18n.language]);

  return (
    <Layout>
      <Header className={styles.header}>
        <span>alibaba-ai-studio</span>
        <Flex justify={'center'}>
          <Menu
            style={{ minWidth: 300 }}
            mode="horizontal"
            selectedKeys={[selectedKey]}
            items={headerMenu}
            onClick={onMenuClick}
          />
        </Flex>
        <Flex>
          <Button color="default" variant="link">
            {t('github')}
          </Button>
          <Button color="default" variant="link">
            {t('document')}
          </Button>
          <Radio.Group
            options={languageOptions}
            onChange={onLanguageChange}
            value={language}
            optionType="button"
            buttonStyle="solid"
          />
        </Flex>
      </Header>
      <div className={styles.body}>
        <Outlet />
      </div>
    </Layout>
  );
}
