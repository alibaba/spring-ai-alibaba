import React, { useEffect, useState } from 'react';
import { Routes, Route } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import ErrorBoundary from './components/ErrorBoundary';
import Layout from './components/Layout';
import PromptsPage from './pages/prompts/prompts';
import PlaygroundPage from './pages/playground/playground';
import PromptDetailPage from './pages/prompts/prompt-detail/prompt-detail';
import TracingPage from './pages/tracing/tracing';
import VersionHistoryPage from './pages/prompts/version-history/version-history';
import Experiment from './pages/evaluation/experiment';
import ExperimentCreate from './pages/evaluation/experiment/experimentCreate';
import ExperimentDetail from './pages/evaluation/experiment/experimentDetail';
import EvaluationGather from './pages/evaluation/gather';
import GatherCreate from './pages/evaluation/gather/gatherCreate';
import GatherDetail from './pages/evaluation/gather/gatherDetail';
import EvaluationEvaluator from './pages/evaluation/evaluator';
import EvaluationEvaluatorDetail from './pages/evaluation/evaluator/evaluator-detail';
import EvaluationEvaluatorDebug from './pages/evaluation/evaluator/evaluator-debug';
import { ModelsContext } from './context/models';
import PromptAPI from './services';

function App() {

  const [models, setModels] = useState([]);
  const [modelNameMap, setModelNameMap] = useState({});

  useEffect(() => {
    PromptAPI.getModels().then(res => {
      setModelNameMap(res.data.pageItems.reduce((acc, item) => {
        acc[item.id] = item.name;
        return acc;
      }, {}));
      setModels(res.data.pageItems);
    });
  }, [])

  return (
    <ConfigProvider locale={zhCN}>
      <ErrorBoundary>
        <ModelsContext.Provider value={{
          models,
          modelNameMap,
          setModels
        }}>
          <div className="min-h-screen bg-gray-50">
            <Layout>
              <Routes>
                <Route path="/" element={<PromptsPage />} />
                <Route path="/prompts" element={<PromptsPage />} />
                <Route path="/prompt-detail" element={<PromptDetailPage />} />
                <Route path="/version-history" element={<VersionHistoryPage />} />
                <Route path="/playground" element={<PlaygroundPage />} />
                <Route path="/tracing" element={<TracingPage />} />
                <Route path="/version-history" element={<VersionHistoryPage />} />
                <Route path="/evaluation-experiment" element={<Experiment />} />
                <Route path="/evaluation-experiment/create" element={<ExperimentCreate />} />
                <Route path="/evaluation-experiment/detail/:id" element={<ExperimentDetail />} />
                <Route path="/evaluation-gather" element={<EvaluationGather />} />
                <Route path="/evaluation-gather/create" element={<GatherCreate />} />
                <Route path="/evaluation-gather/detail/:id" element={<GatherDetail />} />
                <Route path="/evaluation-evaluator" element={<EvaluationEvaluator />} />
                <Route path="/evaluation-evaluator/:id" element={<EvaluationEvaluatorDetail />} />
                <Route path="/evaluation-debug" element={<EvaluationEvaluatorDebug />} />
              </Routes>
            </Layout>
          </div>
        </ModelsContext.Provider>
      </ErrorBoundary>
    </ConfigProvider>
  );
}

export default App;
