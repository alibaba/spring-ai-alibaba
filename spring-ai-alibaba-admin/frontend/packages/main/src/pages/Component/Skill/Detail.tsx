import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import {
  getSkill,
  listSkillFiles,
  readSkillFile,
} from '@/services/skill';
import {
  ISkill,
  ISkillFileContent,
  ISkillFileNode,
} from '@/types/skill';
import { Empty } from '@spark-ai/design';
import { FileOutlined, FolderOpenOutlined } from '@ant-design/icons';
import { useMount } from 'ahooks';
import { Flex, Spin, Tree, Typography } from 'antd';
import type { DataNode } from 'antd/es/tree';
import { Key, useMemo, useState } from 'react';
import { useParams } from 'umi';
import styles from './Detail.module.less';

function toTreeData(nodes: ISkillFileNode[]): DataNode[] {
  return nodes.map((node) => ({
    key: node.path,
    title: node.name,
    isLeaf: node.type === 'file',
    icon:
      node.type === 'directory' ? <FolderOpenOutlined /> : <FileOutlined />,
    children: node.children?.length ? toTreeData(node.children) : undefined,
  }));
}

function collectExpandKeys(nodes: ISkillFileNode[], depth = 0): string[] {
  const keys: string[] = [];
  for (const node of nodes) {
    if (node.type === 'directory' && depth < 2) {
      keys.push(node.path);
      if (node.children?.length) {
        keys.push(...collectExpandKeys(node.children, depth + 1));
      }
    }
  }
  return keys;
}

function findFirstFile(nodes: ISkillFileNode[]): string | null {
  for (const node of nodes) {
    if (node.type === 'file' && node.name.toLowerCase() === 'skill.md') {
      return node.path;
    }
  }
  for (const node of nodes) {
    if (node.type === 'file') return node.path;
    if (node.children?.length) {
      const nested = findFirstFile(node.children);
      if (nested) return nested;
    }
  }
  return null;
}

export default function SkillDetail() {
  const { id } = useParams<{ id: string }>();
  const [skill, setSkill] = useState<ISkill | null>(null);
  const [files, setFiles] = useState<ISkillFileNode[]>([]);
  const [selectedPath, setSelectedPath] = useState<string>();
  const [fileContent, setFileContent] = useState<ISkillFileContent | null>(
    null,
  );
  const [loading, setLoading] = useState(true);
  const [contentLoading, setContentLoading] = useState(false);
  const [expandedKeys, setExpandedKeys] = useState<Key[]>([]);

  const treeData = useMemo(() => toTreeData(files), [files]);

  const loadFile = async (path: string) => {
    if (!id) return;
    setSelectedPath(path);
    setContentLoading(true);
    try {
      const res = await readSkillFile(id, path);
      setFileContent(res.data);
    } finally {
      setContentLoading(false);
    }
  };

  useMount(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const [skillRes, filesRes] = await Promise.all([
        getSkill(id),
        listSkillFiles(id),
      ]);
      setSkill(skillRes.data);
      const tree = filesRes.data || [];
      setFiles(tree);
      setExpandedKeys(collectExpandKeys(tree));
      const first = findFirstFile(tree);
      if (first) {
        await loadFile(first);
      }
    } finally {
      setLoading(false);
    }
  });

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.Component.index.componentManagement',
            dm: '组件管理',
          }),
          path: '/component/skill',
        },
        {
          title:
            skill?.name ||
            $i18n.get({
              id: 'main.pages.Component.Skill.detail',
              dm: '技能详情',
            }),
        },
      ]}
      loading={loading}
    >
      <div className={styles.page}>
        <div className={styles.header}>
          <div className={styles.title}>{skill?.name}</div>
          <div className={styles.meta}>
            skill_name: {skill?.skill_name}
            {skill?.description ? ` · ${skill.description}` : ''}
          </div>
        </div>
        <Flex className={styles.body}>
          <div className={styles.treePane}>
            <div className={styles.paneTitle}>
              {$i18n.get({
                id: 'main.pages.Component.Skill.fileTree',
                dm: '文件树',
              })}
            </div>
            {treeData.length ? (
              <Tree
                showIcon
                treeData={treeData}
                selectedKeys={selectedPath ? [selectedPath] : []}
                expandedKeys={expandedKeys}
                onExpand={(keys) => setExpandedKeys(keys)}
                onSelect={(keys, info) => {
                  if (!info.node.isLeaf) return;
                  const key = String(keys[0] || '');
                  if (key) loadFile(key);
                }}
              />
            ) : (
              <Empty
                title={$i18n.get({
                  id: 'main.pages.Component.Skill.noFiles',
                  dm: '暂无文件',
                })}
              />
            )}
          </div>
          <div className={styles.contentPane}>
            <div className={styles.paneTitle}>
              {selectedPath ||
                $i18n.get({
                  id: 'main.pages.Component.Skill.selectFile',
                  dm: '选择文件查看内容',
                })}
            </div>
            {contentLoading ? (
              <div className={styles.contentLoading}>
                <Spin />
              </div>
            ) : fileContent?.is_binary ? (
              <Empty
                title={$i18n.get({
                  id: 'main.pages.Component.Skill.binaryFile',
                  dm: '二进制文件，暂不支持预览',
                })}
                description={fileContent.path}
              />
            ) : fileContent?.content != null ? (
              <Typography.Paragraph className={styles.code}>
                <pre>{fileContent.content}</pre>
              </Typography.Paragraph>
            ) : (
              <Empty
                title={$i18n.get({
                  id: 'main.pages.Component.Skill.selectFile',
                  dm: '选择文件查看内容',
                })}
              />
            )}
          </div>
        </Flex>
      </div>
    </InnerLayout>
  );
}
