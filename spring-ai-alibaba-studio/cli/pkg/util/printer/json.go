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
