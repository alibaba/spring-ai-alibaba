import { Menu, MenuItem } from '@mui/material';
import React from 'react';

interface NodeTypeSelectorProps {
  anchorEl: null | HTMLElement;
  open: boolean;
  onClose: () => void;
  onSelectNodeType: (nodeType: string) => void;
}

const NodeTypeSelector: React.FC<NodeTypeSelectorProps> = ({
  anchorEl,
  open,
  onClose,
  onSelectNodeType,
}) => {
  return (
    <Menu anchorEl={anchorEl} open={open} onClose={onClose}>
      <MenuItem onClick={() => onSelectNodeType('typeA')}>类型A</MenuItem>
      <MenuItem onClick={() => onSelectNodeType('typeB')}>类型B</MenuItem>
      <MenuItem onClick={() => onSelectNodeType('typeC')}>类型C</MenuItem>
    </Menu>
  );
};

export default NodeTypeSelector;
