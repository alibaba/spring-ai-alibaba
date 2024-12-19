import { Box, Button, Drawer, TextField, Typography } from '@mui/material';
import { useAtom } from 'jotai';
import { useCallback, useEffect, useState } from 'react';
import { nodesAtom, selectedNodeAtom } from '../atoms/flowState';

export const NodeEditorDrawer = () => {
  const [selectedNode, setSelectedNode] = useAtom(selectedNodeAtom);
  const [nodes, setNodes] = useAtom(nodesAtom);

  const [nodeLabel, setNodeLabel] = useState('');
  const [nodeIcon, setNodeIcon] = useState('');
  const [nodeBgColor, setNodeBgColor] = useState('');

  // 更新本地状态以反映选中的节点
  useEffect(() => {
    if (selectedNode) {
      const node = nodes.find((n) => n.id === selectedNode);
      if (node) {
        setNodeLabel(node.data.label || '');
        setNodeIcon(node.data.icon || '');
        setNodeBgColor(node.data.background || '');
      }
    }
  }, [selectedNode, nodes]);

  // 保存更新的节点数据
  const handleSave = () => {
    if (!selectedNode) return;

    setNodes((currentNodes) =>
      currentNodes.map((node) =>
        node.id === selectedNode
          ? {
              ...node,
              data: {
                ...node.data,
                label: nodeLabel,
                icon: nodeIcon,
                background: nodeBgColor,
              },
            }
          : node,
      ),
    );
  };

  // 点击画布时关闭抽屉
  const handlePaneClick = useCallback(() => {
    setSelectedNode(null); // 关闭抽屉
  }, [setSelectedNode]);

  return (
    <Drawer
      anchor="right"
      open={!!selectedNode}
      onClose={handlePaneClick} // 画布点击时关闭抽屉
      ModalProps={{
        BackdropProps: { style: { backgroundColor: 'transparent' } },
      }}
    >
      <Box padding={2} width={300}>
        <Typography variant="h6" gutterBottom>
          编辑节点
        </Typography>
        {selectedNode ? (
          <>
            <TextField
              label="节点名称"
              value={nodeLabel}
              onChange={(e) => setNodeLabel(e.target.value)}
              fullWidth
              margin="normal"
            />
            <TextField
              label="图标"
              value={nodeIcon}
              onChange={(e) => setNodeIcon(e.target.value)}
              fullWidth
              margin="normal"
            />
            <TextField
              label="背景颜色"
              value={nodeBgColor}
              onChange={(e) => setNodeBgColor(e.target.value)}
              fullWidth
              margin="normal"
            />
            <Button
              variant="contained"
              color="primary"
              onClick={handleSave}
              fullWidth
              style={{ marginTop: 16 }}
            >
              保存
            </Button>
          </>
        ) : (
          <Typography variant="body2">未选择节点</Typography>
        )}
      </Box>
    </Drawer>
  );
};
