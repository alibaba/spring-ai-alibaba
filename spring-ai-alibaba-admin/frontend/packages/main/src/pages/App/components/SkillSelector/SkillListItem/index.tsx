import $i18n from '@/i18n';
import { ISkill } from '@/types/skill';
import { IconFont, renderTooltip } from '@spark-ai/design';
import { Checkbox, Flex, Typography } from 'antd';
import classNames from 'classnames';
import styles from './index.module.less';

interface IProps {
  item: ISkill;
  checked: boolean;
  disabled?: boolean;
  onChange: (checked: boolean) => void;
}

export default function SkillListItem(props: IProps) {
  const { item, checked, disabled, onChange } = props;

  const handleToggle = () => {
    if (disabled && !checked) return;
    onChange(!checked);
  };

  return (
    <div
      className={classNames(styles.wrapper, {
        [styles.active]: checked,
        [styles.disabled]: disabled && !checked,
      })}
      onClick={handleToggle}
    >
      <Flex gap={12} align="flex-start" className={styles.row}>
        <Checkbox
          checked={checked}
          disabled={disabled && !checked}
          onClick={(e) => e.stopPropagation()}
          onChange={(e) => {
            if (disabled && !checked) return;
            onChange(e.target.checked);
          }}
          style={{ marginTop: 10 }}
        />
        <Flex
          align="center"
          justify="center"
          className={styles.logo}
        >
          <IconFont type="spark-paper-line" className={styles.logoIcon} />
        </Flex>
        <div className={styles.content}>
          <Flex justify="space-between" align="center" className={styles.header}>
            <Typography.Text
              className={styles.title}
              ellipsis={{ tooltip: renderTooltip(item.name) }}
            >
              {item.name}
            </Typography.Text>
            {item.source ? (
              <span className={styles.badge}>
                {item.source === 'upload'
                  ? $i18n.get({
                      id: 'main.pages.App.components.SkillSelector.SkillListItem.upload',
                      dm: '上传',
                    })
                  : item.source}
              </span>
            ) : null}
          </Flex>
          <Typography.Text
            className={styles.skillName}
            ellipsis={{ tooltip: renderTooltip(item.skill_name) }}
          >
            {item.skill_name}
          </Typography.Text>
          {item.description ? (
            <Typography.Paragraph
              className={styles.desc}
              style={{ marginBottom: 0 }}
              ellipsis={{
                rows: 1,
                tooltip: renderTooltip(item.description),
              }}
            >
              {item.description}
            </Typography.Paragraph>
          ) : null}
        </div>
      </Flex>
    </div>
  );
}
