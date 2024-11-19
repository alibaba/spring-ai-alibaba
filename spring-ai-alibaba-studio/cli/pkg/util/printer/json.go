package printer

import (
	"encoding/json"
	"io"
)

type JsonPrinter[T any] struct {
	writer io.Writer
}

func NewJsonPrinter[T any](output io.Writer) *JsonPrinter[T] {
	return &JsonPrinter[T]{
		writer: output,
	}
}

func (p *JsonPrinter[T]) print(data any) error {
	jsonData, err := json.MarshalIndent(data, "", "  ")
	if err != nil {
		return err
	}
	_, err = p.writer.Write(jsonData)
	return err
}

func (p *JsonPrinter[T]) PrintOne(data T) error {
	return p.print(data)
}

func (p *JsonPrinter[T]) PrintSlice(data []T) error {
	return p.print(data)
}
