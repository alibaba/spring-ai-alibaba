package handler

import (
	"fmt"
	"io"
	"time"

	"github.com/spf13/cobra"
)

type handlerFunc func(cmd *cobra.Command, args []string)

func showLoading(done chan struct{}, output io.Writer) {
	loadingChars := []rune{'|', '/', '-', '\\'}
	i := 0
	for {
		select {
		case <-done:
			fmt.Fprint(output, "\r \r")
			return
		default:
			fmt.Fprintf(output, "\r%c", loadingChars[i])
			i = (i + 1) % len(loadingChars)
			time.Sleep(100 * time.Millisecond)
		}
	}
}

type requestFunc[REQ any, RSP any] func(REQ) (RSP, error)

func loadingWrapper[REQ any, RSP any](f requestFunc[REQ, RSP], req REQ, loadingOutput io.Writer) (RSP, error) {
	done := make(chan struct{})
	defer close(done)
	go showLoading(done, loadingOutput)
	rsp, err := f(req)
	done <- struct{}{}
	return rsp, err
}
