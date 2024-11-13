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

import { Layout, Menu, Flex, Button } from "antd";
import { Outlet, useNavigate } from "ice";
import styles from "./layout.module.css";

export default function PageLayout() {
  const { Header } = Layout;
  const navigate = useNavigate();

  const headerMenu = [
    {
      key: "run",
      label: "运行"
    },
    {
      key: "history",
      label: "历史"
    },
    {
      key: "evaluate",
      label: "评估"
    }
  ];

  const onMenuClick = (e) => {
    navigate(e.key);
  };

  return (
    <Layout>
      <Header className={styles.header}>
        <Flex>
          <span>alibaba-studio</span>
          <Menu
            mode="horizontal"
            defaultSelectedKeys={[headerMenu[0].key]}
            items={headerMenu}
            onClick={onMenuClick}
          />
        </Flex>
        <Flex>
          <Button color="default" variant="link">
            Github
          </Button>
          <Button color="default" variant="link">
            官方文档
          </Button>
          <Button color="default" variant="link">
            切换语言
          </Button>
        </Flex>
      </Header>
      <div className={styles.body}>
        <Outlet />
      </div>
    </Layout>
  );
}
