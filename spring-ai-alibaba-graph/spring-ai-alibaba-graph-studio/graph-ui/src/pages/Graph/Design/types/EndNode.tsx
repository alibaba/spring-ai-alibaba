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
import { Icon } from '@iconify/react';
import { useProxy } from '@umijs/max';
import { Handle, Position } from '@xyflow/react';
import { Flex } from 'antd';
import React from 'react';
import './base.less';

type props = {
  data: any;
};
const EndNode: React.FC<props> = ({ data }) => {
  const graphStore = useProxy(graphState);
  const onClick = () => {
    graphStore.formDrawer.isOpen = true;
  };

  return (
    <>
      <div
        onClick={onClick}
        style={{
          width: data.width || '120px',
          height: data.height || '60px',
        }}
      >
        <Handle
          type="target"
          position={Position.Left}
          className={'graph-node__handle'}
        ></Handle>
        <Flex vertical={true} className="cust-node-wrapper center">
          <div className="node-type">
            <Icon
              className="type-icon end"
              icon={'material-symbols:stop-circle'}
            ></Icon>
            End
          </div>
        </Flex>
      </div>
    </>
  );
};

export default EndNode;
