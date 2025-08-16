import $i18n from '@/i18n';
import { updateApp } from '@/services/appManage';
import { Form, Input, Modal, message } from '@spark-ai/design';

interface IProps {
  onClose: () => void;
  onOk: () => void;
  name?: string;
  description?: string;
  maxLength?: number;
  app_id: string;
}

export const EditNameModal = (props: IProps) => {
  const [form] = Form.useForm();
  const handleOk = () => {
    form.validateFields().then((values) => {
      updateApp({
        app_id: props.app_id,
        name: values.name,
        description: values.description,
      }).then(() => {
        message.success(
          $i18n.get({
            id: 'main.pages.App.components.EditNameModal.index.updateSuccess',
            dm: '更新成功',
          }),
        );
        props.onOk();
      });
    });
  };
  return (
    <Modal
      open
      onCancel={props.onClose}
      onOk={handleOk}
      title={$i18n.get({
        id: 'main.pages.App.components.EditNameModal.index.editAppName',
        dm: '编辑应用名称',
      })}
    >
      <Form
        initialValues={{
          name: props.name,
          description: props.description,
        }}
        form={form}
        layout="vertical"
      >
        <Form.Item
          required
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.App.components.EditNameModal.index.enterAppName',
                dm: '请输入应用名称',
              }),
            },
          ]}
          label={$i18n.get({
            id: 'main.pages.App.components.EditNameModal.index.appName',
            dm: '应用名称',
          })}
          name="name"
        >
          <Input
            style={{ height: 36, marginBottom: 12 }}
            maxLength={props?.maxLength || 50}
            showCount={{
              formatter: ({ count, maxLength }) => (
                <>
                  {count}/{maxLength}
                </>
              ),
            }}
            placeholder={$i18n.get({
              id: 'main.pages.App.components.EditNameModal.index.enterAppName',
              dm: '请输入应用名称',
            })}
          />
        </Form.Item>
        <Form.Item
          label={$i18n.get({
            id: 'main.pages.App.components.EditNameModal.index.appDescription',
            dm: '应用描述',
          })}
          name="description"
        >
          <Input.TextArea
            style={{ height: 150 }}
            maxLength={200}
            placeholder={$i18n.get({
              id: 'main.pages.App.components.EditNameModal.index.enterAppDescription',
              dm: '请输入应用描述',
            })}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};
