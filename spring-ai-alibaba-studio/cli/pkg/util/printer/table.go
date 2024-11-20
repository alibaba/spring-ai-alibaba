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
	"reflect"
	"text/tabwriter"
)

type TablePrinter[T any] struct {
	writer *tabwriter.Writer
}

func NewTablePrinter[T any](output io.Writer) Printer[T] {
	return &TablePrinter[T]{
		writer: tabwriter.NewWriter(output, 0, 8, 0, '\t', 0),
	}
}

func NewCustomTablePrinter[T any](writer *tabwriter.Writer) Printer[T] {
	return &TablePrinter[T]{
		writer: writer,
	}
}

func (p *TablePrinter[T]) PrintSlice(data []T) error {
	// use reflection to get the fields of data
	val := reflect.ValueOf(data)
	if val.Kind() != reflect.Slice {
		return fmt.Errorf("data is not a slice")
	}

	if len(data) == 0 {
		return fmt.Errorf("no data to print")
	}

	pointerFlag := false
	// Print header
	elemType := val.Index(0).Type()
	if elemType.Kind() == reflect.Pointer {
		elemType = elemType.Elem()
		pointerFlag = true
	}
	for i := 0; i < elemType.NumField(); i++ {
		fmt.Fprintf(p.writer, "%s\t", elemType.Field(i).Name)
	}
	fmt.Fprintln(p.writer)

	// Print rows
	for i := 0; i < val.Len(); i++ {
		elem := val.Index(i)
		if pointerFlag {
			if elem.IsNil() {
				continue
			}
			elem = elem.Elem()
		}
		for j := 0; j < elem.NumField(); j++ {
			fmt.Fprintf(p.writer, "%v\t", elem.Field(j).Interface())
		}
		fmt.Fprintln(p.writer)
	}

	p.writer.Flush()
	return nil
}

func (p *TablePrinter[T]) PrintOne(data T) error {
	// use reflection to get the fields of data
	val := reflect.ValueOf(data)
	if val.Kind() == reflect.Pointer {
		if val.IsNil() {
			return fmt.Errorf("data is nil")
		}
		val = val.Elem()
	}
	if val.Kind() != reflect.Struct {
		return fmt.Errorf("data is not a struct")
	}

	// Print header
	elemType := val.Type()
	for i := 0; i < elemType.NumField(); i++ {
		fmt.Fprintf(p.writer, "%s\t", elemType.Field(i).Name)
	}
	fmt.Fprintln(p.writer)

	// Print row
	for i := 0; i < val.NumField(); i++ {
		fmt.Fprintf(p.writer, "%v\t", val.Field(i).Interface())
	}
	fmt.Fprintln(p.writer)

	p.writer.Flush()
	return nil
}
