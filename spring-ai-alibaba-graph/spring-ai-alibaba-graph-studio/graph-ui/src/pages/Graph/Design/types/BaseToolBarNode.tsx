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

import { EditOutlined, StepForwardOutlined } from '@ant-design/icons';
import { Handle, NodeToolbar, Position } from '@xyflow/react';
import { Button } from 'antd';
import ButtonGroup from 'antd/es/button/button-group';
import React, { memo } from 'react';
import './base.less';

interface Props {
  data: any;
  children: any;
}

const ToolbarNode: React.FC<Props> = ({ data, children }) => {
  return (
    <>
      <NodeToolbar align={'end'} isVisible>
        <ButtonGroup size={'small'}>
          <Button>
            <StepForwardOutlined
              style={{
                color: 'var(--ant-color-primary)',
              }}
            />
          </Button>
          <Button>
            <EditOutlined
              style={{
                color: 'var(--ant-color-primary)',
              }}
            />
          </Button>
          <Button
            style={{
              color: 'var(--ant-color-primary)',
            }}
          >
            ...
          </Button>
        </ButtonGroup>
      </NodeToolbar>

      <Handle
        type="target"
        position={Position.Left}
        className={'graph-node__handle'}
      ></Handle>
      <Handle
        className={'graph-node__handle'}
        type="source"
        position={Position.Right}
      />
      <div
        style={{
          marginBottom: '10px',
          fontWeight: 'bold',
          fontSize: '16px',
        }}
      >
        {data.label}
      </div>
      {children}
    </>
  );
};

export default memo(ToolbarNode);
