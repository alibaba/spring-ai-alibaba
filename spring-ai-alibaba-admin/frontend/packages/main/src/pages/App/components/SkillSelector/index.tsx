import $i18n from '@/i18n';
import { CreateSkillBtn } from '@/pages/Component/Skill';
import { listSkills } from '@/services/skill';
import { ISkill, SKILL_MAX_LIMIT } from '@/types/skill';
import {
  Button,
  Drawer,
  Empty,
  IconFont,
  Input,
  message,
  Pagination,
} from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Flex, Spin } from 'antd';
import { debounce } from 'lodash-es';
import { useEffect, useState } from 'react';
import SkillListItem from './SkillListItem';

export interface ISkillSelectorProps {
  selectedSkills?: ISkill[];
  onOk: (skills: ISkill[]) => void;
  onClose: () => void;
}

export function SkillSelectDrawer(props: ISkillSelectorProps) {
  const [filterParams, setFilterParams] = useSetState({
    current: 1,
    size: 10,
    name: '',
  });
  const [total, setTotal] = useState(0);
  const [list, setList] = useState<ISkill[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<ISkill[]>(props.selectedSkills || []);

  const fetchList = () => {
    setLoading(true);
    listSkills(filterParams)
      .then((res) => {
        setList(res.data.records || []);
        setTotal(res.data.total || 0);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchList();
  }, [filterParams]);

  const onInputChange = debounce((e) => {
    setFilterParams({ current: 1, name: e.target.value });
  }, 500);

  const toggleSkill = (item: ISkill, checked: boolean) => {
    if (checked) {
      if (selected.length >= SKILL_MAX_LIMIT) {
        message.warning(
          $i18n.get({
            id: 'main.pages.App.components.SkillSelector.reachedMaxLimit',
            dm: '已达到最大数量限制',
          }),
        );
        return;
      }
      setSelected([...selected, item]);
    } else {
      setSelected(selected.filter((s) => s.skill_id !== item.skill_id));
    }
  };

  const reachedLimit = selected.length >= SKILL_MAX_LIMIT;

  return (
    <Drawer
      title={$i18n.get({
        id: 'main.pages.App.components.SkillSelector.title',
        dm: '选择技能',
      })}
      open
      width={560}
      onClose={props.onClose}
      footer={
        <Flex justify="space-between" align="center" style={{ width: '100%' }}>
          <span
            style={{
              fontSize: 12,
              color: 'var(--ag-ant-color-text-tertiary)',
            }}
          >
            {$i18n.get({
              id: 'main.pages.App.components.SkillSelector.selected',
              dm: '已选',
            })}
            {` ${selected.length}/${SKILL_MAX_LIMIT}`}
          </span>
          <Flex gap={8}>
            <Button onClick={props.onClose}>
              {$i18n.get({
                id: 'main.pages.App.components.SkillSelector.cancel',
                dm: '取消',
              })}
            </Button>
            <Button type="primary" onClick={() => props.onOk(selected)}>
              {$i18n.get({
                id: 'main.pages.App.components.SkillSelector.confirm',
                dm: '确定',
              })}
            </Button>
          </Flex>
        </Flex>
      }
    >
      <Flex justify="space-between" align="center" className="mb-[24px]" gap={12}>
        <Input
          onChange={onInputChange}
          prefix={<IconFont type="spark-search-line" />}
          placeholder={$i18n.get({
            id: 'main.pages.App.components.SkillSelector.search',
            dm: '搜索技能',
          })}
          allowClear
          style={{ width: 240 }}
        />
        <CreateSkillBtn onSuccess={fetchList} />
      </Flex>
      {loading ? (
        <Flex justify="center" className="py-[48px]">
          <Spin />
        </Flex>
      ) : list.length ? (
        <Flex vertical gap={16}>
          {list.map((item) => {
            const checked = selected.some((s) => s.skill_id === item.skill_id);
            return (
              <SkillListItem
                key={item.skill_id}
                item={item}
                checked={checked}
                disabled={reachedLimit && !checked}
                onChange={(next) => toggleSkill(item, next)}
              />
            );
          })}
          <Flex justify="flex-end" className="mt-[4px]">
            <Pagination
              pageSize={filterParams.size}
              current={filterParams.current}
              total={total}
              hideOnSinglePage
              onChange={(page, pageSize) =>
                setFilterParams({ current: page, size: pageSize })
              }
            />
          </Flex>
        </Flex>
      ) : (
        <Empty
          title={$i18n.get({
            id: 'main.pages.App.components.SkillSelector.empty',
            dm: '暂无技能',
          })}
        />
      )}
    </Drawer>
  );
}
