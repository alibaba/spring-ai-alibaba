import $i18n from '@/i18n';
import { deleteSkill } from '@/services/skill';
import { ISkill } from '@/types/skill';
import { AlertDialog, IconButton, IconFont, message } from '@spark-ai/design';
import { Button, Dropdown, Typography } from 'antd';
import dayjs from 'dayjs';
import { history } from 'umi';
import styles from './index.module.less';

export default function SkillCard(props: ISkill & { reload: () => void }) {
  const goDetail = (e?: React.MouseEvent) => {
    e?.stopPropagation();
    history.push(`/component/skill/${props.skill_id}`);
  };

  const handleDelete = () => {
    AlertDialog.warning({
      title: $i18n.get({
        id: 'main.pages.Component.Skill.confirmDelete',
        dm: '确认删除此技能吗',
      }),
      children: $i18n.get({
        id: 'main.pages.Component.Skill.deleteTip',
        dm: '删除后不可恢复，已挂载该技能的智能体可能失效，请谨慎操作',
      }),
      danger: true,
      onOk: async () => {
        await deleteSkill(props.skill_id);
        message.success(
          $i18n.get({
            id: 'main.pages.Component.Skill.deleteSuccess',
            dm: '删除成功',
          }),
        );
        props.reload();
      },
    });
  };

  return (
    <div className={styles.skillCard} onClick={() => goDetail()}>
      <div className={styles.cardMain}>
        <div className={styles.cardHeader}>
          <div className={styles.logo}>
            <IconFont type="spark-paper-line" />
          </div>
          <div className={styles.headerText}>
            <Typography.Text className={styles.title} ellipsis={{ tooltip: true }}>
              {props.name}
            </Typography.Text>
            <Typography.Text className={styles.skillName} ellipsis={{ tooltip: true }}>
              {props.skill_name}
            </Typography.Text>
          </div>
        </div>

        <Typography.Paragraph
          className={styles.desc}
          ellipsis={{ rows: 2, tooltip: true }}
        >
          {props.description || '-'}
        </Typography.Paragraph>

        <div className={styles.meta}>
          {$i18n.get({
            id: 'main.pages.Component.Skill.updatedAt',
            dm: '更新于',
          })}
          {dayjs(props.gmt_modified).format('YYYY-MM-DD HH:mm')}
        </div>
      </div>

      <div className={styles.cardActions} onClick={(e) => e.stopPropagation()}>
        <Button type="primary" block onClick={(e) => goDetail(e)}>
          {$i18n.get({
            id: 'main.pages.Component.Skill.viewContent',
            dm: '查看内容',
          })}
        </Button>
        <Dropdown
          trigger={['click']}
          menu={{
            items: [
              {
                key: 'delete',
                danger: true,
                label: $i18n.get({
                  id: 'main.pages.Component.Skill.delete',
                  dm: '删除',
                }),
              },
            ],
            onClick: ({ key }) => {
              if (key === 'delete') handleDelete();
            },
          }}
        >
          <IconButton shape="default" icon="spark-more-line" />
        </Dropdown>
      </div>
    </div>
  );
}
