// Copyright 2024 the original author or authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
