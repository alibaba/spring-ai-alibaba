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
