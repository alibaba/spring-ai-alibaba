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

import { useEffect, useState } from "react";
import { Layout, Menu } from "antd";
import { Outlet, useNavigate, useLocation } from "ice";
import styles from "./layout.module.css";

export default function PageLayout() {
  const { Content, Sider } = Layout;
  const navigate = useNavigate();
  const location = useLocation();

  const runMenu = [
    {
      key: "/run/clients",
      label: "ChatClient"
    },
    {
      key: "/run/models",
      label: "Chat Model",
      children: [
        {
          key: "/run/models/chatModel",
          label: "Chat Model 1"
        },
        {
          key: "/run/models/imageModel",
          label: "Image Model 2"
        }
      ]
    }
  ];

  const [selectedKey, setSelectKey] = useState(runMenu[0].key);

  const onMenuClick = (e) => {
    navigate(e.key);
    setSelectKey(e.key);
  };

  useEffect(() => {
    if (location.pathname === "/run") {
      navigate("/run/clients");
      setSelectKey(runMenu[0].key);
    }
  }, [location]);

  return (
    <Layout className={styles.container}>
      <Sider width={200}>
        <Menu
          mode="inline"
          style={{ height: "100%", borderRight: 0 }}
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
