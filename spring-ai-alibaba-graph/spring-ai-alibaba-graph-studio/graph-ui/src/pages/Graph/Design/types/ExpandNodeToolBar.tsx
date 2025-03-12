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

import { Icon } from '@iconify/react';
import { Button } from 'antd';
import ButtonGroup from 'antd/es/button/button-group';
import React, { memo } from 'react';
import './base.less';

/**
 * todo
 * @constructor
 */
const ExpandNodeToolBar: React.FC = () => {
  return (
    <>
      <div className={'toolbar-wrapper'}>
        <ButtonGroup size={'small'}>
          <Button>
            <Icon icon="material-symbols:play-arrow" />
          </Button>
          <Button>
            <Icon icon="material-symbols:edit-note" />
          </Button>
          <Button>
            <Icon icon="material-symbols:more-vert" />
          </Button>
        </ButtonGroup>
      </div>
    </>
  );
};

export default memo(ExpandNodeToolBar);
