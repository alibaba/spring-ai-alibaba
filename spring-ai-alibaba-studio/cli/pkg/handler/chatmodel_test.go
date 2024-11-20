package handler

import (
	"bytes"
	"errors"
	"testing"

	"github.com/alibaba/spring-ai-alibaba/pkg/api/chatmodel"
	"github.com/alibaba/spring-ai-alibaba/pkg/constant"
	"github.com/spf13/cobra"
	"github.com/stretchr/testify/assert"
)

func TestChatModelListHandler(t *testing.T) {
	cmd := &cobra.Command{}
	cmd.Flags().String(constant.OutputFlag, "json", "output format")

	t.Run("success", func(t *testing.T) {
		mockAPI := &chatmodel.MockChatModelAPI{}
		mockOutput := new(bytes.Buffer)
		hm := &chatModelHandlerManager{
			apis:   mockAPI,
			output: mockOutput,
			isMock: true,
		}

		expectedModels := []*chatmodel.ChatModel{{Name: "model1"}, {Name: "model2"}}
		mockAPI.On("ListChatModels", &chatmodel.ListChatModelsReq{}).Return(chatmodel.ListChatModelsRsp(expectedModels), nil)

		hm.ChatModelListHandler(cmd, []string{})

		mockAPI.AssertExpectations(t)
		assert.Contains(t, mockOutput.String(), "model1")
		assert.Contains(t, mockOutput.String(), "model2")
	})

	t.Run("error in ListChatModels", func(t *testing.T) {
		mockAPI := &chatmodel.MockChatModelAPI{}
		mockOutput := new(bytes.Buffer)
		hm := &chatModelHandlerManager{
			apis:   mockAPI,
			output: mockOutput,
			isMock: true,
		}
		expectedModels := []*chatmodel.ChatModel{}
		mockAPI.On("ListChatModels", &chatmodel.ListChatModelsReq{}).Return(chatmodel.ListChatModelsRsp(expectedModels), errors.New("mock error"))

		hm.ChatModelListHandler(cmd, []string{})

		// mockAPI.AssertExpectations(t)
		assert.Contains(t, mockOutput.String(), "mock error")
	})
}

func TestChatModelGetHandler(t *testing.T) {
	cmd := &cobra.Command{}
	cmd.Flags().String(constant.OutputFlag, "json", "output format")

	t.Run("success", func(t *testing.T) {
		mockAPI := &chatmodel.MockChatModelAPI{}
		mockOutput := new(bytes.Buffer)
		hm := &chatModelHandlerManager{
			apis:   mockAPI,
			output: mockOutput,
			isMock: true,
		}

		expectedModel := &chatmodel.ChatModel{Name: "mockModel", Model: "qwen-mock", ModelType: "mock"}
		mockAPI.On("GetChatModel", &chatmodel.GetChatModelReq{ModelName: "mockModel"}).Return(&chatmodel.GetChatModelRsp{ChatModel: expectedModel}, nil)

		hm.ChatModelGetHandler(cmd, []string{"mockModel"})

		mockAPI.AssertExpectations(t)
		assert.Contains(t, mockOutput.String(), "mockModel")
		assert.Contains(t, mockOutput.String(), "qwen-mock")
	})

	t.Run("error in GetChatModel", func(t *testing.T) {
		mockAPI := &chatmodel.MockChatModelAPI{}
		mockOutput := new(bytes.Buffer)
		hm := &chatModelHandlerManager{
			apis:   mockAPI,
			output: mockOutput,
			isMock: true,
		}
		mockAPI.On("GetChatModel", &chatmodel.GetChatModelReq{ModelName: "err"}).Return(&chatmodel.GetChatModelRsp{}, errors.New("mock error"))

		hm.ChatModelGetHandler(cmd, []string{"err"})

		assert.Contains(t, mockOutput.String(), "mock error")
	})
}

func TestChatModelRunHandler(t *testing.T) {
	t.Run("success", func(t *testing.T) {
		cmd := &cobra.Command{}
		cmd.Flags().String(constant.OutputFlag, "json", "output format")
		cmd.Flags().String(constant.PromptFlag, "", "prompt message")
		cmd.Flags().Count(constant.VerboseFlag, "verbose level")
		mockAPI := &chatmodel.MockChatModelAPI{}
		mockOutput := new(bytes.Buffer)
		hm := &chatModelHandlerManager{
			apis:   mockAPI,
			output: mockOutput,
			isMock: true,
		}

		expectedResponse := &chatmodel.RunChatModelRsp{
			Result: &chatmodel.ActionResult{
				Response: "mock response",
			},
		}
		mockAPI.On("RunChatModel", &chatmodel.RunChatModelReq{Key: "mockModel", Input: "mock input"}).Return(expectedResponse, nil)

		hm.ChatModelRunHandler(cmd, []string{"mockModel", "mock input"})

		mockAPI.AssertExpectations(t)
		assert.Contains(t, mockOutput.String(), "mock response")
	})

	t.Run("error in RunChatModel", func(t *testing.T) {
		cmd := &cobra.Command{}
		cmd.Flags().String(constant.OutputFlag, "json", "output format")
		cmd.Flags().String(constant.PromptFlag, "", "prompt message")
		cmd.Flags().Count(constant.VerboseFlag, "verbose level")
		mockAPI := &chatmodel.MockChatModelAPI{}
		mockOutput := new(bytes.Buffer)
		hm := &chatModelHandlerManager{
			apis:   mockAPI,
			output: mockOutput,
			isMock: true,
		}

		mockAPI.On("RunChatModel", &chatmodel.RunChatModelReq{Key: "err", Input: "err"}).Return(&chatmodel.RunChatModelRsp{}, errors.New("mock error"))

		hm.ChatModelRunHandler(cmd, []string{"err", "err"})

		assert.Contains(t, mockOutput.String(), "mock error")
	})

	t.Run("error in flags", func(t *testing.T) {
		cmd := &cobra.Command{}
		cmd.Flags().String(constant.OutputFlag, "json", "output format")
		cmd.Flags().String(constant.PromptFlag, "", "prompt message")
		cmd.Flags().Count(constant.VerboseFlag, "verbose level")
		mockAPI := &chatmodel.MockChatModelAPI{}
		mockOutput := new(bytes.Buffer)
		hm := &chatModelHandlerManager{
			apis:   mockAPI,
			output: mockOutput,
			isMock: true,
		}

		hm.ChatModelRunHandler(cmd, []string{})
		assert.Contains(t, mockOutput.String(), "invalid args")

		hm.ChatModelRunHandler(cmd, []string{"mockModel"})
		assert.Contains(t, mockOutput.String(), "invalid args")
	})
}
