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
	"encoding/json"
	"reflect"
	"testing"
)

type TestJsonPrinterStruct struct {
	Name string `json:"name"`
	Age  int    `json:"age"`
}

func TestJsonPrinter_PrintSlice(t *testing.T) {
	tests := []struct {
		name    string
		data    []TestJsonPrinterStruct
		want    string
		wantErr bool
	}{
		{
			name: "empty slice",
			data: []TestJsonPrinterStruct{},
			want: "[]\n",
		},
		{
			name: "slice with one element",
			data: []TestJsonPrinterStruct{{Name: "John", Age: 30}},
			want: "[\n  {\n    \"name\": \"John\",\n    \"age\": 30\n  }\n]\n",
		},
		{
			name: "slice with multiple elements",
			data: []TestJsonPrinterStruct{{Name: "John", Age: 30}, {Name: "Jane", Age: 25}},
			want: "[\n  {\n    \"name\": \"John\",\n    \"age\": 30\n  },\n  {\n    \"name\": \"Jane\",\n    \"age\": 25\n  }\n]\n",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var buf bytes.Buffer
			p := &JsonPrinter[TestJsonPrinterStruct]{writer: &buf}
			err := p.PrintSlice(tt.data)
			if (err != nil) != tt.wantErr {
				t.Errorf("PrintSlice() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			var gotObj, wantObj interface{}
			if err := json.Unmarshal(buf.Bytes(), &gotObj); err != nil {
				t.Errorf("Failed to unmarshal got JSON: %v", err)
				return
			}
			if err := json.Unmarshal([]byte(tt.want), &wantObj); err != nil {
				t.Errorf("Failed to unmarshal want JSON: %v", err)
				return
			}

			if !reflect.DeepEqual(gotObj, wantObj) {
				t.Errorf("PrintSlice() = %v, want %v", buf.String(), tt.want)
			}
		})
	}
}

func TestJsonPrinter_PrintOne(t *testing.T) {
	tests := []struct {
		name    string
		data    TestJsonPrinterStruct
		want    string
		wantErr bool
	}{
		{
			name: "single element",
			data: TestJsonPrinterStruct{Name: "John", Age: 30},
			want: "{\n  \"name\": \"John\",\n  \"age\": 30\n}\n",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var buf bytes.Buffer
			p := &JsonPrinter[TestJsonPrinterStruct]{writer: &buf}
			err := p.PrintOne(tt.data)
			if (err != nil) != tt.wantErr {
				t.Errorf("PrintOne() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			var gotObj, wantObj interface{}
			if err := json.Unmarshal(buf.Bytes(), &gotObj); err != nil {
				t.Errorf("Failed to unmarshal got JSON: %v", err)
				return
			}
			if err := json.Unmarshal([]byte(tt.want), &wantObj); err != nil {
				t.Errorf("Failed to unmarshal want JSON: %v", err)
				return
			}

			if !reflect.DeepEqual(gotObj, wantObj) {
				t.Errorf("PrintOne() = %v, want %v", buf.String(), tt.want)
			}
		})
	}
}
