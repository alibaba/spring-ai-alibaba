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
import { Layout, Menu } from 'antd';
import { Outlet, useNavigate, useLocation } from 'ice';
import styles from './layout.module.css';

import { SubMenuItem } from '@/types/menu';
import chatModelsService from '@/services/chat_models';
import chatClientsService from '@/services/chat_clients';

export default function PageLayout() {
  const { Content, Sider } = Layout;
  const navigate = useNavigate();
  const location = useLocation();

  const [runMenu, setRunMenu] = useState<SubMenuItem[]>([
    {
      key: '/run/clients',
      label: 'Chat Client',
      children: [],
    },
    {
      key: '/run/models',
      label: 'Chat Model',
      children: [],
    },
    {
      key: '/run/prompts',
      label: 'Prompts',
      children: [],
    },
  ]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const results = await Promise.all([
          chatModelsService.getChatModels(),
          chatClientsService.getChatClients(),
        ]);
        const [chatModelList, chatClientList] = results;

        // 更新runMenu的children
        setRunMenu((prevRunMenu) => {
          const updatedRunMenu = [...prevRunMenu];

          // 组装 ChatClient 目录
          updatedRunMenu[0].children = chatClientList.map((client) => ({
            key: `/run/clients/${client.name}`,
            label: client.name,
          }));

          // 组装 ChatModel 目录
          updatedRunMenu[1].children = chatModelList.map((model) => ({
            key: `/run/models/${model.name}`,
            label: model.name,
          }));

          // todo 组装xxx目录
          return updatedRunMenu;
        });
      } catch (error) {
        console.error('Failed to fetch chat models: ', error);
      }
    };
    fetchData();
  }, []);

  const [selectedKey, setSelectedKey] = useState(
    `/${location.pathname}` || runMenu[0].key);

  const onMenuClick = (e) => {
    navigate(e.key);
    setSelectedKey(e.key);
  };

  useEffect(() => {
    if (location.pathname === '/run') {
      navigate(runMenu[0].key);
      setSelectedKey(runMenu[0].key);
    } else {
      setSelectedKey(location.pathname);
    }
  }, [location, runMenu]);

  return (
    <Layout className={styles.container}>
      <Sider width={200}>
        <Menu
          mode="inline"
          style={{ height: '100%', borderRight: 0 }}
          items={runMenu}
          onClick={onMenuClick}
          selectedKeys={[selectedKey]}
        />
      </Sider>
      <Content>
        <Outlet />
      </Content>
    </Layout>
  );
}
