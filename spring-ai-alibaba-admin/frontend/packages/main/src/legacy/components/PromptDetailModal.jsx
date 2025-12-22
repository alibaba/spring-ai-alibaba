import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import VersionHistoryModal from './VersionHistoryModal';
import { buildLegacyPath } from '../utils/path';

const PromptDetailModal = ({ prompt, onClose }) => {
  const navigate = useNavigate();
  const [showVersionHistory, setShowVersionHistory] = useState(false);
  const [activeTab, setActiveTab] = useState('details');

  const handleDebug = () => {
    if (prompt.currentVersion) {
      // Navigate directly to playground with URL parameters
      navigate(buildLegacyPath('/playground', {
        promptKey: prompt.promptKey || prompt.name,
        version: prompt.currentVersion.version,
        fromDetail: 'true'
      }));
      onClose();
    } else {
      alert('没有可用版本进行调试');
    }
  };

  const handleShowHistory = () => {
    setShowVersionHistory(true);
  };

  const handleViewVersionHistory = () => {
    navigate(buildLegacyPath('/version-history', { promptKey: prompt.promptKey || prompt.name }));
    onClose();
  };


  const tabs = [
    { id: 'details', label: '详情', icon: 'fas fa-info-circle' },
    { id: 'versions', label: '版本历史', icon: 'fas fa-history' }
  ];

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 modal-overlay">
      <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full mx-4 max-h-[90vh] overflow-y-auto fade-in">
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold text-gray-900">Prompt详情</h2>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600"
            >
              <i className="fas fa-times"></i>
            </button>
          </div>
          
          {/* Tab 导航 */}
          <div className="border-b border-gray-200">
            <nav className="-mb-px flex space-x-8">
              {tabs.map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`py-2 px-1 border-b-2 font-medium text-sm flex items-center space-x-2 ${
                    activeTab === tab.id
                      ? 'border-primary-500 text-primary-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  <i className={tab.icon}></i>
                  <span>{tab.label}</span>
                </button>
              ))}
            </nav>
          </div>
        </div>
        
        <div className="p-6">
          {activeTab === 'details' && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Prompt Key
                  </label>
                  <div className="px-4 py-2 bg-gray-50 rounded-lg text-gray-900">
                    {prompt.name}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    当前版本
                  </label>
                  <div className="px-4 py-2 bg-gray-50 rounded-lg">
                    {prompt.currentVersion ? (
                      <div className="flex items-center space-x-2">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                          {prompt.currentVersion.version}
                        </span>
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                          <i className="fas fa-check-circle mr-1"></i>
                          正式版本
                        </span>
                      </div>
                    ) : prompt.versions && prompt.versions.some(v => v.versionType === 'pre') ? (
                      <div className="flex items-center space-x-2">
                        <span className="text-gray-500 text-sm">无正式版本</span>
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-orange-100 text-orange-800">
                          <i className="fas fa-flask mr-1"></i>
                          仅PRE版本
                        </span>
                      </div>
                    ) : (
                      <span className="text-gray-500 text-sm">暂无版本</span>
                    )}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    创建时间
                  </label>
                  <div className="px-4 py-2 bg-gray-50 rounded-lg text-gray-900">
                    {prompt.createdAt}
                  </div>
                </div>
              </div>

              <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    标签
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {prompt.tags && prompt.tags.length > 0 ? (
                      prompt.tags.map((tag, index) => (
                        <span
                          key={index}
                          className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-gray-100 text-gray-800"
                        >
                          {tag}
                        </span>
                      ))
                    ) : (
                      <span className="text-gray-500 text-sm">无标签</span>
                    )}
                  </div>
              </div>

              {prompt.description && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    描述
                  </label>
                  <div className="px-4 py-3 bg-gray-50 rounded-lg text-gray-900">
                    {prompt.description}
                  </div>
                </div>
              )}

              {prompt.currentVersion && (
                <>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      当前版本内容
                    </label>
                    <div className="px-4 py-3 bg-gray-50 rounded-lg text-gray-900 whitespace-pre-wrap font-mono text-sm">
                      {prompt.currentVersion.content}
                    </div>
                  </div>

                  {prompt.currentVersion.parameters && prompt.currentVersion.parameters.length > 0 && (
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        参数列表
                      </label>
                      <div className="flex flex-wrap gap-2">
                        {prompt.currentVersion.parameters.map((param, index) => (
                          <span
                            key={index}
                            className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800"
                          >
                            {param}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {prompt.currentVersion.modelConfig && (
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        模型配置
                      </label>
                      <div className="px-4 py-3 bg-gray-50 rounded-lg">
                        <div className="grid grid-cols-2 gap-4 text-sm">
                          <div>
                            <span className="font-medium text-gray-700">模型：</span>
                            <span className="ml-2 text-gray-900">{prompt.currentVersion.modelConfig.modelId}</span>
                          </div>
                          <div>
                            <span className="font-medium text-gray-700">最大令牌：</span>
                            <span className="ml-2 text-gray-900">{prompt.currentVersion.modelConfig.maxTokens}</span>
                          </div>
                          <div>
                            <span className="font-medium text-gray-700">Temperature：</span>
                            <span className="ml-2 text-gray-900">{prompt.currentVersion.modelConfig.temperature}</span>
                          </div>
                          <div>
                            <span className="font-medium text-gray-700">Top P：</span>
                            <span className="ml-2 text-gray-900">{prompt.currentVersion.modelConfig.topP}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          )}

          {activeTab === 'versions' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-medium text-gray-900">版本历史</h3>
                {prompt.versions && prompt.versions.length > 0 && (
                  <button
                    onClick={handleViewVersionHistory}
                    className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors text-sm"
                  >
                    查看完整历史
                  </button>
                )}
              </div>
              
              <div className="space-y-3 max-h-96 overflow-y-auto">
                {prompt.versions && prompt.versions.length > 0 ? (
                  prompt.versions.slice().reverse().slice(0, 5).map((version, index) => (
                    <div
                      key={version.id}
                      className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50 transition-colors"
                    >
                        <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center space-x-3">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      {version.version}
                    </span>
                    {version.versionType === 'release' ? (
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                        <i className="fas fa-check-circle mr-1"></i>
                        正式版本
                      </span>
                    ) : (
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-orange-100 text-orange-800">
                        <i className="fas fa-flask mr-1"></i>
                        PRE版本
                      </span>
                    )}
                    {prompt.currentVersion && version.id === prompt.currentVersion.id && (
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                        当前版本
                      </span>
                    )}
                  </div>
                  <div className="text-right text-sm text-gray-500">
                    <div>{version.createdAt}</div>
                  </div>
                        </div>
                      
                      <div className="mb-2">
                        <p className="text-sm text-gray-600">
                          {version.description}
                        </p>
                      </div>
                      
                      <div className="text-xs text-gray-500 bg-gray-50 p-2 rounded font-mono">
                        {version.content.substring(0, 100)}
                        {version.content.length > 100 && '...'}
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="text-center py-8">
                    <i className="fas fa-history text-3xl text-gray-300 mb-3"></i>
                    <p className="text-gray-500">暂无版本发布</p>
                  </div>
                )}
              </div>
              
              {prompt.versions && prompt.versions.length > 5 && (
                <div className="text-center pt-4 border-t border-gray-200">
                  <button
                    onClick={handleViewVersionHistory}
                    className="text-primary-600 hover:text-primary-700 text-sm font-medium"
                  >
                    查看全部 {prompt.versions.length} 个版本 →
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="p-6 border-t border-gray-200 flex justify-between">
          {activeTab === 'details' && (
            <button
              onClick={handleShowHistory}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center"
            >
              <i className="fas fa-history mr-2"></i>
              版本对比
            </button>
          )}
          
          {activeTab === 'versions' && (
            <div></div>
          )}
          
          <div className="flex space-x-3">
            <button
              onClick={onClose}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
            >
              关闭
            </button>
            {prompt.currentVersion && (
              <button
                onClick={handleDebug}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors flex items-center"
              >
                <i className="fas fa-bug mr-2"></i>
                Debug
              </button>
            )}
          </div>
        </div>
      </div>
      
      {showVersionHistory && (
        <VersionHistoryModal
          prompt={prompt}
          onClose={() => setShowVersionHistory(false)}
        />
      )}
    </div>
  );
};

export default PromptDetailModal;
