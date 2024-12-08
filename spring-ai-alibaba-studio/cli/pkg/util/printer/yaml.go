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
	"io"

	"gopkg.in/yaml.v3"
)

type YamlPrinter[T any] struct {
	writer io.Writer
}

func NewYamlPrinter[T any](output io.Writer) *YamlPrinter[T] {
	return &YamlPrinter[T]{
		writer: output,
	}
}

func (p *YamlPrinter[T]) PrintOne(data T) error {
	return p.print(data)
}

func (p *YamlPrinter[T]) PrintSlice(data []T) error {
	return p.print(data)
}

func (p *YamlPrinter[T]) print(data any) error {
	yamlData, err := yaml.Marshal(data)
	if err != nil {
		return err
	}
	_, err = p.writer.Write(yamlData)
	return err
}
