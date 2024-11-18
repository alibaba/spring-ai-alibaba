package chatmodel

import (
	"github.com/spf13/cobra"
)

const chatModelCmdName = "chatmodel"

// chatmodelCmd represents the chatmodel command
var chatmodelCmd = &cobra.Command{
	Use:   chatModelCmdName,
	Short: "Used to manage chat models",
}

func GetChatModelCmd() *cobra.Command {
	return chatmodelCmd
}

func init() {
	chatmodelCmd.AddCommand(getCmd)
	chatmodelCmd.AddCommand(listCmd)
	chatmodelCmd.AddCommand(runCmd)

	// Here you will define your flags and configuration settings.

	// Cobra supports Persistent Flags which will work for this command
	// and all subcommands, e.g.:
	// chatmodelCmd.PersistentFlags().String("foo", "", "A help for foo")

	// Cobra supports local flags which will only run when this command
	// is called directly, e.g.:
	// chatmodelCmd.Flags().BoolP("toggle", "t", false, "Help message for toggle")
}
