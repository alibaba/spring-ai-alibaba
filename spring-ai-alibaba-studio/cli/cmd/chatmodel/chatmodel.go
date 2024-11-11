package chatmodel

import (
	"github.com/spf13/cobra"
)

// chatmodelCmd represents the chatmodel command
var chatmodelCmd = &cobra.Command{
	Use:   "chatmodel",
	Short: "A brief description of your command",
	Long: `A longer description that spans multiple lines and likely contains examples
and usage of using your command. For example:

Cobra is a CLI library for Go that empowers applications.
This application is a tool to generate the needed files
to quickly create a Cobra application.`,
}

func GetChatModelCmd() *cobra.Command {
	return chatmodelCmd
}

func init() {
	chatmodelCmd.AddCommand(getCmd)
	chatmodelCmd.AddCommand(listCmd)

	// Here you will define your flags and configuration settings.

	// Cobra supports Persistent Flags which will work for this command
	// and all subcommands, e.g.:
	// chatmodelCmd.PersistentFlags().String("foo", "", "A help for foo")

	// Cobra supports local flags which will only run when this command
	// is called directly, e.g.:
	// chatmodelCmd.Flags().BoolP("toggle", "t", false, "Help message for toggle")
}
