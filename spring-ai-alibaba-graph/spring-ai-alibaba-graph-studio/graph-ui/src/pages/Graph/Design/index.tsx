import { Routes, Route } from 'react-router-dom';
import FlowEditor from './components/FlowEditor ';

function App() {
  return (
    <Routes>
      <Route path="/" element={<FlowEditor   />} />
    </Routes>

  );
}

export default App;
