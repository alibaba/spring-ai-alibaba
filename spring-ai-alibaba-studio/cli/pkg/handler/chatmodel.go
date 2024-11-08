package handler

import (
	"fmt"
	"os"

	"github.com/alibaba/spring-ai-alibaba/pkg/api/chatmodel"
	"github.com/alibaba/spring-ai-alibaba/pkg/config"
	"github.com/spf13/cobra"
)

var chatModelAPI chatmodel.ChatModelAPI

func getChatModelAPI(baseURL string) chatmodel.ChatModelAPI {
	if chatModelAPI == nil {
		chatModelAPI = chatmodel.NewChatModelAPI(baseURL)
	}
	return chatModelAPI
}

func ChatModelListHandler(cmd *cobra.Command, args []string) {
	apis := chatmodel.NewChatModelAPI(config.GetConfigInstance().BaseURL)
	models, err := apis.ListChatModels(&chatmodel.ListChatModelsReq{})
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
	}
	for _, model := range models {
		fmt.Println(model.Name)
	}
}

func ChatModelGetHandler(cmd *cobra.Command, args []string) {
	fmt.Println("get called")
	fmt.Println(cmd.Flags().GetString("modelName"))
	fmt.Println(config.GetConfigInstance().BaseURL)
}
