package handler

import (
	"fmt"
	"os"

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
		fmt.Fprintln(os.Stderr, err)
	}
	// format output
	outputKind, err := cmd.Flags().GetString(constant.OutputFlag)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
	}
	// print result
	printer.PrintSlice(models, printer.PrinterKind(outputKind))
}

func ChatModelGetHandler(cmd *cobra.Command, args []string) {
	// param validation
	modelName, err := cmd.Flags().GetString(constant.ModelNameFlag)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
	}
	if modelName == "" {
		fmt.Fprintln(os.Stderr, `required flag "modelName" should not be empty`)
		os.Exit(1)
	}
	// send http request
	apis := chatmodel.NewChatModelAPI(config.GetConfigInstance().BaseURL)
	model, err := apis.GetChatModel(&chatmodel.GetChatModelReq{ModelName: modelName})
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
	}
	// format output
	outputKind, err := cmd.Flags().GetString(constant.OutputFlag)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
	}
	// print result
	printer.PrintOne(model, printer.PrinterKind(outputKind))
}
