package handler

import (
	"os"

	"github.com/spf13/cobra"
)

func handleError(cmd *cobra.Command, err error) {
	cmd.PrintErrln(err)
	cmd.Usage()
	os.Exit(1)
}
