package handler

import (
	"fmt"

	"github.com/alibaba/spring-ai-alibaba/pkg/api/chatmodel"
	"github.com/alibaba/spring-ai-alibaba/pkg/config"
	"github.com/alibaba/spring-ai-alibaba/pkg/constant"
	"github.com/alibaba/spring-ai-alibaba/pkg/util/printer"
	"github.com/spf13/cobra"
)

func ChatModelListHandler(cmd *cobra.Command, args []string) {
	// send http request
	apis := chatmodel.NewChatModelAPI(config.GetConfigInstance().BaseURL)
	models, err := apis.ListChatModels(&chatmodel.ListChatModelsReq{})
	if err != nil {
		handleError(cmd, err)
	}
	// format output
	outputKind, err := cmd.Flags().GetString(constant.OutputFlag)
	if err != nil {
		handleError(cmd, err)
	}
	// print result
	if err := printer.PrintSlice(models, printer.PrinterKind(outputKind)); err != nil {
		handleError(cmd, err)
	}
}

func ChatModelGetHandler(cmd *cobra.Command, args []string) {
	// args validation
	modelNames := args
	result := make([]*chatmodel.GetChatModelRsp, 0, len(modelNames))
	// send http request
	apis := chatmodel.NewChatModelAPI(config.GetConfigInstance().BaseURL)
	for _, modelName := range modelNames {
		model, err := apis.GetChatModel(&chatmodel.GetChatModelReq{ModelName: modelName})
		if err != nil {
			handleError(cmd, err)
		}
		result = append(result, model)
	}
	// format output
	outputKind, err := cmd.Flags().GetString(constant.OutputFlag)
	if err != nil {
		handleError(cmd, err)
	}
	// print result
	if err := printer.PrintSlice(result, printer.PrinterKind(outputKind)); err != nil {
		handleError(cmd, err)
	}
}

func ChatModelRunHandler(cmd *cobra.Command, args []string) {
	// args validation
	if len(args) < 2 {
		handleError(cmd, fmt.Errorf("invalid args"))
	}
	modelName := args[0]
	input := args[1]
	prompt, err := cmd.Flags().GetString(constant.PromptFlag)
	if err != nil {
		handleError(cmd, err)
	}
	// send http request
	apis := chatmodel.NewChatModelAPI(config.GetConfigInstance().BaseURL)
	result, err := apis.RunChatModel(&chatmodel.RunChatModelReq{Key: modelName, Input: input, Prompt: prompt})
	if err != nil {
		handleError(cmd, err)
	}
	// format output
	outputKind, err := cmd.Flags().GetString(constant.OutputFlag)
	if err != nil {
		handleError(cmd, err)
	}
	// print result
	verbose, err := cmd.Flags().GetCount(constant.VerboseFlag)
	if err != nil {
		handleError(cmd, err)
	}
	var printResult any
	switch verbose {
	case 0:
		printer.PrintText(result.Result.Response)
		return
	case 1:
		printResult = result.Result
	case 2:
		printResult = result
	default:
		handleError(cmd, fmt.Errorf("invalid verbose level"))
	}
	if err := printer.PrintOne(printResult, printer.PrinterKind(outputKind)); err != nil {
		handleError(cmd, err)
	}
}
