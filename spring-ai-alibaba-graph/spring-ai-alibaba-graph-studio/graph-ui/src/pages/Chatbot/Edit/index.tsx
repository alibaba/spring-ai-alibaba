/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Bubble, Sender } from '@ant-design/x';
import { Icon } from '@iconify/react/dist/iconify.js';
import {
  Button,
  Card,
  Checkbox,
  Empty,
  Flex,
  Form,
  Input,
  Modal,
  Popover,
  Radio,
  Select,
  Space,
  Switch,
  Table,
  TableProps,
  Tag,
  Typography,
} from 'antd';
import { useEffect, useState } from 'react';
import styles from './index.less';

const { TextArea } = Input;
const { Text } = Typography;
interface VariableDataType {
  id: number;
  key: string;
  name: string;
  optional: boolean;
  type: string;
  value: string;
  options?: string[];
  maxLength?: number;
}

export default function () {
  // 提示词
  const [prompt, setPrompt] = useState('');

  // 添加变量可选内容
  const variableContent = (
    <Space direction="vertical" style={{ width: '100px' }}>
      <Button
        onClick={() => addVariable('text')}
        type="text"
        style={{ width: '100%' }}
      >
        文本
      </Button>
      <Button
        onClick={() => addVariable('paragraph')}
        type="text"
        style={{ width: '100%' }}
      >
        段落
      </Button>
      <Button
        onClick={() => addVariable('select')}
        type="text"
        style={{ width: '100%' }}
      >
        下拉选项
      </Button>
      <Button
        onClick={() => addVariable('number')}
        type="text"
        style={{ width: '100%' }}
      >
        数字
      </Button>
    </Space>
  );

  // 变量数据
  const [variableData, setVariableData] = useState<VariableDataType[]>([
    {
      id: 1,
      key: 'input',
      name: '变量1',
      optional: true,
      type: 'text',
      value: '变量1',
      maxLength: 48,
    },
    {
      id: 2,
      key: 'paragraph',
      name: '变量2',
      optional: true,
      type: 'paragraph',
      value: '变量2',
      maxLength: 148,
    },
    {
      id: 3,
      key: 'select',
      name: '变量3',
      optional: true,
      type: 'select',
      value: '变量3',
      options: ['选项1', '选项2'],
    },
    {
      id: 4,
      key: 'number',
      name: '变量4',
      optional: true,
      type: 'number',
      value: '变量4',
    },
  ]);

  // 改变变量key
  const changeVariableKey = (value: string, record: VariableDataType) => {
    console.log(value, record);
    // 改变变量key
    setVariableData(
      variableData.map((item) =>
        item.id === record.id ? { ...item, key: value } : item,
      ),
    );
  };

  // 改变变量名称
  const changeVariableName = (value: string, record: VariableDataType) => {
    // 改变所有相同key的变量名称
    setVariableData(
      variableData.map((item) =>
        item.key === record.key ? { ...item, name: value } : item,
      ),
    );
  };

  // 改变量可选
  const changeVariableOptional = (value: boolean, record: VariableDataType) => {
    // 改变所有相同key的变量可选状态
    setVariableData(
      variableData.map((item) =>
        item.key === record.key ? { ...item, optional: value } : item,
      ),
    );
  };

  // 删除变量
  const deleteVariable = (record: VariableDataType) => {
    console.log(record);
    // 删除变量
    setVariableData(variableData.filter((item) => item.key !== record.key));
  };

  // 根据变量类型添加变量
  const addVariable = (type: string) => {
    console.log('添加变量');
    // 添加变量
    setVariableData([
      ...variableData,
      {
        id: variableData.length + 1,
        key: type,
        name: '变量' + (variableData.length + 1),
        optional: true,
        type: type,
        value: '变量' + (variableData.length + 1),
        options: type === 'select' ? [] : undefined,
      },
    ]);
  };

  useEffect(() => {
    console.log(variableData);
  }, [variableData]);

  const variableDataColumns: TableProps<VariableDataType>['columns'] = [
    {
      title: '变量 KEY',
      dataIndex: 'key',
      key: 'key',
      render: (text: string, record: VariableDataType) => (
        <Input
          value={text}
          onChange={(e) => changeVariableKey(e.target.value, record)}
          placeholder="Filled"
          variant="filled"
        />
      ),
    },
    {
      title: '字段名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: VariableDataType) => (
        <Input
          value={text}
          onChange={(e) => changeVariableName(e.target.value, record)}
          placeholder="Filled"
          variant="filled"
        />
      ),
    },
    {
      title: '可选',
      dataIndex: 'optional',
      key: 'optional',
      render: (text: boolean, record: VariableDataType) => (
        <Switch
          checked={text}
          onChange={(value) => changeVariableOptional(value, record)}
        />
      ),
    },
    {
      title: '操作',
      dataIndex: 'operation',
      key: 'operation',
      render: (text: string, record: VariableDataType) => (
        <Space>
          <Button
            onClick={() => handleEditClick(record)}
            type="text"
            icon={
              <Icon
                style={{ fontSize: '16px' }}
                icon="material-symbols:settings-outline-rounded"
              />
            }
          />
          <Button
            type="text"
            onClick={() => deleteVariable(record)}
            icon={
              <Icon
                style={{ fontSize: '16px' }}
                icon="material-symbols:delete-outline-rounded"
              />
            }
          />
        </Space>
      ),
    },
  ];

  // 编辑变量Modal
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [currentVariable, setCurrentVariable] =
    useState<VariableDataType | null>(null);

  // 编辑form
  const [editForm] = Form.useForm();
  // 点击编辑图标时，设置当前编辑的变量
  const handleEditClick = (record: VariableDataType) => {
    setCurrentVariable(record);
    editForm.setFieldsValue({ ...record });
    console.log('curent variable', record);
    setIsEditModalOpen(true);
  };

  // 在 Modal 中绑定表单字段到状态
  const handleFieldChange = (field: string, value: any) => {
    console.log(value);
    if (currentVariable) {
      switch (field) {
        case 'type':
          if (value === 'select') {
            setCurrentVariable({
              ...currentVariable,
              maxLength: 0,
              value: '',
              options: [],
              type: value,
            });
          } else if (value === 'paragraph') {
            setCurrentVariable({
              ...currentVariable,
              maxLength: 48,
              options: [],
              type: value,
            });
          } else if (value === 'text') {
            setCurrentVariable({
              ...currentVariable,
              maxLength: 48,
              options: [],
              type: value,
            });
          } else if (value === 'number') {
            setCurrentVariable({
              ...currentVariable,
              maxLength: 0,
              options: [],
              type: value,
            });
          } else {
            setCurrentVariable({
              ...currentVariable,
              maxLength: undefined,
              options: undefined,
              type: value,
            });
          }
          break;
        case 'maxLength':
          setCurrentVariable({ ...currentVariable, maxLength: value });
          break;
        case 'options':
          setCurrentVariable({ ...currentVariable, options: value });
          break;
        default:
          setCurrentVariable({ ...currentVariable });
          break;
      }
    }
  };

  // 在保存时更新 variableData
  const handleEditOk = () => {
    if (currentVariable) {
      setVariableData(
        variableData.map((item) =>
          item.id === currentVariable.id ? currentVariable : item,
        ),
      );
    }
    setIsEditModalOpen(false);
  };

  const handleEditCancel = () => {
    setIsEditModalOpen(false);
  };

  // 字段类型
  const fieldTypeOptions = [
    { label: '文本', value: 'text' },
    { label: '段落', value: 'paragraph' },
    { label: '下拉选项', value: 'select' },
    { label: '数字', value: 'number' },
  ];

  // 删除选项
  const deleteOption = (index: number) => {
    console.log(index);
    // 删除选项
    setCurrentVariable({
      ...currentVariable!,
      options: currentVariable!.options?.filter((_, i) => i !== index),
    });
  };

  // 添加选项
  const addOption = () => {
    console.log('添加选项');
    // 添加选项
    setCurrentVariable({
      ...currentVariable!,
      options: [...(currentVariable!.options || []), ''],
    });
  };

  // 调试和预览显示
  const [isDebugAndPreviewOpen, setIsDebugAndPreviewOpen] = useState(false);

  // 是否开启视觉
  const [isVisualOpen, setIsVisualOpen] = useState(false);

  // 格式化变量数据
  const formatVariableData = () => {
    return variableData.map((item) => {
      const baseConfig = {
        default: item.value || '',
        label: item.name,
        required: item.optional || false,
        variable: item.key,
      };

      const config: any = {
        text: {
          ...baseConfig,
          max_length: item.maxLength || 48,
        },
        paragraph: baseConfig,
        select: {
          ...baseConfig,
          options: item.options || [],
        },
        number: baseConfig,
      };

      return {
        [item.type === 'text' ? 'text-input' : item.type]:
          config[item.type as keyof typeof config],
      };
    });
  };

  // 导出
  const exportData = () => {
    let exportData = {
      app: {
        description: '',
        icon: '🤖',
        icon_background: '#FFEAD5',
        mode: 'chat',
        name: 'f',
        use_icon_as_answer_icon: false,
      },
      kind: 'app',
      model_config: {
        agent_mode: {
          enabled: false,
          max_iteration: 5,
          strategy: 'function_call',
          tools: [],
        },
        annotation_reply: {
          enabled: false,
        },
        chat_prompt_config: {},
        completion_prompt_config: {},
        dataset_configs: {
          datasets: {
            datasets: [],
          },
          reranking_enable: true,
          retrieval_model: 'multiple',
          top_k: 4,
        },
        dataset_query_variable: '',
        external_data_tools: [],
        file_upload: {
          allowed_file_extensions: [
            '.JPG',
            '.JPEG',
            '.PNG',
            '.GIF',
            '.WEBP',
            '.SVG',
            '.MP4',
            '.MOV',
            '.MPEG',
            '.MPGA',
          ],
          allowed_file_types: [],
          allowed_file_upload_methods: ['remote_url', 'local_file'],
          enabled: false,
          image: {
            detail: 'high',
            enabled: false,
            number_limits: 3,
            transfer_methods: ['remote_url', 'local_file'],
          },
          number_limits: 3,
        },
        model: {
          completion_params: {
            stop: [],
          },
          mode: 'chat',
          name: 'gpt-4o-mini',
          provider: 'openai',
        },
        more_like_this: {
          enabled: false,
        },
        opening_statement: '',
        pre_prompt: prompt,
        prompt_type: 'simple',
        retriever_resource: {
          enabled: true,
        },
        sensitive_word_avoidance: {
          configs: [],
          enabled: false,
          type: '',
        },
        speech_to_text: {
          enabled: false,
        },
        suggested_questions: [],
        suggested_questions_after_answer: {
          enabled: false,
        },
        text_to_speech: {
          enabled: false,
          language: '',
          voice: '',
        },
        user_input_form: formatVariableData(),
      },
      version: '0.1.4',
    };
  };

  // 添加上下文modal
  const [isAddContextModalOpen, setAddContextModalOpen] = useState(false);
  // 可用的资料库
  type AvailableDataset = {
    id: string;
    name: string;
    tag: string;
  };
  const [availableDatasets, setAvailableDatasets] = useState<
    AvailableDataset[]
  >([
    {
      id: '1',
      name: 'index.htmldadadadadasdada',
      tag: '高质量·向量检索',
    },
    {
      id: '2',
      name: 'index.html',
      tag: '高质量·向量检索',
    },
  ]);

  // 选择的资料库
  const [selectedDataset, setSelectedDataset] = useState<AvailableDataset[]>(
    [],
  );

  // 使用的资料库
  const [usedDatasets, setUsedDatasets] = useState<AvailableDataset[]>([]);
  // 点击资料库添加选择
  const handleDatasetSelect = (dataset: AvailableDataset) => {
    const isDatasetSelected = selectedDataset.some(
      (item) => item.id === dataset.id,
    );
    if (isDatasetSelected) {
      setSelectedDataset(
        selectedDataset.filter((item) => item.id !== dataset.id),
      );
    } else {
      setSelectedDataset([...selectedDataset, dataset]);
    }
  };

  // 添加上下文
  const handleAddContextOk = () => {
    setAddContextModalOpen(false);
    setUsedDatasets(usedDatasets.concat(selectedDataset));
  };

  return (
    <>
      {/* 编辑变量Modal */}
      <Modal
        centered
        title="编辑变量"
        open={isEditModalOpen}
        okText="保存"
        onOk={handleEditOk}
        onCancel={handleEditCancel}
      >
        <Form
          form={editForm}
          layout="vertical"
          initialValues={currentVariable || undefined}
        >
          <Form.Item label="字段类型" name="type">
            <Radio.Group
              block
              options={fieldTypeOptions}
              value={currentVariable?.type}
              onChange={(e) => handleFieldChange('type', e.target.value)}
              optionType="button"
              buttonStyle="solid"
            />
          </Form.Item>
          <Form.Item label="变量名称" name="name">
            <Input
              value={currentVariable?.key}
              onChange={(e) => handleFieldChange('key', e.target.value)}
              placeholder="请输入"
              variant="filled"
            />
          </Form.Item>
          <Form.Item label="显示名称" name="optional">
            <Input
              value={currentVariable?.name}
              onChange={(e) => handleFieldChange('name', e.target.value)}
              placeholder="请输入"
              variant="filled"
            />
          </Form.Item>
          {currentVariable?.maxLength && (
            <Form.Item label="最大长度" name="maxLength">
              <Input
                min={1}
                type="number"
                value={currentVariable?.maxLength}
                onChange={(e) =>
                  handleFieldChange('maxLength', parseInt(e.target.value))
                }
                placeholder="请输入"
                variant="filled"
              />
            </Form.Item>
          )}
          {currentVariable?.options && (
            <Form.Item label="选项" name="options">
              <Space direction="vertical" style={{ width: '100%' }}>
                {currentVariable.options.map((option, index) => (
                  <Flex key={index} gap={10} style={{ width: '100%' }}>
                    <Input
                      style={{ width: '100%' }}
                      value={option}
                      onChange={(e) =>
                        handleFieldChange('options', e.target.value)
                      }
                      placeholder="请输入"
                      variant="filled"
                    />
                    <Button
                      type="text"
                      icon={
                        <Icon
                          style={{ fontSize: '16px' }}
                          icon="material-symbols:delete-outline-rounded"
                        />
                      }
                      onClick={() => deleteOption(index)}
                    />
                  </Flex>
                ))}
                <Button
                  style={{ width: '100%' }}
                  type="text"
                  icon={
                    <Icon
                      style={{ fontSize: '16px' }}
                      icon="material-symbols:add"
                    />
                  }
                  onClick={addOption}
                >
                  添加选项
                </Button>
              </Space>
            </Form.Item>
          )}
          <Form.Item label="" name="required">
            <Checkbox
              checked={!currentVariable?.optional}
              onChange={(e) => handleFieldChange('optional', !e.target.checked)}
            >
              必填
            </Checkbox>
          </Form.Item>
        </Form>
      </Modal>
      {/* 添加上下文modal */}
      <Modal
        onCancel={() => setAddContextModalOpen(false)}
        centered
        title="选择引用知识库"
        open={isAddContextModalOpen}
        okText="添加"
        onOk={handleAddContextOk}
      >
        {availableDatasets.length > 0 ? (
          availableDatasets.map((dataset, index) => (
            <div
              onClick={() => handleDatasetSelect(dataset)}
              key={index}
              className={`${styles['dataset-item']} ${
                selectedDataset.some((item) => item.id === dataset.id)
                  ? styles['dataset-item-selected']
                  : ''
              }`}
            >
              <div className={styles['dataset-item-name']}>{dataset.name}</div>
              <Tag>{dataset.tag}</Tag>
            </div>
          ))
        ) : (
          <Empty
            imageStyle={{ display: 'none' }}
            description={
              <Typography.Text>
                未找到知识库 <a href="#API">去创建</a>
              </Typography.Text>
            }
          />
        )}
      </Modal>
      <Card
        title="编辑"
        extra={
          <Flex justify="end">
            <Button type="link" onClick={exportData}>
              导出
            </Button>
            <Button type="primary">发布</Button>
          </Flex>
        }
        style={{ width: '100%' }}
      >
        <Flex justify="space-between" gap={10} style={{ width: '100%' }}>
          {/* 左侧 */}
          <Space
            direction="vertical"
            style={{
              width: '50%',
              maxHeight: 'calc(100vh - 170px)',
              overflow: 'auto',
            }}
          >
            {/* 提示词 */}
            <Card
              size="small"
              title="提示词"
              extra={
                <Button size="small" type="primary">
                  生成
                </Button>
              }
            >
              <TextArea
                onChange={(e) => setPrompt(e.target.value)}
                value={prompt}
                rows={10}
                placeholder="在这里写你的提示词，输入'插入变量'插入变量、输入'插入提示内容块'插入提示内容块"
              />
            </Card>

            {/* 变量 */}
            <Card
              size="small"
              title="变量"
              extra={
                <Popover
                  placement="bottomRight"
                  trigger="click"
                  content={variableContent}
                >
                  <Button size="small" type="primary">
                    添加
                  </Button>
                </Popover>
              }
              style={{ width: '100%' }}
            >
              {variableData.length > 0 ? (
                <Table<VariableDataType>
                  columns={variableDataColumns}
                  dataSource={variableData}
                  pagination={false}
                  rowKey="id"
                />
              ) : (
                <Text
                  disabled
                >{`变量能使用户输入表单引入提示词或开场白，你可以试试在提示词中输入 {{ input }}`}</Text>
              )}
            </Card>
            {/* 上下文 */}
            <Card
              size="small"
              title="上下文"
              extra={
                <Space>
                  <Button
                    size="small"
                    type="text"
                    icon={
                      <Icon
                        style={{ fontSize: '16px' }}
                        icon="material-symbols:settings-outline-rounded"
                      />
                    }
                  >
                    召回设置
                  </Button>
                  <Button
                    size="small"
                    type="primary"
                    onClick={() => setAddContextModalOpen(true)}
                  >
                    添加
                  </Button>
                </Space>
              }
            >
              {usedDatasets.length > 0 ? (
                usedDatasets.map((dataset, index) => (
                  <div
                    onClick={() => handleDatasetSelect(dataset)}
                    key={index}
                    className={`${styles.dataset} ${
                      selectedDataset.some((item) => item.id === dataset.id)
                        ? styles.datasetSelected
                        : ''
                    }`}
                  >
                    <div className={styles.datasetName}>{dataset.name}</div>
                    <Tag>{dataset.tag}</Tag>
                  </div>
                ))
              ) : (
                <span>您可以导入知识库作为上下文</span>
              )}
            </Card>
            {/* 视觉 */}
            <Card size="small">
              <Flex justify="space-between" gap={10} style={{ width: '100%' }}>
                <Space>视觉</Space>
                <Space>
                  <Button
                    type="text"
                    icon={
                      <Icon
                        style={{ fontSize: '16px' }}
                        icon="material-symbols:settings-outline-rounded"
                      />
                    }
                  >
                    设置
                  </Button>
                  <Switch
                    checked={isVisualOpen}
                    onChange={(value) => setIsVisualOpen(value)}
                  />
                </Space>
              </Flex>
            </Card>
          </Space>
          {/* 右侧 */}
          <Card
            size="small"
            title="调试和预览"
            style={{ width: '50%', height: 'calc(100vh - 170px)' }}
            extra={
              <Space>
                <Button
                  onClick={() =>
                    setIsDebugAndPreviewOpen(!isDebugAndPreviewOpen)
                  }
                  type="text"
                  icon={
                    <Icon
                      style={{ fontSize: '16px' }}
                      icon="tdesign:adjustment"
                    />
                  }
                />
              </Space>
            }
          >
            <Space
              direction="vertical"
              style={{ width: '100%', height: '100%' }}
            >
              {/* 用户输入字段调试与预览 */}
              {isDebugAndPreviewOpen && (
                <Card>
                  <Form layout="vertical" initialValues={{}}>
                    {variableData.map((item) => {
                      switch (item.type) {
                        case 'text':
                          return (
                            <Form.Item
                              label={
                                item.name + (item?.optional ? '（选填）' : '')
                              }
                              name={item.key}
                            >
                              <Input
                                maxLength={item.maxLength}
                                placeholder="请输入"
                                variant="filled"
                              />
                            </Form.Item>
                          );
                        case 'paragraph':
                          return (
                            <Form.Item
                              label={
                                item.name + (item?.optional ? '（选填）' : '')
                              }
                              name={item.key}
                            >
                              <TextArea
                                maxLength={item.maxLength}
                                placeholder="请输入"
                                variant="filled"
                              />
                            </Form.Item>
                          );
                        case 'select':
                          return (
                            <Form.Item
                              label={
                                item.name + (item?.optional ? '（选填）' : '')
                              }
                              name={item.key}
                            >
                              <Select
                                variant="filled"
                                placeholder="请选择"
                                value={item.value}
                                onChange={(value) => {
                                  setVariableData(
                                    variableData.map((variable) => {
                                      if (variable.key === item.key) {
                                        return {
                                          ...variable,
                                          value: value,
                                        };
                                      }
                                      return variable;
                                    }),
                                  );
                                }}
                                options={item.options?.map((option) => ({
                                  label: option,
                                  value: option,
                                }))}
                              />
                            </Form.Item>
                          );
                        case 'number':
                          return (
                            <Form.Item
                              label={
                                item.name + (item?.optional ? '（选填）' : '')
                              }
                              name={item.key}
                            >
                              <Input
                                min={1}
                                type="number"
                                placeholder="请输入"
                                variant="filled"
                              />
                            </Form.Item>
                          );
                      }
                    })}
                  </Form>
                </Card>
              )}
              {/* 气泡 */}
              <Flex gap="middle" vertical>
                <Bubble
                  placement="start"
                  content="What a beautiful day!"
                  avatar={{}}
                />

                <Bubble placement="end" content="Thank you!" avatar={{}} />
              </Flex>
            </Space>
            <Sender
              style={{
                width: '90%',
                position: 'absolute',
                left: '50%',
                bottom: 20,
                transform: 'translateX(-50%)',
              }}
              value="Force as loading"
            />
          </Card>
        </Flex>
      </Card>
    </>
  );
}
