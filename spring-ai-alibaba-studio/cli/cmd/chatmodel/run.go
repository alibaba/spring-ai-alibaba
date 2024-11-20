package chatmodel

import (
	"fmt"
	"strings"

	"github.com/alibaba/spring-ai-alibaba/pkg/constant"
	"github.com/alibaba/spring-ai-alibaba/pkg/handler"
	"github.com/alibaba/spring-ai-alibaba/pkg/util/printer"
	"github.com/spf13/cobra"
)

// runCmd represents the run command
var runCmd = &cobra.Command{
	Use:   "run",
	Short: "Run a chat model with the given input",
	Long: `Run a chat model with the given input
Arguments:
[modelName]  Name of the chat model to run (required)
[input]      Input text to run the chat model with (required)
	`,
	Example: fmt.Sprintf(`  # Run the chat model named "model1" with input "hello"
  %s %s run model1 hello
  # Run the chat model named "model1" with input "a longer input" and additional prompt message
  %s %s run model1 "a longer input" -p "additional prompt message"`, constant.RootCmdName, chatModelCmdName, constant.RootCmdName, chatModelCmdName),
	Args: cobra.MinimumNArgs(2),
	Run: func(cmd *cobra.Command, args []string) {
		handler.NewChatModelHandlerManager().ChatModelRunHandler(cmd, args)
	},
}

func init() {
	// Here you will define your flags and configuration settings.

	// Cobra supports Persistent Flags which will work for this command
	// and all subcommands, e.g.:
	// runCmd.PersistentFlags().String("foo", "", "A help for foo")

	// Cobra supports local flags which will only run when this command
	// is called directly, e.g.:
	// runCmd.Flags().BoolP("toggle", "t", false, "Help message for toggle")
	runCmd.Flags().StringP(constant.PromptFlag, "p", "", "additional prompt message for chat model running")
	runCmd.Flags().CountP(constant.VerboseFlag, "v", "verbose output level (use -v, -vv for higher verbosity)")
	runCmd.Flags().StringP(constant.OutputFlag, "o", string(printer.JsonPrinterKind), fmt.Sprintf("Output format supported values: %s", strings.Join(printer.PrinterDetailKindsAsString(), ", ")))
}
