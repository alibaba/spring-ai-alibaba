// Copyright 2024 the original author or authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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

func (m *MockChatModelAPI) RunImageModelFunc(outputFileName string) func(req *RunImageModelReq) (*RunImageModelRsp, error) {
	args := m.Called(outputFileName)
	return args.Get(0).(func(req *RunImageModelReq) (*RunImageModelRsp, error))
}
