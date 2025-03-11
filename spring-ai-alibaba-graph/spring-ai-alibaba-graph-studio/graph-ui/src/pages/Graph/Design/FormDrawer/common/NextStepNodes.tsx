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
