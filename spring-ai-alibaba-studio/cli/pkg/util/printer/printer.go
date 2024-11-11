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
	YamlPrinterKind  PrinterKind = "yaml"
)

func PrinterKindsAsString() []string {
	return []string{string(TablePrinterKind), string(JsonPrinterKind), string(YamlPrinterKind)}
}

func PrinterDetailKindsAsString() []string {
	return []string{string(JsonPrinterKind), string(YamlPrinterKind)}
}

func PrintSlice[T any](data []T, kind PrinterKind) error {
	switch kind {
	case TablePrinterKind:
		return NewTablePrinter[T]().PrintSlice(data)
	case JsonPrinterKind:
		return NewJsonPrinter[T]().PrintSlice(data)
	case YamlPrinterKind:
		return NewYamlPrinter[T]().PrintSlice(data)
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
	case YamlPrinterKind:
		return NewYamlPrinter[T]().PrintOne(data)
	default:
		return fmt.Errorf("unsupported printer kind: %s", kind)
	}
}
