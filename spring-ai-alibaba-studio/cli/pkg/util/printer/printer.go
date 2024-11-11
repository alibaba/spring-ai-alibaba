package printer

import "fmt"

type Printer[T any] interface {
	PrintOne(data T) error
	PrintSlice(data []T) error
}

type PrinterKind string

const (
	TablePrinterKind PrinterKind = "table"
	JsonPrinterKind  PrinterKind = "json"
)

func PrintSlice[T any](data []T, kind PrinterKind) error {
	switch kind {
	case TablePrinterKind:
		return NewTablePrinter[T]().PrintSlice(data)
	case JsonPrinterKind:
		return NewJsonPrinter[T]().PrintSlice(data)
	default:
		return fmt.Errorf("unsupported printer kind: %s", kind)
	}
}

func PrintOne[T any](data T, kind PrinterKind) error {
	switch kind {
	case TablePrinterKind:
		return NewTablePrinter[T]().PrintOne(data)
	case JsonPrinterKind:
		return NewJsonPrinter[T]().PrintOne(data)
	default:
		return fmt.Errorf("unsupported printer kind: %s", kind)
	}
}
