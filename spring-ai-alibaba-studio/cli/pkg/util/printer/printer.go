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
