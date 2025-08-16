import { useStore } from '@/flow/context';
import { useNodesInteraction } from '@/hooks';
import $i18n from '@/i18n';
import { Input } from '@spark-ai/design';
import { useStore as useReactFlowStore } from '@xyflow/react';
import { Tree } from 'antd';
import { debounce } from 'lodash-es';
import React, { memo, useCallback, useMemo, useState } from 'react';
import CustomIcon from '../CustomIcon';
import './index.less';

const HighlightText = ({
  text,
  keyword,
}: {
  text: string;
  keyword: string;
}) => {
  if (!keyword) return <>{text}</>;

  const parts = text.split(new RegExp(`(${keyword})`, 'gi'));
  return (
    <span>
      {parts.map((part, i) =>
        part.toLowerCase() === keyword.toLowerCase() ? (
          <span key={i} className="bg-yellow-200">
            {part}
          </span>
        ) : (
          part
        ),
      )}
    </span>
  );
};

export default memo(function NodeTree() {
  const nodes = useReactFlowStore((store) => store.nodes);
  const nodeSchemaMap = useStore((store) => store.nodeSchemaMap);
  const { handleNodeClickByNodeId } = useNodesInteraction();
  const [searchValue, setSearchValue] = useState('');
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);

  const findParentKeys = (nodes: any[], targetKey: string): string[] => {
    const parentKeys: string[] = [];

    const findParent = (items: any[], parentId?: string) => {
      for (const item of items) {
        if (item.key === targetKey) {
          if (parentId) parentKeys.push(parentId);
          return true;
        }
        if (item.children) {
          if (findParent(item.children, item.key)) {
            if (parentId) parentKeys.push(parentId);
            return true;
          }
        }
      }
      return false;
    };

    findParent(nodes);
    return parentKeys;
  };

  const filterNodes = useMemo(() => {
    return nodes.map((item) => ({
      title: (
        <div className="spark-flow-node-tree-sub-title">
          <HighlightText
            text={item.data.label as string}
            keyword={searchValue}
          />
        </div>
      ),

      key: item.id,
      type: item.type,
      parentId: item.parentId,
      label: item.data.label as string,
    }));
  }, [nodes, searchValue]);

  const treeData = useMemo(() => {
    let newTree: any[] = [];
    const subFlowNodesMap: Record<string, any[]> = {};
    filterNodes.forEach((item) => {
      if (item.parentId) {
        subFlowNodesMap[item.parentId] = [
          ...(subFlowNodesMap[item.parentId] || []),
          item,
        ];
      } else {
        newTree.push(item);
      }
    });
    newTree = newTree.map((item) => {
      if (['Iterator', 'Parallel'].includes(item.type)) {
        return {
          ...item,
          children: subFlowNodesMap[item.key],
        };
      }
      return item;
    });
    const categoryMap: Record<string, any[]> = {};
    newTree.forEach((item) => {
      if (categoryMap[item.type]) {
        categoryMap[item.type].push(item);
      } else {
        categoryMap[item.type] = [item];
      }
    });

    return Object.keys(categoryMap).map((itemKey) => {
      return {
        title: (
          <div className="flex items-center spark-flow-node-tree-title">
            <CustomIcon
              className="text-[20px]"
              type={nodeSchemaMap[itemKey].iconType}
            />

            <HighlightText
              text={nodeSchemaMap[itemKey].title}
              keyword={searchValue}
            />
          </div>
        ),

        key: itemKey,
        children: categoryMap[itemKey],
        renderType: itemKey,
        selectable: false,
        label: nodeSchemaMap[itemKey].title,
      };
    });
  }, [filterNodes, nodeSchemaMap, searchValue]);

  const getAllKeys = (nodes: any[]): React.Key[] => {
    const keys: React.Key[] = [];

    const traverse = (items: any[]) => {
      items.forEach((item) => {
        keys.push(item.key);
        if (item.children) {
          traverse(item.children);
        }
      });
    };

    traverse(nodes);
    return keys;
  };

  const updateExpandedKeys = useCallback(
    (value: string) => {
      if (!value) {
        setExpandedKeys(getAllKeys(treeData));
        return;
      }

      const matchedKeys = filterNodes
        .filter((node) =>
          node.label.toLowerCase().includes(value.toLowerCase()),
        )
        .map((node) => node.key);

      const parentKeys = matchedKeys.reduce((acc: string[], key) => {
        return [...acc, ...findParentKeys(treeData, key as string)];
      }, []);

      setExpandedKeys(Array.from(new Set([...matchedKeys, ...parentKeys])));
    },
    [filterNodes, treeData],
  );

  const debouncedUpdateExpandedKeys = useMemo(
    () => debounce(updateExpandedKeys, 300),
    [updateExpandedKeys],
  );

  React.useEffect(() => {
    debouncedUpdateExpandedKeys(searchValue);

    return () => {
      debouncedUpdateExpandedKeys.cancel();
    };
  }, [searchValue, debouncedUpdateExpandedKeys]);

  return (
    <div className="spark-flow-node-tree h-full flex flex-col">
      <div className="spark-flow-node-tree-search flex-shrink-0">
        <Input
          placeholder={$i18n.get({
            id: 'spark-flow.components.NodeTree.index.searchNodeName',
            dm: '搜索节点名称',
          })}
          prefix={<CustomIcon type="spark-search-line" />}
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
        />
      </div>
      <div className="flex-1 overflow-y-auto px-[16px]">
        <Tree
          expandedKeys={expandedKeys}
          onExpand={(keys) => setExpandedKeys(keys)}
          switcherIcon={<CustomIcon type="spark-down-line" />}
          treeData={treeData}
          onSelect={([key]) => {
            handleNodeClickByNodeId(key as string);
          }}
        />
      </div>
    </div>
  );
});
