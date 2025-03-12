/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { graphState } from '@/store/GraphState';
import { DeleteFilled } from '@ant-design/icons';
import { useProxy } from '@umijs/max';
import type { Node } from '@xyflow/react';
import { Button, Flex, Space, Tag, Typography } from 'antd';

interface INextStepNodesProps {
  currentNode: Node;
  nextStepNodes: Array<Node>;
}

const NextStepNodes = (props: INextStepNodesProps) => {
  const graphStore = useProxy(graphState);
  const { nextStepNodes, currentNode } = props;

  const onDeleteNode = (nodeId: string) => {
    graphStore.nodes = graphStore.nodes.filter((node) => node.id !== nodeId);
    graphStore.edges = graphStore.edges.filter(
      (edge) => edge.source !== nodeId && edge.target !== nodeId,
    );
  };

  if (!currentNode || currentNode.type === 'end' || !nextStepNodes?.length) {
    return null;
  }

  return (
    <>
      <Typography.Title level={5}>NEXT STEP</Typography.Title>
      <Tag style={{ width: '100%', padding: '4px' }}>
        {nextStepNodes?.map((node, index) => {
          return (
            <Button key={'nextStepNode' + index} style={{ width: '100%' }}>
              <Flex justify="space-between" style={{ width: '100%' }}>
                <Typography.Text>
                  {node.type} #{node.id}
                </Typography.Text>
                <Space>
                  <DeleteFilled onClick={() => onDeleteNode(node.id)} />
                </Space>
              </Flex>
            </Button>
          );
        })}
      </Tag>
    </>
  );
};

export default NextStepNodes;
