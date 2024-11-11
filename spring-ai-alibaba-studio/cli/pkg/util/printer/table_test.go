package printer

import (
	"bytes"
	"testing"
	"text/tabwriter"
)

type TestTablePrinterStruct struct {
	Field1 string
	Field2 int
	Field3 bool
}

func TestPrintSlice(t *testing.T) {
	tests := []struct {
		name    string
		data    []*TestTablePrinterStruct
		want    string
		wantErr bool
	}{
		{
			name: "valid slice",
			data: []*TestTablePrinterStruct{
				{"value1", 1, true},
				{"value2", 2, false},
			},
			want:    "Field1\tField2\tField3\t\nvalue1\t1\ttrue\t\nvalue2\t2\tfalse\t\n",
			wantErr: false,
		},
		{
			name: "nil as elements",
			data: []*TestTablePrinterStruct{
				nil,
				{"value2", 2, false},
			},
			want:    "Field1\tField2\tField3\t\nvalue2\t2\tfalse\t\n",
			wantErr: false,
		},
		{
			name:    "empty slice",
			data:    []*TestTablePrinterStruct{},
			want:    "",
			wantErr: true,
		},
		{
			name:    "nil slice",
			data:    nil,
			want:    "",
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var buf bytes.Buffer
			writer := tabwriter.NewWriter(&buf, 0, 8, 0, '\t', 0)
			printer := NewCustomTablePrinter[*TestTablePrinterStruct](writer)

			err := printer.PrintSlice(tt.data)
			if (err != nil) != tt.wantErr {
				t.Errorf("PrintSlice() error = %v, wantErr %v", err, tt.wantErr)
				return
			}

			writer.Flush()
			if got := buf.String(); got != tt.want {
				t.Errorf("PrintSlice() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestPrintOne(t *testing.T) {
	tests := []struct {
		name    string
		data    TestTablePrinterStruct
		want    string
		wantErr bool
	}{
		{
			name:    "valid struct",
			data:    TestTablePrinterStruct{"value1", 1, true},
			want:    "Field1\tField2\tField3\t\nvalue1\t1\ttrue\t\n",
			wantErr: false,
		},
		{
			name:    "empty struct",
			data:    TestTablePrinterStruct{},
			want:    "Field1\tField2\tField3\t\n\t0\tfalse\t\n",
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var buf bytes.Buffer
			writer := tabwriter.NewWriter(&buf, 0, 8, 0, '\t', 0)
			printer := NewCustomTablePrinter[TestTablePrinterStruct](writer)

			err := printer.PrintOne(tt.data)
			if (err != nil) != tt.wantErr {
				t.Errorf("PrintOne() error = %v, wantErr %v", err, tt.wantErr)
				return
			}

			writer.Flush()
			if got := buf.String(); got != tt.want {
				t.Errorf("PrintOne() got = %v, want %v", got, tt.want)
			}
		})
	}
}
