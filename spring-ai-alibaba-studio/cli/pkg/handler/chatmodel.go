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
	apis := chatmodel.NewChatModelAPI(config.GetConfigInstance().BaseURL)
	models, err := apis.ListChatModels(&chatmodel.ListChatModelsReq{})
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
	}
	outputKind, err := cmd.Flags().GetString(constant.OutputFlagName)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
	}
	printer.PrintSlice(models, printer.PrinterKind(outputKind))
}

func ChatModelGetHandler(cmd *cobra.Command, args []string) {
	fmt.Println("get called")
	fmt.Println(cmd.Flags().GetString("modelName"))
	fmt.Println(config.GetConfigInstance().BaseURL)
}
