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
import { useParams } from 'ice';
import chatClientsService from '@/services/chat_clients'
import styles from './index.module.css';
import { Card, Input, Divider, Tabs, Form, Button, Select, Slider } from 'antd';

type Params = {
  model_name: string;
};

export default function ChatClient() {
  // 路径参数
  const params = useParams<Params>();

  const [clientData, setChatClientData] = useState<any>();

  useEffect(() => {
    const fetchData = async () => {
      try {
        const chatClientData = await chatClientsService.getChatClientByName(params.model_name);
        setChatClientData(chatClientData);
      } catch (error) {
        console.error("Failed to fetch chat models: ", error);
      }
    };
    fetchData();
  }, [params]);

  const onChange = (key: string) => {
    console.log(key);
  };

  return (
    clientData ? (
      <>
        <div className={styles.container}>
          <div className={styles.left}>
            <Card title="Model" extra={<a href="#">More</a>} style={{ width: '100%' }}>
              <p>{clientData.name}</p>
              <Input placeholder="Basic usage" />
            </Card>
          </div>
          <div className="full-height-container">
            <Divider type="vertical" className="full-height-divider" />
          </div>
          <div className={styles.right}>
            <Card style={{ width: '100%' }}>
              <Tabs
                defaultActiveKey="1"
                onChange={onChange}
                items={[
                  {
                    label: 'Config',
                    key: '1',
                    children: (
                      <Form layout="vertical">
                        <Form.Item label="Model" name="name">
                          <Select placeholder="Select Model" />
                        </Form.Item>
                        <Form.Item label="Temperature" name="temperature">
                          <Slider defaultValue={30} disabled={false} />
                        </Form.Item>
                        <Form.Item label="Top_p" name="top_p">
                          <Slider defaultValue={30} disabled={false} />
                        </Form.Item>
                        <Form.Item label="Top_k" name="top_k">
                          <Slider defaultValue={30} disabled={false} />
                        </Form.Item>
                        <Form.Item label="Max output tokens" name="Top_k">
                          <Input placeholder="input tokens" />
                        </Form.Item>
                        <Form.Item label="Stop sequences" name="Top_k">
                          <Input placeholder="input sequences" />
                        </Form.Item>
                        <Form.Item label="Model version" name="name">
                          <Select placeholder="Select Model version" />
                        </Form.Item>
                        <Form.Item>
                          <Button type="primary" htmlType="submit">
                            Run
                          </Button>
                        </Form.Item>
                      </Form>
                    ),
                  },
                  {
                    label: 'Tools',
                    key: '2',
                    children: 'Content of Tools',
                  },
                ]}
              />
            </Card>

          </div>
        </div>
      </>
    ) : (
      <>
        not found
      </>
    )
  );
}
