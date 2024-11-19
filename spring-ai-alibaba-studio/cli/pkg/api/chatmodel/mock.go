package chatmodel

import "github.com/stretchr/testify/mock"

type MockChatModelAPI struct {
	mock.Mock
}

func (m *MockChatModelAPI) GetChatModel(req *GetChatModelReq) (*GetChatModelRsp, error) {
	args := m.Called(req)
	return args.Get(0).(*GetChatModelRsp), args.Error(1)
}

func (m *MockChatModelAPI) ListChatModels(req *ListChatModelsReq) (ListChatModelsRsp, error) {
	args := m.Called(req)
	return args.Get(0).(ListChatModelsRsp), args.Error(1)
}

func (m *MockChatModelAPI) RunChatModel(req *RunChatModelReq) (*RunChatModelRsp, error) {
	args := m.Called(req)
	return args.Get(0).(*RunChatModelRsp), args.Error(1)
}
