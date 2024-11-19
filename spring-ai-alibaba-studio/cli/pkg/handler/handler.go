package handler

import (
	"github.com/spf13/cobra"
)

type handlerFunc func(cmd *cobra.Command, args []string)
