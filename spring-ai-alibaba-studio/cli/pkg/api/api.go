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

package api

import "fmt"

const (
	DefaultBaseURL = "http://localhost:8080"
	CommonPrefix   = "/studio/api"
	successCode    = 200
)

type Resp[T any] struct {
	Code int    `json:"code"`
	Msg  string `json:"msg"`
	Data T      `json:"data"`
}

func ValidateResp[T any](resp *Resp[T]) error {
	if resp.Code != successCode {
		return fmt.Errorf("code: %d, msg: %s", resp.Code, resp.Msg)
	}
	return nil
}
