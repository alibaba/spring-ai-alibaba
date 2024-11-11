package printer

import (
	"encoding/json"
	"io"
	"os"
)

type JsonPrinter[T any] struct {
	writer io.Writer
}

func NewJsonPrinter[T any]() *JsonPrinter[T] {
	return &JsonPrinter[T]{
		writer: os.Stdout,
	}
}

func (p *JsonPrinter[T]) print(data interface{}) error {
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
