import Search from '@/components/Search';
import $i18n from '@/i18n';
import { getKnowledgeList } from '@/services/knowledge';
import { IKnowledgeListItem } from '@/types/knowledge';
import {
  Button,
  Drawer,
  Empty,
  IconFont,
  message,
  Modal,
} from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { Checkbox, Pagination, Spin, Typography } from 'antd';
import classNames from 'classnames';
import { useMemo, useRef, useState } from 'react';
import styles from './index.module.less';

const MAX_LEN = 10;

export interface IKnowledgeSelectorProps {
  value: IKnowledgeListItem[];
  onChange: (val: IKnowledgeListItem[]) => void;
  mode?: 'modal' | 'drawer';
}

export function KnowledgeSelectorItem(props: {
  item: IKnowledgeListItem;
  value: IKnowledgeListItem[];
  onCheckChange: (val: boolean) => void;
}) {
  const checked = useMemo(() => {
    return props.value.some((item) => item.kb_id === props.item.kb_id);
  }, [props.value, props.item.kb_id]);

  return (
    <div
      className={classNames(styles.card, {
        [styles.active]: checked,
      })}
      onClick={() => props.onCheckChange(!checked)}
    >
      <Checkbox
        checked={checked}
        onChange={(e) => props.onCheckChange(e.target.checked)}
      />

      <img src={'/images/knowledge.svg'} alt="" />
      <div className="flex flex-col flex-1 w-[1px] gap-[4px]">
        <div className="flex gap-[8px]">
          <Typography.Text ellipsis className={styles['title']}>
            {props.item.name}
          </Typography.Text>
          <span>
            {$i18n.get(
              {
                id: 'main.pages.App.components.KnowledgeSelector.index.knowledgeCount',
                dm: '{var1}个知识',
              },
              { var1: props.item.total_docs },
            )}
          </span>
        </div>
        <Typography.Text
          ellipsis={{ tooltip: props.item.description }}
          className={styles['desc']}
        >
          {props.item.description}
        </Typography.Text>
      </div>
    </div>
  );
}

export default function KnowledgeSelector(props: IKnowledgeSelectorProps) {
  const { mode = 'modal' } = props;
  const [state, setState] = useSetState({
    list: [] as IKnowledgeListItem[],
    loading: false,
    current: 1,
    size: 10,
    name: '',
    total: 0,
  });
  const isSearchRef = useRef(false);

  const fetchList = (params: any = {}) => {
    setState({ loading: true });
    const extraParams = isSearchRef.current ? { name: state.name } : {};
    getKnowledgeList({
      current: state.current,
      size: state.size,
      ...params,
      ...extraParams,
    })
      .then((res) => {
        setState({
          list: res.records,
          total: res.total,
          loading: false,
        });
      })
      .catch(() => {
        setState({
          loading: false,
        });
      });
  };

  const handleSearch = () => {
    isSearchRef.current = !!state.name;
    setState({ current: 1 });
    fetchList({ current: 1 });
  };

  const handleCheckChange = (checked: boolean, payload: IKnowledgeListItem) => {
    if (checked) {
      if (props.value.length >= MAX_LEN) {
        message.warning(
          $i18n.get({
            id: 'main.pages.App.components.KnowledgeSelector.index.maxTenKnowledge',
            dm: '最多只能选择10个知识库',
          }),
        );
        return;
      }
      props.onChange([...props.value, payload]);
    } else {
      props.onChange(
        props.value.filter((item) => payload.kb_id !== item.kb_id),
      );
    }
  };

  useMount(() => {
    fetchList();
  });

  return (
    <>
      <div className="flex-justify-between px-[24px]">
        <Search
          onSearch={handleSearch}
          placeholder={$i18n.get({
            id: 'main.pages.App.components.KnowledgeSelector.index.enterKnowledgeName',
            dm: '请输入知识库名称',
          })}
          onChange={(val) => setState({ name: val })}
          value={state.name}
          className={styles.search}
        />

        <Button
          onClick={() => window.open('/knowledge')}
          icon={<IconFont type="spark-plus-line" />}
        >
          {$i18n.get({
            id: 'main.pages.App.components.KnowledgeSelector.index.createNewKnowledge',
            dm: '创建新知识库',
          })}
        </Button>
      </div>
      <div className={classNames(styles['list-wrap'], styles[mode])}>
        {state.loading ? (
          <div className="loading-center">
            <Spin className="loading-center" spinning />
          </div>
        ) : !state.list.length ? (
          <div className="loading-center">
            <Empty
              title={
                isSearchRef.current
                  ? $i18n.get({
                      id: 'main.pages.App.components.KnowledgeSelector.index.noSearchResult',
                      dm: '未搜索出来符合条件的知识库',
                    })
                  : $i18n.get({
                      id: 'main.pages.App.components.KnowledgeSelector.index.noKnowledge',
                      dm: '暂无知识库',
                    })
              }
              description={
                isSearchRef.current
                  ? $i18n.get({
                      id: 'main.pages.App.components.KnowledgeSelector.index.tryAnotherSearch',
                      dm: '换个搜索条件试试',
                    })
                  : $i18n.get({
                      id: 'main.pages.App.components.KnowledgeSelector.index.goCreateFirst',
                      dm: '请先前往知识库管理创建知识库',
                    })
              }
            >
              {!isSearchRef.current && (
                <Button
                  className="mt-[12px]"
                  onClick={() => window.open('/knowledge')}
                  type="primary"
                >
                  {$i18n.get({
                    id: 'main.pages.App.components.KnowledgeSelector.index.goCreate',
                    dm: '前往创建',
                  })}
                </Button>
              )}
            </Empty>
          </div>
        ) : (
          <div className="flex flex-col gap-[16px]">
            {state.list.map((item) => (
              <KnowledgeSelectorItem
                key={item.kb_id}
                item={item}
                value={props.value}
                onCheckChange={(val) => {
                  handleCheckChange(val, item);
                }}
              />
            ))}
          </div>
        )}
      </div>
      <Pagination
        className={styles.page}
        current={state.current}
        pageSize={state.size}
        total={state.total}
        hideOnSinglePage
        onChange={(page, pageSize) => {
          setState({ current: page, size: pageSize });
          fetchList({ current: page, size: pageSize });
        }}
      />
    </>
  );
}

export interface IKnowledgeSelectorModalProps {
  onOk: (val: IKnowledgeListItem[]) => void;
  onClose: () => void;
  value?: IKnowledgeListItem[];
}

export function KnowledgeSelectorModal(props: IKnowledgeSelectorModalProps) {
  const [value, setValue] = useState<IKnowledgeListItem[]>(props.value || []);

  return (
    <Modal
      title={$i18n.get({
        id: 'main.pages.App.components.KnowledgeSelector.index.selectKnowledge',
        dm: '选择知识库',
      })}
      onOk={() => props.onOk(value)}
      open
      width={640}
      className={styles['modal-wrap']}
      onCancel={props.onClose}
    >
      <div className="py-[18px] gap-[12px] flex flex-col">
        <KnowledgeSelector value={value} onChange={setValue} />
      </div>
    </Modal>
  );
}

export function KnowledgeSelectorDrawer(props: IKnowledgeSelectorModalProps) {
  const [value, setValue] = useState<IKnowledgeListItem[]>(props.value || []);

  return (
    <Drawer
      title={$i18n.get({
        id: 'main.pages.App.components.KnowledgeSelector.index.selectKnowledge',
        dm: '选择知识库',
      })}
      footer={
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
          className="w-full"
        >
          <div
            style={{
              color: 'var(--ag-ant-color-text-tertiary)',
              fontSize: '14px',
              fontWeight: 'normal',
              lineHeight: '24px',
            }}
          >
            {!!value?.length &&
              $i18n.get(
                {
                  id: 'main.pages.App.components.KnowledgeSelector.index.addedKnowledgeBase',
                  dm: '已添加知识库{var1}/{var2}个',
                },
                { var1: value?.length, var2: MAX_LEN },
              )}
          </div>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              gap: 12,
            }}
          >
            <Button onClick={props.onClose}>
              {$i18n.get({
                id: 'main.pages.App.components.KnowledgeSelector.index.cancel',
                dm: '取消',
              })}
            </Button>
            <Button type="primary" onClick={() => props.onOk(value)}>
              {$i18n.get({
                id: 'main.pages.App.components.KnowledgeSelector.index.confirm',
                dm: '确定',
              })}
            </Button>
          </div>
        </div>
      }
      onClose={props.onClose}
      open
      width={640}
      className={styles['drawer-wrap']}
    >
      <div className="py-[18px] h-full overflow-y-auto flex flex-col gap-[12px]">
        <KnowledgeSelector mode="drawer" value={value} onChange={setValue} />
      </div>
    </Drawer>
  );
}
