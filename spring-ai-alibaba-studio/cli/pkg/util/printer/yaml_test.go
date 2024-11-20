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
	"bytes"
	"reflect"
	"testing"

	"gopkg.in/yaml.v3"
)

type TestYamlPrinterStruct struct {
	Name string `json:"name"`
	Age  int    `json:"age"`
}

func TestYamlPrinter_PrintSlice(t *testing.T) {
	tests := []struct {
		name    string
		data    []TestYamlPrinterStruct
		want    string
		wantErr bool
	}{
		{
			name: "empty slice",
			data: []TestYamlPrinterStruct{},
			want: "[]\n",
		},
		{
			name: "slice with one element",
			data: []TestYamlPrinterStruct{{Name: "John", Age: 30}},
			want: "- name: John\n  age: 30\n",
		},
		{
			name: "slice with multiple elements",
			data: []TestYamlPrinterStruct{{Name: "John", Age: 30}, {Name: "Jane", Age: 25}},
			want: "- name: John\n  age: 30\n- name: Jane\n  age: 25\n",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var buf bytes.Buffer
			p := &YamlPrinter[TestYamlPrinterStruct]{writer: &buf}
			err := p.PrintSlice(tt.data)
			if (err != nil) != tt.wantErr {
				t.Errorf("PrintSlice() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if buf.String() != tt.want {
				t.Errorf("PrintSlice() = %v, want %v", buf.String(), tt.want)
			}
		})
	}
}

func TestYamlPrinter_PrintOne(t *testing.T) {
	tests := []struct {
		name    string
		data    TestYamlPrinterStruct
		want    string
		wantErr bool
	}{
		{
			name: "single element",
			data: TestYamlPrinterStruct{Name: "John", Age: 30},
			want: "name: John\nage: 30\n",
		},
		{
			name: "empty element",
			data: TestYamlPrinterStruct{},
			want: "name: \"\"\nage: 0\n",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var buf bytes.Buffer
			p := &YamlPrinter[TestYamlPrinterStruct]{writer: &buf}
			err := p.PrintOne(tt.data)
			if (err != nil) != tt.wantErr {
				t.Errorf("PrintOne() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if buf.String() != tt.want {
				t.Errorf("PrintOne() = %v, want %v", buf.String(), tt.want)
			}
		})
	}
}

func TestYamlPrinter_PrintOnePointer(t *testing.T) {
	tests := []struct {
		name    string
		data    *TestYamlPrinterStruct
		want    string
		wantErr bool
	}{
		{
			name: "single pointer",
			data: &TestYamlPrinterStruct{Name: "John", Age: 30},
			want: "name: John\nage: 30\n",
		},
		{
			name: "nil",
			data: nil,
			want: "null",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var buf bytes.Buffer
			p := &YamlPrinter[*TestYamlPrinterStruct]{writer: &buf}
			err := p.PrintOne(tt.data)
			if (err != nil) != tt.wantErr {
				t.Errorf("PrintOne() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			var gotObj, wantObj interface{}
			if err := yaml.Unmarshal(buf.Bytes(), &gotObj); err != nil {
				t.Errorf("Failed to unmarshal got YAML: %v", err)
				return
			}
			if err := yaml.Unmarshal([]byte(tt.want), &wantObj); err != nil {
				t.Errorf("Failed to unmarshal want YAML: %v", err)
				return
			}

			if !reflect.DeepEqual(gotObj, wantObj) {
				t.Errorf("PrintOne() = %v, want %v", buf.String(), tt.want)
			}
		})
	}
}
