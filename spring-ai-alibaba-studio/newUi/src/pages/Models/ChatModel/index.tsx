import { useEffect, useState } from 'react';
import { request, useRequest } from 'ice';
import { Card, Input, Divider, Tabs, Form, Button, Select, Slider } from 'antd';
import styles from './index.module.css';
import { ChatModel } from "@/types/chat_model";

import chatModelsService from '@/services/chat-models';

export default function ChatModel() {
  const [modelList, setModelList] = useState<ChatModel[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      const data = await chatModelsService.getChatModels();
      setModelList(data);
    };

    fetchData();
  }, []);

  const onChange = (key: string) => {
    console.log(key);
  };

  return (
    <div className={styles.container}>
      <div className={styles.left}>
        {modelList.length > 0 ? (
          modelList.map((model, index) => (
            <>
              <Card title="Model" extra={<a href="#">More</a>} style={{ width: '100%' }}>
                <p>{model.name}</p>

                <Input placeholder="Basic usage" />
              </Card>
            </>
          ))
        ) : (
          <h2>No Models Available</h2>
        )}
      </div>
      <div className="full-height-container">
        <Divider type="vertical" className="full-height-divider" />
      </div>
      <div className={styles.right}>
        {modelList.length > 0 ? (
          <>
            <Card style={{ width: '100%' }}>
              <Tabs
                defaultActiveKey="1"
                onChange={onChange}
                items={[
                  {
                    label: `Config`,
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
                    label: `Tools`,
                    key: '2',
                    children: `Content of Tools`,
                  },
                ]}
              />
            </Card>
          </>
        ) : (
          <h2>No Models Available</h2>
        )}
      </div>
    </div>
  );
}
