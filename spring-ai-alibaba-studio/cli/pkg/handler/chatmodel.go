package handler

import (
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
