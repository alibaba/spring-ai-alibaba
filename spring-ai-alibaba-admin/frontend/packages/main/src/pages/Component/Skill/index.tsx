import CardList from '@/components/Card/List';
import { useInnerLayout } from '@/components/InnerLayout/utils';
import $i18n from '@/i18n';
import { createSkill, listSkills } from '@/services/skill';
import { IPagingList, ISkill } from '@/types/skill';
import {
  Button,
  IconFont,
  Input,
  message,
  Modal,
} from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { Flex, Form, Input as AntdInput, Upload } from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import { memo, useState } from 'react';
import SkillCard from './Card';
import styles from './index.module.less';

export const CreateSkillBtn = memo((props: { onSuccess?: () => void }) => {
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [loading, setLoading] = useState(false);

  const handleOk = async () => {
    const values = await form.validateFields();
    const file = fileList[0]?.originFileObj as File | undefined;
    if (!file) {
      message.warning(
        $i18n.get({
          id: 'main.pages.Component.Skill.uploadZipRequired',
          dm: '请上传包含 SKILL.md 的 zip 包',
        }),
      );
      return;
    }
    setLoading(true);
    try {
      await createSkill({
        file,
        name: values.name,
        description: values.description,
      });
      message.success(
        $i18n.get({
          id: 'main.pages.Component.Skill.createSuccess',
          dm: '技能创建成功',
        }),
      );
      setOpen(false);
      form.resetFields();
      setFileList([]);
      props.onSuccess?.();
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Button
        type="primary"
        icon={<IconFont type="spark-plus-line" />}
        onClick={() => setOpen(true)}
      >
        {$i18n.get({
          id: 'main.pages.Component.Skill.uploadSkill',
          dm: '上传技能',
        })}
      </Button>
      <Modal
        title={$i18n.get({
          id: 'main.pages.Component.Skill.uploadSkill',
          dm: '上传技能',
        })}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={handleOk}
        confirmLoading={loading}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label={$i18n.get({
              id: 'main.pages.Component.Skill.zipPackage',
              dm: '技能包 (zip)',
            })}
            required
          >
            <Upload
              accept=".zip"
              maxCount={1}
              fileList={fileList}
              beforeUpload={() => false}
              onChange={({ fileList: next }) => setFileList(next)}
            >
              <Button>
                {$i18n.get({
                  id: 'main.pages.Component.Skill.selectZip',
                  dm: '选择 zip 文件',
                })}
              </Button>
            </Upload>
          </Form.Item>
          <Form.Item
            name="name"
            label={$i18n.get({
              id: 'main.pages.Component.Skill.displayName',
              dm: '展示名称（可选）',
            })}
          >
            <Input
              placeholder={$i18n.get({
                id: 'main.pages.Component.Skill.displayNamePlaceholder',
                dm: '默认使用 SKILL.md 中的 name',
              })}
            />
          </Form.Item>
          <Form.Item
            name="description"
            label={$i18n.get({
              id: 'main.pages.Component.Skill.displayDesc',
              dm: '描述（可选）',
            })}
          >
            <AntdInput.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
});

export default function SkillList() {
  const { rightPortal } = useInnerLayout();
  const [state, setState] = useSetState<{
    list: ISkill[];
    pageNo: number;
    pageSize: number;
    total: number;
    loading: boolean;
    name: string;
  }>({
    list: [],
    pageNo: 1,
    pageSize: 50,
    total: 0,
    loading: false,
    name: '',
  });

  const fetchList = async (
    extraParams: Partial<{ pageNo: number; pageSize: number; name: string }> = {},
  ) => {
    setState({ loading: true });
    try {
      const queryParams = {
        current: extraParams.pageNo ?? state.pageNo,
        size: extraParams.pageSize ?? state.pageSize,
        name: extraParams.name ?? state.name,
      };
      const response = await listSkills(queryParams);
      const pagingData = response.data as IPagingList<ISkill>;
      setState({
        list: pagingData.records || [],
        total: pagingData.total || 0,
        pageNo: queryParams.current,
        pageSize: queryParams.size,
      });
    } finally {
      setState({ loading: false });
    }
  };

  useMount(() => {
    fetchList();
  });

  return (
    <>
      {rightPortal(<CreateSkillBtn onSuccess={() => fetchList()} />)}
      <Flex justify="space-between" align="center" className="mb-[24px]">
        <Input
          style={{ width: 240 }}
          allowClear
          prefix={<IconFont type="spark-search-line" />}
          placeholder={$i18n.get({
            id: 'main.pages.Component.Skill.searchPlaceholder',
            dm: '搜索技能名称',
          })}
          onChange={(e) => {
            const name = e.target.value;
            setState({ name, pageNo: 1 });
            fetchList({ name, pageNo: 1 });
          }}
        />
      </Flex>
      <CardList
        className={styles.grid}
        loading={state.loading}
        pagination={{
          current: state.pageNo,
          total: state.total,
          pageSize: state.pageSize,
          onChange: (page, pageSize) =>
            fetchList({ pageNo: page, pageSize }),
        }}
        emptyAction={<CreateSkillBtn onSuccess={() => fetchList()} />}
        emptyProps={{
          title: $i18n.get({
            id: 'main.pages.Component.Skill.empty',
            dm: '暂无技能，请上传包含 SKILL.md 的 zip 包',
          }),
        }}
      >
        {state.list.map((item) => (
          <SkillCard key={item.skill_id} {...item} reload={() => fetchList()} />
        ))}
      </CardList>
    </>
  );
}
