package cmd

import (
	"github.com/alibaba/spring-ai-alibaba/pkg/handler"
	"github.com/spf13/cobra"
)

var initCmd = &cobra.Command{
	Use:   "init",
	Short: "Initialize a new Spring AI Alibaba project",
	Run: func(cmd *cobra.Command, args []string) {
		handler.InitHandler(cmd, args)
	},
}

func init() {
	rootCmd.AddCommand(initCmd)

	initCmd.Flags().StringP("name", "n", "", "Project name")
	initCmd.MarkFlagRequired("name")
}
