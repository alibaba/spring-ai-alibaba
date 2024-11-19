package chatmodel

import (
	"fmt"

	"github.com/alibaba/spring-ai-alibaba/pkg/constant"
	"github.com/alibaba/spring-ai-alibaba/pkg/handler"
	"github.com/spf13/cobra"
)

// listCmd represents the list command
var listCmd = &cobra.Command{
	Use:   "list",
	Short: "List all available chat models",
	Long:  "List all available chat models",
	Example: fmt.Sprintf(`  # List all available chat models
  %s %s list
  # List all available chat models in JSON format
  %s %s list -o json
  	`, constant.RootCmdName, chatModelCmdName, constant.RootCmdName, chatModelCmdName),
	Run: func(cmd *cobra.Command, args []string) {
		handler.NewChatModelHandlerManager().ChatModelListHandler(cmd, args)
	},
}

func init() {
	// Here you will define your flags and configuration settings.

	// Cobra supports Persistent Flags which will work for this command
	// and all subcommands, e.g.:
	// listCmd.PersistentFlags().String("foo", "", "A help for foo")

	// Cobra supports local flags which will only run when this command
	// is called directly, e.g.:
	// listCmd.Flags().BoolP("toggle", "t", false, "Help message for toggle")
}
