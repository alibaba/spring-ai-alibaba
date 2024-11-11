package chatmodel

import (
	"fmt"
	"strings"

	"github.com/alibaba/spring-ai-alibaba/pkg/constant"
	"github.com/alibaba/spring-ai-alibaba/pkg/handler"
	"github.com/alibaba/spring-ai-alibaba/pkg/util/printer"
	"github.com/spf13/cobra"
)

// getCmd represents the list command
var getCmd = &cobra.Command{
	Use:   "get",
	Short: "A brief description of your command",
	Long: `A longer description that spans multiple lines and likely contains examples
and usage of using your command. For example:

Cobra is a CLI library for Go that empowers applications.
This application is a tool to generate the needed files
to quickly create a Cobra application.`,
	Args: cobra.MinimumNArgs(1),
	Run:  handler.ChatModelGetHandler,
}

func init() {
	// Here you will define your flags and configuration settings.

	// Cobra supports Persistent Flags which will work for this command
	// and all subcommands, e.g.:
	// listCmd.PersistentFlags().String("foo", "", "A help for foo")

	// Cobra supports local flags which will only run when this command
	// is called directly, e.g.:
	// listCmd.Flags().BoolP("toggle", "t", false, "Help message for toggle")
	getCmd.Flags().StringP(constant.OutputFlag, "o", string(printer.YamlPrinterKind), fmt.Sprintf("Output format supported values: %s", strings.Join(printer.PrinterDetailKindsAsString(), ", ")))
}
