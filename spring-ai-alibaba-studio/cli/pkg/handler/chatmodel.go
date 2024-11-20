package handler

import (
	"fmt"
	"io"
	"os"

	"github.com/alibaba/spring-ai-alibaba/pkg/api/chatmodel"
	"github.com/alibaba/spring-ai-alibaba/pkg/constant"
	"github.com/alibaba/spring-ai-alibaba/pkg/util/printer"
	"github.com/spf13/cobra"
)

type chatModelHandlerManager struct {
	apis   chatmodel.ChatModelAPI
	output io.Writer
	isMock bool
}

func NewChatModelHandlerManager() *chatModelHandlerManager {
	return &chatModelHandlerManager{
		apis:   chatmodel.NewChatModelAPI(),
		output: os.Stdout,
		isMock: false,
	}
}

func (m *chatModelHandlerManager) ChatModelListHandler(cmd *cobra.Command, args []string) {
	// send http request
	models, err := m.apis.ListChatModels(&chatmodel.ListChatModelsReq{})
	if err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}
	// format output
	outputKind, err := cmd.Flags().GetString(constant.OutputFlag)
	if err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}
	// print result
	if err := printer.PrintSlice(models, printer.PrinterKind(outputKind), m.output); err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
	}
}

func (m *chatModelHandlerManager) ChatModelGetHandler(cmd *cobra.Command, args []string) {
	// args validation
	modelNames := args
	result := make([]*chatmodel.GetChatModelRsp, 0, len(modelNames))
	// send http request
	for _, modelName := range modelNames {
		model, err := m.apis.GetChatModel(&chatmodel.GetChatModelReq{ModelName: modelName})
		if err != nil {
			printer.PrintError(err, cmd, m.output, m.isMock)
			return
		}
		result = append(result, model)
	}
	// format output
	outputKind, err := cmd.Flags().GetString(constant.OutputFlag)
	if err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}
	// print result
	if err := printer.PrintSlice(result, printer.PrinterKind(outputKind), m.output); err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
	}
}

func (m *chatModelHandlerManager) ChatModelRunHandler(cmd *cobra.Command, args []string) {
	// args validation
	if len(args) < 2 {
		err := fmt.Errorf("invalid args")
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}
	modelName := args[0]
	// check model type
	model, err := loadingWrapper(m.apis.GetChatModel, &chatmodel.GetChatModelReq{ModelName: modelName}, m.output)
	if err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}
	switch model.ModelType {
	case string(constant.ChatModelType):
		m.runChatModel(cmd, args)
	case string(constant.ImageModelType):
		m.runImageModel(cmd, args)
	}
}

func (m *chatModelHandlerManager) runChatModel(cmd *cobra.Command, args []string) {
	modelName := args[0]
	input := args[1]
	prompt, err := cmd.Flags().GetString(constant.PromptFlag)
	if err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}

	// send http request
	result, err := loadingWrapper(m.apis.RunChatModel, &chatmodel.RunChatModelReq{Key: modelName, Input: input, Prompt: prompt}, m.output)
	if err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}
	// format output
	outputKind, err := cmd.Flags().GetString(constant.OutputFlag)
	if err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}
	// print result
	verbose, err := cmd.Flags().GetCount(constant.VerboseFlag)
	if err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}
	var printResult any
	switch verbose {
	case 0:
		printer.PrintText(result.Result.Response, m.output)
		return
	case 1:
		printResult = result.Result
	case 2:
		printResult = result
	default:
		err := fmt.Errorf("invalid verbose level")
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}
	if err := printer.PrintOne(printResult, printer.PrinterKind(outputKind), m.output); err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
	}
}

func (m *chatModelHandlerManager) runImageModel(cmd *cobra.Command, args []string) {
	modelName := args[0]
	input := args[1]
	prompt, err := cmd.Flags().GetString(constant.PromptFlag)
	if err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}
	// format output
	f, err := cmd.Flags().GetString(constant.FileFlag)
	if err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}

	// send http request
	_, err = loadingWrapper(m.apis.RunImageModelFunc(f), &chatmodel.RunImageModelReq{Key: modelName, Input: input, Prompt: prompt}, m.output)
	if err != nil {
		printer.PrintError(err, cmd, m.output, m.isMock)
		return
	}
	printer.PrintText(fmt.Sprintf("Image model running successfully, output file path: %s", f), m.output)
}
