import { Box, Button, Typography } from '@mui/material';
import { useAtom } from 'jotai';
import { nodesAtom } from '../atoms/flowState';

const Toolbar = () => {
  const [nodes, setNodes] = useAtom(nodesAtom);

  const addNode = () => {
    const newNode = {
      id: `node-${nodes.length + 1}`,
      type: 'default',
      position: { x: Math.random() * 400, y: Math.random() * 400 },
      data: { label: `Node ${nodes.length + 1}` },
    };
    setNodes((nds) => [...nds, newNode]);
  };

  return (
    <Box p={2} width="240px">
      <Typography variant="h6">工具栏</Typography>
      <Button variant="contained" color="primary" fullWidth onClick={addNode}>
        添加节点
      </Button>
    </Box>
  );
};

export default Toolbar;
