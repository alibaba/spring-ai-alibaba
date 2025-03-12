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

import ExpandNodeToolBar from '@/pages/Graph/Design/types/ExpandNodeToolBar';
import { graphState } from '@/store/GraphState';
import { Icon } from '@iconify/react';
import { useProxy } from '@umijs/max';
import { Handle, Position } from '@xyflow/react';
import { Flex, Tag } from 'antd';
import React, { memo } from 'react';
import './base.less';

interface Props {
  data: any;
}

const LLMNode: React.FC<Props> = ({ data }) => {
  const graphStore = useProxy(graphState);
  const onClick = () => {
    graphStore.formDrawer.isOpen = true;
  };

  return (
    <div
      onClick={onClick}
      style={{
        width: data.width,
        height: data.height,
      }}
    >
      <ExpandNodeToolBar></ExpandNodeToolBar>
      <Handle
        type="target"
        position={Position.Left}
        className={'graph-node__handle'}
      ></Handle>
      <Handle
        className={'graph-node__handle'}
        type="source"
        id="source"
        position={Position.Right}
      />

      <Flex vertical={true} className="cust-node-wrapper">
        <Flex>
          <div className="node-type">
            <Icon icon="material-symbols:android-safety-outline"></Icon>LLM Node
          </div>
        </Flex>
        <Flex className="cust-node-wrapper">
          <Tag color="volcano" className="node-body-tag">
            {data.label}
          </Tag>
        </Flex>
      </Flex>
    </div>
  );
};

export default memo(LLMNode);
