import React, { useState } from 'react';
import VersionCompareModal from './VersionCompareModal';

const VersionHistoryModal = ({ prompt, onClose }) => {
  const [showCompare, setShowCompare] = useState(false);
  const [selectedVersions, setSelectedVersions] = useState([]);

  const handleVersionClick = (version) => {
    if (selectedVersions.length === 0) {
      setSelectedVersions([version]);
    } else if (selectedVersions.length === 1) {
      if (selectedVersions[0].id === version.id) {
        setSelectedVersions([]);
      } else {
        setSelectedVersions([selectedVersions[0], version]);
        setShowCompare(true);
      }
    } else {
      setSelectedVersions([version]);
    }
  };


  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 modal-overlay">
      <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-y-auto fade-in">
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-gray-900">
              版本记录 - {prompt.name}
            </h2>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600"
            >
              <i className="fas fa-times"></i>
            </button>
          </div>
          <p className="text-sm text-gray-500 mt-2">
            点击两个版本进行对比，或单击查看详情
          </p>
        </div>
        
        <div className="p-6">
          <div className="space-y-4">
            {prompt.history && prompt.history.length > 0 ? (
              prompt.history.slice().reverse().map((version, index) => (
                <div
                  key={version.id}
                  onClick={() => handleVersionClick(version)}
                  className={`border rounded-lg p-4 cursor-pointer transition-all hover:shadow-md ${
                    selectedVersions.some(v => v.id === version.id)
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center space-x-3">
                        <i className="fas fa-tag text-blue-500"></i>
                        <div>
                          <span className="font-medium text-gray-900">
                            {version.version}
                          </span>
                        </div>
                      </div>
                      <div className="text-right text-sm text-gray-500">
                        <div>{version.createdAt}</div>
                      </div>
                  </div>
                  
                  <div className="mb-3">
                    <h4 className="text-sm font-medium text-gray-700 mb-1">
                      版本说明:
                    </h4>
                    <p className="text-sm text-gray-600">
                      {version.description}
                    </p>
                  </div>
                  
                  <div>
                    <h4 className="text-sm font-medium text-gray-700 mb-1">
                      内容预览:
                    </h4>
                    <div className="text-sm text-gray-600 bg-gray-50 p-2 rounded max-h-20 overflow-hidden">
                      {version.content.substring(0, 150)}
                      {version.content.length > 150 && '...'}
                    </div>
                  </div>
                  
                  {selectedVersions.some(v => v.id === version.id) && (
                    <div className="mt-2 text-xs text-blue-600">
                      <i className="fas fa-check-circle mr-1"></i>
                      已选择用于对比
                    </div>
                  )}
                </div>
              ))
            ) : (
                  <div className="text-center py-8">
                    <i className="fas fa-history text-3xl text-gray-300 mb-3"></i>
                    <p className="text-gray-500">暂无版本记录</p>
                  </div>
            )}
          </div>
        </div>

        <div className="p-6 border-t border-gray-200 flex justify-between">
          <div className="text-sm text-gray-500">
            {selectedVersions.length > 0 && (
              <span>
                已选择 {selectedVersions.length} 个版本
                {selectedVersions.length === 2 && ' (将自动打开对比)'}
              </span>
            )}
          </div>
          
          <div className="flex space-x-3">
            <button
              onClick={() => setSelectedVersions([])}
              disabled={selectedVersions.length === 0}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 disabled:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed transition-colors"
            >
              清除选择
            </button>
            <button
              onClick={onClose}
              className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
      
      {showCompare && selectedVersions.length === 2 && (
        <VersionCompareModal
          prompt={prompt}
          version1={selectedVersions[0]}
          version2={selectedVersions[1]}
          onClose={() => {
            setShowCompare(false);
            setSelectedVersions([]);
          }}
        />
      )}
    </div>
  );
};

export default VersionHistoryModal;
