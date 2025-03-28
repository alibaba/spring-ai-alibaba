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
import { useProxy } from '@umijs/max';
import type { Node } from '@xyflow/react';
import { Drawer, Form } from 'antd';
import { useCallback, useRef } from 'react';
import NextStepNodes from './common/NextStepNodes';
import EndNodeForm from './EndNodeForm';
import StartNodeForm from './StartNodeForm';

const FormDrawer = () => {
  const formRef = useRef<any>(null);
  const graphStore = useProxy(graphState);
  const nodeId2NodeDataMap: Record<string, Node> = {};
  graphStore.nodes.forEach((node) => {
    if (node.id) {
      nodeId2NodeDataMap[node.id] = node;
    }
  });
  const nodeId = graphStore.currentNodeId as string;
  const currentNode = nodeId2NodeDataMap[nodeId];
  const nextStepNodes = Array.from(
    new Set(
      graphStore.edges
        .filter((edge) => edge.source === nodeId)
        .map((edge) => {
          return nodeId2NodeDataMap[edge.target];
        }),
    ),
  );

  const handleFormFinish = useCallback(
    (name: string, { values }: { values: any }) => {
      if (nodeId) {
        console.log('values', values);
        graphStore.formDrawer.formDataMap[nodeId] = values;
      }
    },
    [nodeId],
  );

  const handleCancel = () => {
    // 触发当前表单的提交
    if (formRef.current) {
      formRef.current.submit();
    }
    graphStore.formDrawer.isOpen = false;
  };

  const getDynamicForm = useCallback(() => {
    const initialValues = graphStore.formDrawer.formDataMap[nodeId] || {};

    const componentMap: Record<string, JSX.Element> = {
      start: <StartNodeForm form={formRef.current} />,
      end: <EndNodeForm form={formRef.current} />,
    };

    const content = componentMap[currentNode?.type as string];

    return (
      content && (
        <Form
          ref={formRef}
          name={`form-${nodeId}`}
          initialValues={initialValues}
        >
          {content}
        </Form>
      )
    );
  }, [currentNode, nodeId]);

  if (!currentNode) return null;

  return (
    <Drawer
      destroyOnClose
      title={(currentNode?.data?.label as string) || ''}
      placement="right"
      onClose={handleCancel}
      open={graphStore.formDrawer?.isOpen}
      width={500}
    >
      <Form.Provider onFormFinish={handleFormFinish}>
        {getDynamicForm()}
        <NextStepNodes
          currentNode={currentNode}
          nextStepNodes={nextStepNodes}
        />
      </Form.Provider>
    </Drawer>
  );
};

export default FormDrawer;
