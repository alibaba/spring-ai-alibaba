import $i18n from '@/i18n';
import { SkillSelectDrawer } from '@/pages/App/components/SkillSelector';
import { ISkill, SKILL_MAX_LIMIT } from '@/types/skill';
import { Button, HelpIcon, IconFont } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Divider, Flex } from 'antd';
import cls from 'classnames';
import { useContext, useEffect } from 'react';
import { AssistantAppContext } from '../../AssistantAppContext';
import SelectedConfigItem from '../SelectedConfigItem';
import styles from '../MCPSelectorComp/index.module.less';

export function SelectedSkillItem({
  item,
  handleRemove,
}: {
  item: ISkill;
  handleRemove: (item: ISkill) => void;
}) {
  return (
    <SelectedConfigItem
      iconType="spark-paper-line"
      name={item.name}
      description={item.description || item.skill_name}
      rightArea={
        <IconFont
          type="spark-delete-line"
          isCursorPointer
          onClick={() => handleRemove(item)}
        />
      }
    />
  );
}

export default function SkillSelectorComp() {
  const { appState, onAppConfigChange } = useContext(AssistantAppContext);
  const { skills = [] as ISkill[] } = appState.appBasicConfig?.config || {};
  const [state, setState] = useSetState({
    expand: false,
    selectVisible: false,
  });

  const onSelectSkills = (val: ISkill[]) => {
    onAppConfigChange({ skills: val });
  };

  useEffect(() => {
    if (skills.length) {
      setState({ expand: true });
    }
  }, [skills]);

  return (
    <Flex vertical gap={6} className="mb-[20px]">
      <Flex justify="space-between" align="center">
        <Flex
          gap={8}
          className="text-[13px] font-medium leading-[20px]"
          style={{ color: 'var(--ag-ant-color-text)' }}
          align="center"
        >
          <Flex align="center">
            <span>
              {$i18n.get({
                id: 'main.components.SkillSelectorComp.index.skill',
                dm: 'Skills',
              })}
            </span>
            <HelpIcon
              content={$i18n.get({
                id: 'main.components.SkillSelectorComp.index.help',
                dm: '智能体可按需加载 SKILL.md 指令包（渐进式披露），通过 read_skill 读取完整内容。',
              })}
            />
          </Flex>
          <span
            className="text-[12px] leading-[20px]"
            style={{ color: 'var(--ag-ant-color-text-tertiary)' }}
          >
            {skills.length}/{SKILL_MAX_LIMIT}
          </span>
        </Flex>
        <span>
          <Button
            style={{ padding: 0 }}
            onClick={() => setState({ selectVisible: true })}
            iconType="spark-plus-line"
            type="text"
            size="small"
          >
            Skill
          </Button>
          <Divider type="vertical" className="ml-[16px] mr-[16px]" />
          <IconFont
            onClick={() => setState({ expand: !state.expand })}
            className={cls(styles['expand-btn'], !state.expand && styles.hidden)}
            type="spark-up-line"
            isCursorPointer
          />
        </span>
      </Flex>
      {state.expand && (
        <Flex vertical gap={8}>
          {skills.map(
            (item) =>
              item && (
                <SelectedSkillItem
                  key={item.skill_id}
                  item={item}
                  handleRemove={() =>
                    onSelectSkills(
                      skills.filter((s) => s.skill_id !== item.skill_id),
                    )
                  }
                />
              ),
          )}
        </Flex>
      )}
      {state.selectVisible && (
        <SkillSelectDrawer
          selectedSkills={skills}
          onOk={(val) => {
            onSelectSkills(val);
            setState({ selectVisible: false, expand: true });
          }}
          onClose={() => setState({ selectVisible: false })}
        />
      )}
    </Flex>
  );
}
