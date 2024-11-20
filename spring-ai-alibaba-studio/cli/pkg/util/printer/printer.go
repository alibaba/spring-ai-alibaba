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

package printer

import (
	"fmt"
	"io"

	"github.com/spf13/cobra"
)

type Printer[T any] interface {
	PrintOne(data T) error
	PrintSlice(data []T) error
}

type PrinterKind string

const (
	TablePrinterKind PrinterKind = "table"
	JsonPrinterKind  PrinterKind = "json"
	YamlPrinterKind  PrinterKind = "yaml"
)

func PrinterKindsAsString() []string {
	return []string{string(TablePrinterKind), string(JsonPrinterKind), string(YamlPrinterKind)}
}

func PrinterDetailKindsAsString() []string {
	return []string{string(JsonPrinterKind), string(YamlPrinterKind)}
}

func PrintSlice[T any](data []T, kind PrinterKind, output io.Writer) error {
	switch kind {
	case TablePrinterKind:
		return NewTablePrinter[T](output).PrintSlice(data)
	case JsonPrinterKind:
		return NewJsonPrinter[T](output).PrintSlice(data)
	case YamlPrinterKind:
		return NewYamlPrinter[T](output).PrintSlice(data)
	default:
		return fmt.Errorf("unsupported output kind: %s", kind)
	}
}

func PrintOne[T any](data T, kind PrinterKind, output io.Writer) error {
	switch kind {
	case TablePrinterKind:
		return NewTablePrinter[T](output).PrintOne(data)
	case JsonPrinterKind:
		return NewJsonPrinter[T](output).PrintOne(data)
	case YamlPrinterKind:
		return NewYamlPrinter[T](output).PrintOne(data)
	default:
		return fmt.Errorf("unsupported output kind: %s", kind)
	}
}

func PrintText(text string, output io.Writer) {
	fmt.Fprintln(output, text)
}

func PrintError(err error, cmd *cobra.Command, output io.Writer, isMock bool) {
	fmt.Fprintln(output, err)
	if !isMock {
		cmd.Usage()
	}
}
