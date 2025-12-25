import $i18n from '@/i18n';
import { IconFont, Tag, Tooltip } from '@spark-ai/design';
import {
  extractVariables,
  INodeSchema,
  IWorkFlowNode,
  operatorLabelRender,
  useReactFlowStore,
  useStore,
} from '@spark-ai/flow';
import { Flex } from 'antd';
import classNames from 'classnames';
import { memo, useMemo } from 'react';
import styles from './index.module.less';

export interface IJudgeResultProps {
  input?: string;
  multiBranchResults?: Array<{
    condition_id: string;
    target_ids: string[];
  }>;
}

interface IConditionResult {
  conditionId: string;
  logic: string;
  label: string;
  subBranches: Array<{
    leftKey: string;
    leftValue?: string;
    rightKey?: string;
    rightValue?: string;
    operator: string;
    leftInfo: {
      nodeName: string;
      variableKey: string;
      nodeIconType: string;
    } | null;
    rightInfo: {
      nodeName: string;
      variableKey: string;
      nodeIconType: string;
    } | null;
  }>;
}

const logicMap = {
  and: $i18n.get({
    id: 'main.pages.App.Workflow.components.NodeResultPanel.JudgeResult.all',
    dm: '所有',
  }),
  or: $i18n.get({
    id: 'main.pages.App.Workflow.components.NodeResultPanel.JudgeResult.any',
    dm: '任意',
  }),
};

const transformVariableKey = ({
  text,
  nodes,
  nodeSchemaMap,
}: {
  text?: string;
  nodes: IWorkFlowNode[];
  nodeSchemaMap: Record<string, INodeSchema>;
}) => {
  if (!text) return null;
  const finalValue = extractVariables(text.replace(/[\[]]/g, ''))[0];
  const list = finalValue.split('.');
  if (!list.length) return null;
  const [nodeId, ...variableKeyList] = list;
  if (nodeId === 'sys' || nodeId === 'conversation') {
    return {
      nodeName: nodeId,
      variableKey: variableKeyList[variableKeyList.length - 1],
      nodeIconType: nodeSchemaMap[nodeId]?.iconType,
    };
  }
  const targetNode = nodes.find((node) => node.id === nodeId);
  if (!targetNode) return null;
  return {
    nodeName: targetNode.data.label as string,
    variableKey: variableKeyList[variableKeyList.length - 1],
    nodeIconType: nodeSchemaMap[targetNode.type]?.iconType,
  };
};

function SubBranchItem(props: {
  subBranch: IConditionResult['subBranches'][number];
}) {
  const { subBranch } = props;
  return (
    <Flex
      align="center"
      className={styles['condition-sub-branch-item']}
      gap={8}
    >
      {subBranch.leftInfo ? (
        <Tooltip
          title={
            <Flex
              className={styles['condition-item-variable-tooltip']}
              gap={8}
              align="center"
            >
              <IconFont type={subBranch.leftInfo.nodeIconType} />
              <span>
                {subBranch.leftInfo.nodeName}/
                <span className={styles['condition-item-variable-key']}>
                  {subBranch.leftInfo.variableKey}
                </span>
              </span>
            </Flex>
          }
        >
          <Flex
            className={styles['condition-item-left']}
            align="center"
            gap={8}
          >
            <span>
              {subBranch.leftInfo.variableKey}:&nbsp;{subBranch.leftValue}
            </span>
            <IconFont type="spark-arrow-right-line" />
          </Flex>
        </Tooltip>
      ) : (
        <span className={styles['condition-item-left']}>
          {subBranch.leftKey}:&nbsp;{subBranch.leftValue}
        </span>
      )}
      <span>{operatorLabelRender(subBranch.operator)}</span>
      <Flex className={styles['condition-item-right']} align="center">
        <div className={styles['condition-item-right-icon']}>
          {subBranch.rightKey ? (
            <IconFont size="small" type="spark-quotation-line" />
          ) : (
            <IconFont size="small" type="spark-edit-line" />
          )}
        </div>
        <div className={styles['condition-item-right-value']}>
          {subBranch.rightValue}
        </div>
      </Flex>
    </Flex>
  );
}

export default memo(function JudgeResult(props: IJudgeResultProps) {
  const nodes = useReactFlowStore((state) => state.nodes) as IWorkFlowNode[];
  const nodeSchemaMap = useStore((state) => state.nodeSchemaMap);

  const conditionResult = useMemo(() => {
    let conditions: IConditionResult[] = [];
    if (!props.input) return conditions;
    try {
      conditions = (JSON.parse(props.input) as IConditionResult[]).map(
        (item) => {
          return {
            ...item,
            subBranches: item.subBranches.map((subBranch) => {
              return {
                ...subBranch,
                leftInfo: transformVariableKey({
                  text: subBranch.leftKey,
                  nodes,
                  nodeSchemaMap,
                }),
                rightInfo: transformVariableKey({
                  text: subBranch.rightKey,
                  nodes,
                  nodeSchemaMap,
                }),
              };
            }),
          };
        },
      );
    } catch (error) {
      console.error(error);
    }
    return conditions;
  }, [props.input, nodes]);

  if (!conditionResult.length) {
    return (
      <Flex
        align="center"
        className={classNames(styles['condition-result-item'], styles.active)}
        justify="space-between"
      >
        <span className={styles['condition-title']}>
          {$i18n.get({
            id: 'main.pages.App.Workflow.components.NodeResultPanel.JudgeResult.hitDefaultCondition',
            dm: '命中【默认】条件',
          })}
        </span>
        <Tag className={styles['condition-result-tag']} color="success">
          {$i18n.get({
            id: 'main.pages.App.Workflow.components.NodeResultPanel.JudgeResult.successHitThisCondition',
            dm: '成功：命中本条件',
          })}
        </Tag>
      </Flex>
    );
  }

  return (
    <div>
      {conditionResult.map((item) => {
        const isActive =
          item.conditionId === props.multiBranchResults?.[0]?.condition_id;
        return (
          <Flex
            vertical
            gap={8}
            className={classNames(styles['condition-result-item'], {
              [styles['active']]: isActive,
            })}
            key={item.conditionId}
          >
            <Flex gap={16} align="center">
              <div
                style={{ maxWidth: 100 }}
                className={styles['condition-title']}
              >
                {item.label}
              </div>
              <Flex className={styles['condition-desc']} align="center" gap={4}>
                <span>
                  {$i18n.get({
                    id: 'main.pages.App.Workflow.components.BranchTitleHeader.index.whenSatisfy',
                    dm: '当满足以下',
                  })}
                </span>
                <span className={styles['logic-label']}>
                  {logicMap[item.logic as keyof typeof logicMap]}
                </span>
                <span>
                  {$i18n.get({
                    id: 'main.pages.App.Workflow.components.BranchTitleHeader.index.conditions',
                    dm: '条件时',
                  })}
                </span>
              </Flex>
              {isActive && (
                <Tag className={styles['condition-result-tag']} color="success">
                  {$i18n.get({
                    id: 'main.pages.App.Workflow.components.NodeResultPanel.JudgeResult.successHitThisCondition',
                    dm: '成功：命中本条件',
                  })}
                </Tag>
              )}
            </Flex>
            {item.subBranches.map((subBranch, index) => {
              return <SubBranchItem key={index} subBranch={subBranch} />;
            })}
          </Flex>
        );
      })}
    </div>
  );
});
