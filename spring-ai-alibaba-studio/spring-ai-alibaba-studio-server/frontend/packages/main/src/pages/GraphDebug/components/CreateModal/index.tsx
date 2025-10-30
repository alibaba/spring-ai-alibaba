import { Form, Input, Modal, Select, Tag, message } from 'antd';
import $i18n from '@/i18n';
import { useState } from 'react';

interface IGraphCard {
  id: string;
  name: string;
  description?: string;
  tags?: string[];
}

interface CreateModalProps {
  onCancel: () => void;
  onOk: (graph: IGraphCard) => void;
}

const { TextArea } = Input;
const { Option } = Select;

const CreateModal: React.FC<CreateModalProps> = ({ onCancel, onOk }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [tags, setTags] = useState<string[]>([]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      
      // TODO: 调用真实的创建工作流API
      // await graphDebugService.createGraph(values);
      
      const graph: IGraphCard = {
        id: `graph_${Date.now()}`, // 临时ID，实际应从API返回
        name: values.name,
        description: values.description,
        tags,
      };
      
      message.success(
        $i18n.get({
          id: 'main.pages.GraphDebug.components.CreateModal.createSuccess',
          dm: '创建成功',
        })
      );
      
      onOk(graph);
    } catch (error) {
      // Handle form validation errors
    } finally {
      setLoading(false);
    }
  };

  const handleTagsChange = (newTags: string[]) => {
    setTags(newTags);
  };

  return (
    <Modal
      title={$i18n.get({
        id: 'main.pages.GraphDebug.components.CreateModal.title',
        dm: '创建图形',
      })}
      open={true}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      width={600}
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          type: 'workflow',
        }}
      >
        <Form.Item
          name="name"
          label={$i18n.get({
            id: 'main.pages.GraphDebug.components.CreateModal.name',
            dm: '图形名称',
          })}
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.GraphDebug.components.CreateModal.nameRequired',
                dm: '请输入图形名称',
              }),
            },
          ]}
        >
          <Input
            placeholder={$i18n.get({
              id: 'main.pages.GraphDebug.components.CreateModal.namePlaceholder',
              dm: '请输入图形名称',
            })}
          />
        </Form.Item>

        <Form.Item
          name="description"
          label={$i18n.get({
            id: 'main.pages.GraphDebug.components.CreateModal.description',
            dm: '描述',
          })}
        >
          <TextArea
            rows={4}
            placeholder={$i18n.get({
              id: 'main.pages.GraphDebug.components.CreateModal.descriptionPlaceholder',
              dm: '请输入图形描述',
            })}
          />
        </Form.Item>

        <Form.Item
          name="template"
          label={$i18n.get({
            id: 'main.pages.GraphDebug.components.CreateModal.template',
            dm: '选择模板',
          })}
        >
          <Select
            placeholder={$i18n.get({
              id: 'main.pages.GraphDebug.components.CreateModal.templatePlaceholder',
              dm: '选择工作流模板',
            })}
            allowClear
          >
            <Option value="demo-graph-1">单Agent模板</Option>
            <Option value="demo-graph-2">多Agent模板</Option>
          </Select>
        </Form.Item>

        <Form.Item
          label={$i18n.get({
            id: 'main.pages.GraphDebug.components.CreateModal.tags',
            dm: '标签',
          })}
        >
          <Select
            mode="tags"
            style={{ width: '100%' }}
            placeholder={$i18n.get({
              id: 'main.pages.GraphDebug.components.CreateModal.tagsPlaceholder',
              dm: '添加标签',
            })}
            value={tags}
            onChange={handleTagsChange}
          >
            <Option value="ai">AI</Option>
            <Option value="workflow">工作流</Option>
            <Option value="automation">自动化</Option>
            <Option value="analysis">分析</Option>
            <Option value="document">文档</Option>
            <Option value="feedback">反馈</Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CreateModal;
